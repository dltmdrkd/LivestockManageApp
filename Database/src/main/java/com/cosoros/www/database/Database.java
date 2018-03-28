package com.cosoros.www.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.cosoros.www.datastructure.LivestockInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;


public class Database extends SQLiteOpenHelper {

    private static Database mInstance = null;
    private static final int _DATABASE_VERSION = 1;
    private static final String _DB_NAME = "nomad_lwd.db";
    private static final DBTable _table = new DBTable();
    private SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private TimeZone _timeZone = TimeZone.getTimeZone("Asia/Seoul");

    private Database(Context context) {
        super(context, _DB_NAME, null, _DATABASE_VERSION);
        _dateFormat.setTimeZone(_timeZone);
        Log.d("DATABASE", "DB CONSTRUCT START");
    }

    public static Database getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Database(context.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public synchronized void close() {
        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("DATABASE", "DB CREATE START");
        db.execSQL(_table._lwdHistory._create_table);
        db.execSQL(_table._lwdLsMatch._create_table);
        db.execSQL(_table._lwdList._create_table);
        db.execSQL(_table._lwdNotSent._create_table);

        db.execSQL(_table._lsInfo._create_table);
        db.execSQL(_table._lsVaccination._create_table);
        db.execSQL(_table._lsFamily._create_table);

        db.execSQL(_table._codeLsType._create_table);
        db.execSQL(_table._codeLsKind._create_table);
        db.execSQL(_table._codeVaccine._create_table);

        Log.d("DATABASE", "DB CREATE DONE");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DATABASE", "DB UPDATE START");

        db.execSQL("DROP TABLE IF EXISTS " + _table._lwdHistory._table_name);
        db.execSQL("DROP TABLE IF EXISTS " + _table._lwdLsMatch._table_name);
        db.execSQL("DROP TABLE IF EXISTS " + _table._lwdList._table_name);
        db.execSQL("DROP TABLE IF EXISTS " + _table._lwdNotSent._table_name);

        db.execSQL("DROP TABLE IF EXISTS " + _table._lsInfo._table_name);
        db.execSQL("DROP TABLE IF EXISTS " + _table._lsVaccination._table_name);
        db.execSQL("DROP TABLE IF EXISTS " + _table._lsFamily._table_name);

        db.execSQL("DROP TABLE IF EXISTS " + _table._codeLsType._table_name);
        db.execSQL("DROP TABLE IF EXISTS " + _table._codeLsKind._table_name);
        db.execSQL("DROP TABLE IF EXISTS " + _table._codeVaccine._table_name);
        onCreate(db);

        Log.d("DATABASE", "DB UPDATE DONE");
    }

    public void insert(String name, String origin, LivestockInfo info) {
        Log.d("DATABASE", "DB INSERT START");

        SQLiteDatabase db = this.getWritableDatabase();

        // get utc time from parser as Date type
//        Date utcTime = new Date(System.currentTimeMillis());
//        String localDate = _dateFormat.format(utcTime);
//        Log.d("DATABASE-TRY", "INSERT-utcTime : " + utcTime);
//        Log.d("DATABASE-TRY", "INSERT-localDate : " + localDate);

        ContentValues values = new ContentValues();

        values.put(_table._lwdHistory._lwd_id, info.source());
        values.put(_table._lwdHistory._ls_id, "A0001M");
        values.put(_table._lwdHistory._data_origin, origin);
        values.put(_table._lwdHistory._data_latitude, info.latitude());
        values.put(_table._lwdHistory._data_longitude, info.longitude());
        values.put(_table._lwdHistory._data_altitude, info.altitude());
        values.put(_table._lwdHistory._data_satellite_cnt, info.satelliteCount());
//        values.put(_table._lwdHistory._data_time, localDate);
        values.put(_table._lwdHistory._data_time, _dateFormat.format(info.timestamp()));
        values.put(_table._lwdHistory._data_battery, info.voltage());

        db.insert(_table._lwdHistory._table_name, null, values);

        Log.d("DATABASE", "DB INSERT DONE");
    }

    public ArrayList read(String table) {
        Log.d("DATABASE", "DB READ START");

        SQLiteDatabase db = this.getWritableDatabase();
        String sql2
                = "SELECT "
                + _table._lwdHistory._lwd_id + ", "
                + _table._lwdHistory._ls_id + ", "
                + _table._lwdHistory._data_time + ", "
                + _table._lwdHistory._data_latitude + ", "
                + _table._lwdHistory._data_longitude + ", "
                + _table._lwdHistory._data_altitude + ", "
                + _table._lwdHistory._data_satellite_cnt + ", "
                + _table._lwdHistory._data_battery + ", "
                + _table._lwdHistory._data_origin
                + " FROM " + table
                + " ORDER BY lwd_id, data_time DESC;";

        ArrayList readData = new ArrayList();
        ArrayList columnName = _table.getColumnName(table);
        Cursor cursor = db.rawQuery(sql2, null);
        if (cursor.moveToFirst()) {
            do {
                ArrayList rowData = new ArrayList();

                for(int i = 0; i < columnName.size(); i++) {
                    Map tempData = new HashMap();
                    tempData.put("colName", columnName.get(i));
                    tempData.put("data", cursor.getString(i));

                    rowData.add(tempData);
                }

                readData.add(rowData);

                Log.d("DATABASE", "READ DATA");

            } while (cursor.moveToNext());
        }
        cursor.close();

        readData.add(columnName);
        Log.d("DATABASE", "DB READ DONE");
        Log.d("DATABASE", "------------------------------------------------------------------------------EOF");
        return readData;
    }
}

