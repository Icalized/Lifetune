package Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        String CREATE_AWAKE_TABLE = "CREATE TABLE " + Util.DATABASE_TABLE + "( " + Util.KEY_ID + " INTEGER PRIMARY KEY,"
                + Util.KEY_BPM + " TEXT," + Util.KEY_SPO2 + " TEXT," + ")";
        db.execSQL(CREATE_AWAKE_TABLE);
    }

    // dropping the
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String CLOSE_DATABASE = "DROP TABLE IF EXISTS " + Util.DATABASE_TABLE;
        db.execSQL(CLOSE_DATABASE);
        onCreate(db);
    }
    // Adding the vitals to the db
    public void addData(Vitals vitals){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Util.KEY_BPM, vitals.getBpo2());
        values.put(Util.KEY_SPO2, vitals.getSpo2());
        values.put(Util.KEY_ID,vitals.getTime());
        db.insert(Util.DATABASE_TABLE,null,values);
        db.close();
    }
    //Fetching values in the db
    public List<Vitals> fetchData(){
        SQLiteDatabase db = this.getReadableDatabase();
        List<Vitals> list = new ArrayList<>();
        String SELECT_ALL = "SELECT * FROM " + Util.DATABASE_TABLE;
        Cursor cursor = db.rawQuery(SELECT_ALL,null);
        if(cursor.moveToFirst()){
            do{
                Vitals vitals = new Vitals();
                vitals.setTime(cursor.getInt(0));
                vitals.setBpo2(cursor.getString(1));
                vitals.setSpo2(cursor.getString(2));
                list.add(vitals);
            }while (cursor.moveToNext());
        }
        return list;
    }
}
