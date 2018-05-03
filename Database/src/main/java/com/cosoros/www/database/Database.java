package com.cosoros.www.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import com.cosoros.www.datastructure.LivestockInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public void insert(String name, String origin, LivestockInfo info, Pair<Double, Double> userLocation) {
        Log.d("DATABASE", "DB INSERT START");

        if (!info.isValid()) {
            Log.d("DATABASE", "DATA PARSING ERROR");
            return;
        }

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
//        values.put(_table._lwdHistory._data_repeat, info.dataRepeat());
        values.put(_table._lwdHistory._user_latitude, userLocation.first);
        values.put(_table._lwdHistory._user_longitude, userLocation.second);

//        if (info.repeat() != "0000") {
//            values.put(_table._lwdHistory._data_repeat, info.repeat());

        values.put(_table._lwdHistory._data_time, _dateFormat.format(info.timestamp()));
        values.put(_table._lwdHistory._data_battery, info.voltage());

        db.insert(_table._lwdHistory._table_name, null, values);

        Log.d("DATABASE", "DB INSERT DONE");
    }

    public JSONObject readLast() throws JSONException {
        Log.d("DATABASE", "DB READ LAST START");

        SQLiteDatabase db = this.getWritableDatabase();

        // local time : utc_time -> datetime(utc_time, 'localtime')
        String sql =
                "SELECT lwd_id, data_latitude, data_longitude, data_altitude, utc_time " +
                "FROM lwd_history a, " +
                "     (SELECT lwd_id id, MAX(utc_time) time " +
                "      FROM lwd_history" +
                "      GROUP BY lwd_id) b " +
                "WHERE a.lwd_id = b.id" +
                "  AND a.utc_time = b.time " +
                "ORDER BY lwd_id";

//        ArrayList readData = new ArrayList();
//        ArrayList columnData = new ArrayList();

        JSONObject readData = new JSONObject();
        JSONArray key = new JSONArray();
        JSONObject data = new JSONObject();
        JSONObject dataDetail = new JSONObject();

        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                int i = 0;
                String lwd_id;

                lwd_id = cursor.getString(i++);
                key.put(lwd_id);
                dataDetail.put("lat", cursor.getString(i++));
                dataDetail.put("lon", cursor.getString(i++));
                dataDetail.put("alt", cursor.getString(i++));
                dataDetail.put("time", cursor.getString(i++));
                data.put(lwd_id, dataDetail);

                Log.d("DATABASE", "READ-LAST DATA");

            } while (cursor.moveToNext());
        }

        cursor.close();

        readData.put("key", key);
        readData.put("data", data);

        Log.d("DATABASE", "DB READ-LAST DONE");
        Log.d("DATABASE", "------------------------------------------------------------------------------EOF");
        return readData;
    }

    public ArrayList read(String table) {
        Log.d("DATABASE", "DB READ START");

        SQLiteDatabase db = this.getWritableDatabase();
        String sql
                = "SELECT "
                + _table._lwdHistory._utc_time + ", "
                + "(case " +  _table._lwdHistory._data_repeat
                + "    when '0000' then '-' "
                + "    else " + _table._lwdHistory._data_repeat
                + " end) data_repeat, "
                + _table._lwdHistory._lwd_id + ", "
                + _table._lwdHistory._ls_id + ", "
                + "substr(" + _table._lwdHistory._data_time + ", 1, 4)"
                + " || '/' || substr(" + _table._lwdHistory._data_time + ", 5, 2)"
                + " || '/' || substr(" + _table._lwdHistory._data_time + ", 7, 2)"
                + " || ' ' || substr(" + _table._lwdHistory._data_time + ", 9, 2)"
                + " || ':' || substr(" + _table._lwdHistory._data_time + ", 11, 2)"
                + " || ':' || substr(" + _table._lwdHistory._data_time + ", 13, 2) as time, "
                + _table._lwdHistory._data_latitude + ", "
                + _table._lwdHistory._data_longitude + ", "
                + _table._lwdHistory._data_altitude + ", "
                + _table._lwdHistory._data_satellite_cnt + ", "
                + _table._lwdHistory._data_battery + ", "
                + _table._lwdHistory._data_origin
                + " FROM " + table
                + " ORDER BY utc_time DESC;";

        ArrayList readData = new ArrayList();
        ArrayList columnName = _table.getColumnName(table);
        Cursor cursor = db.rawQuery(sql, null);
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

