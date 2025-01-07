package Database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.List;

import Model.Vitals;
import Util.Util;

public class DatabaseHandler extends SQLiteOpenHelper {
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
                "SELECT strftime('%w', time, 'unixepoch') AS day_of_week, " +
                        "AVG(CAST(bpm AS REAL)) AS avg_bpm, " +
                        "AVG(CAST(spo2 AS REAL)) AS avg_spo2 " +
                        "FROM Sleep " +
                        "GROUP BY day_of_week " +
                        "ORDER BY day_of_week",
                null
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
    public List<Entry> getConsecutiveDrops() {
        List<Entry> result = new ArrayList<>();

        String query = "WITH consecutive_drops AS (" +
                "  SELECT t1.time AS t1_time, t2.time AS t2_time, t3.time AS t3_time, " +
                "         t4.time AS t4_time, t5.time AS t5_time " +
                "  FROM Sleep t1 " +
                "  JOIN Sleep t2 ON t2.time = t1.time + 1 " +
                "  JOIN Sleep t3 ON t3.time = t2.time + 1 " +
                "  JOIN Sleep t4 ON t4.time = t3.time + 1 " +
                "  JOIN Sleep t5 ON t5.time = t4.time + 1 " +
                "  WHERE t1.bpm < 70 AND t1.spo2 < 90 " +
                "    AND t2.bpm < 70 AND t2.spo2 < 90 " +
                "    AND t3.bpm < 70 AND t3.spo2 < 90 " +
                "    AND t4.bpm < 70 AND t4.spo2 < 90 " +
                "    AND t5.bpm < 70 AND t5.spo2 < 90" +
                ")" +
                "SELECT * FROM consecutive_drops";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        int index = 0; // For x-axis values in the chart
        if (cursor.moveToFirst()) {
            do {
                // Create an Entry for each time in the consecutive drops
                @SuppressLint("Range") float t1 = cursor.getLong(cursor.getColumnIndex("t1_time"));
                @SuppressLint("Range") float t2 = cursor.getLong(cursor.getColumnIndex("t2_time"));
                @SuppressLint("Range") float t3 = cursor.getLong(cursor.getColumnIndex("t3_time"));
                @SuppressLint("Range") float t4 = cursor.getLong(cursor.getColumnIndex("t4_time"));
                @SuppressLint("Range") float t5 = cursor.getLong(cursor.getColumnIndex("t5_time"));

                // Add each time as a point, assigning index as x-axis
                result.add(new Entry(index++, t1));
                result.add(new Entry(index++, t2));
                result.add(new Entry(index++, t3));
                result.add(new Entry(index++, t4));
                result.add(new Entry(index++, t5));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return result;
    }
}
