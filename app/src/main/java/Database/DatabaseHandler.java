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

}

