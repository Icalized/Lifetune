package Database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.data.BarEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import Model.Vitals;
import Util.Util;

public class DatabaseHandler extends SQLiteOpenHelper {
    private final String TAG = "DatabaseHandler";
    public DatabaseHandler(@Nullable Context context) {
        super(context, Util.DATABASE_NAME, null, Util.DATABASE_VER);
    }

    // Executing the creation of database table
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SLEEP_TABLE = "CREATE TABLE " + Util.DATABASE_TABLE + "( " + Util.KEY_ID + " BIGINT PRIMARY KEY,"
                + Util.KEY_BPM + " TEXT," + Util.KEY_SPO2 + " TEXT" + ")";
        db.execSQL(CREATE_SLEEP_TABLE);
    }

    // dropping the
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String CLOSE_DATABASE = "DROP TABLE IF EXISTS " + Util.DATABASE_TABLE;
        db.execSQL(CLOSE_DATABASE);
        onCreate(db);
    }

    // Adding the vitals to the db
    public void addData(Vitals vitals) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Util.KEY_BPM, vitals.getBpm());
        values.put(Util.KEY_SPO2, vitals.getSpo2());
        values.put(Util.KEY_ID, vitals.getTime());
        db.insert(Util.DATABASE_TABLE, null, values);
        db.close();
    }

    //Fetching values in the db
    public List<Vitals> fetchData() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Vitals> list = new ArrayList<>();
        String SELECT_ALL = "SELECT * FROM " + Util.DATABASE_TABLE;
        Cursor cursor = db.rawQuery(SELECT_ALL, null);
        if (cursor.moveToFirst()) {
            do {
                Vitals vitals = new Vitals();
                vitals.setTime(cursor.getInt(0));
                vitals.setBpm(cursor.getString(1));
                vitals.setSpo2(cursor.getString(2));
                list.add(vitals);
            } while (cursor.moveToNext());
        }
        return list;
    }

    /*Taking long dates from db and converting them to days with respective averages */
    public List<BarEntry> getDailyAverages() {
        SQLiteDatabase db = this.getWritableDatabase();
        List<BarEntry> entries = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT strftime('%w', \n" +
                        "                SUBSTR(time, 1, 4) || '-' || \n" +
                        "                SUBSTR(time, 5, 2) || '-' || \n" +
                        "                SUBSTR(time, 7, 2) || ' ' || \n" +
                        "                SUBSTR(time, 9, 2) || ':' || \n" +
                        "                SUBSTR(time, 11, 2) || ':' || \n" +
                        "                SUBSTR(time, 13, 2), \n" +
                        "                'utc') AS day_of_week, " +
                        "AVG(CAST(bpm AS REAL)) AS avg_bpm, " +
                        "AVG(CAST(spo2 AS REAL)) AS avg_spo2 " +
                        "FROM Sleep " +
                        "GROUP BY day_of_week " +
                        "ORDER BY day_of_week", null
        );

        if (cursor.moveToFirst()) {
            do {
                int dayOfWeek = cursor.getInt(0); // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
                float avgBpm = cursor.getFloat(1);
                float avgSpo2 = cursor.getFloat(2);
                // Add both averages as BarEntry (1 Bar per day)
                entries.add(new BarEntry(dayOfWeek, new float[]{avgBpm, avgSpo2}));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entries;
    }

    // Checking for apnea
    ArrayList<Long> arr = new ArrayList<>();

    public boolean checkSleepApnea(int thresholdBpm, int thresholdSpo2){
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM Sleep WHERE bpm < 70 AND spo2 < 90 ORDER BY time ASC"; // Adjust table name and order column
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null) {
            try {
                int bpmIndex = cursor.getColumnIndex("bpm");
                int spo2Index = cursor.getColumnIndex("spo2");
                int timeIndex = cursor.getColumnIndex("time");

                while (cursor.moveToNext()) {
                    int bpm = cursor.getInt(bpmIndex);
                    int spo2 = cursor.getInt(spo2Index);

                    // Check if the current row meets the threshold
                    if (bpm < thresholdBpm && spo2 < thresholdSpo2) {
                        // Check next 5 rows
                        boolean allBelowThreshold = true;

                        for (int i = 0; i < 5; i++) {
                            if (!cursor.moveToNext()) {
                                allBelowThreshold = false; // If not enough rows
                                break;
                            }

                            bpm = cursor.getInt(bpmIndex);
                            spo2 = cursor.getInt(spo2Index);
                            Long time = cursor.getLong(timeIndex);
                            arr.add(time);

                            if (bpm >= thresholdBpm || spo2 >= thresholdSpo2) {
                                allBelowThreshold = false;
                                arr.clear();
                                break;
                            }
                        }

                        if (allBelowThreshold) {
                            return true; // Sleep apnea condition detected
                        } else {
                            // Move cursor back to the row after the initial one
                            cursor.moveToPosition(cursor.getPosition() - 5);
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false; // No apnea detected
    }

    public ArrayList<Long> timestamps(){
        return arr;
    }

    // Method to get the first and last time for the current day
    @SuppressLint("Range")
    public Long[] getFirstAndLastTimeForToday() {
        SQLiteDatabase db = this.getReadableDatabase();

        // Get the current date in yyyyMMdd format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        long startDay = Long.parseLong(todayDate + "000000");
        long endDay = Long.parseLong(todayDate + "235959");

        // Query to find the first and last time for today
        String query = "SELECT " +
                "MIN(time) AS first_time, " +
                "MAX(time) AS last_time " +
                "FROM Sleep " +
                "WHERE time BETWEEN ? and ?";

        String[] selectionArgs = {String.valueOf(startDay),String.valueOf(endDay)};

        Long firstTime = 0L;
        Long lastTime = 0L;

        try (Cursor cursor = db.rawQuery(query, selectionArgs)) {
            if (cursor != null && cursor.moveToFirst()) {
                int firstTimeIndex = cursor.getColumnIndex("first_time");
                int lastTimeIndex = cursor.getColumnIndex("last_time");

                if (firstTimeIndex >= 0 && lastTimeIndex >= 0) {
                    firstTime = cursor.isNull(firstTimeIndex) ? 0L : cursor.getLong(firstTimeIndex);
                    lastTime = cursor.isNull(lastTimeIndex) ? 0L : cursor.getLong(lastTimeIndex);
                }
            }
        }
        return new Long[]{firstTime,lastTime};
    }


}
