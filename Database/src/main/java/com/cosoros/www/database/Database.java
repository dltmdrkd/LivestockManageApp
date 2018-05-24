package com.cosoros.www.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import com.cosoros.www.datastructure.LivestockInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.TreeSet;


public class Database extends SQLiteOpenHelper {

    private static Database mInstance = null;
    private static final int _DATABASE_VERSION = 2;
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

        db.execSQL(_table._pinTable._create_table);

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

        db.execSQL("DROP TABLE IF EXISTS " + _table._pinTable._table_name);

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

        if (info.repeater() != "0000") {
            values.put(_table._lwdHistory._data_repeater, info.repeater());
        }

        values.put(_table._lwdHistory._data_time, _dateFormat.format(info.timestamp()));
        values.put(_table._lwdHistory._data_battery, info.voltage());

        db.insert(_table._lwdHistory._table_name, null, values);

        db.close();
        Log.d("DATABASE", "DB INSERT DONE");
    }

    public void insertSample(String sample) {
        SQLiteDatabase db = this.getWritableDatabase();

        switch(sample) {
            case "1":
                // jvil
                ContentValues values = new ContentValues();
                values.put(_table._lwdHistory._lwd_id, "A504");
                values.put(_table._lwdHistory._ls_id, "A0001M");
                values.put(_table._lwdHistory._data_origin, "[0042A504A50FA0047.996964^0107.584976^1581.30^10^20180404^010043^07.20^]");
                values.put(_table._lwdHistory._data_latitude, "37.308690");
                values.put(_table._lwdHistory._data_longitude, "126.992449");
                values.put(_table._lwdHistory._data_altitude, "150.10");
                values.put(_table._lwdHistory._data_satellite_cnt, "8");
                values.put(_table._lwdHistory._data_time, "20180518100113");
                values.put(_table._lwdHistory._data_battery, "7.19999980926514");
                values.put(_table._lwdHistory._data_repeater, "0000");
                values.put(_table._lwdHistory._user_latitude, "37.302443");
                values.put(_table._lwdHistory._user_longitude, "127.014329");

                db.insert(_table._lwdHistory._table_name, null, values);
                break;
            case "2":
                // sung univ 37.293615, 126.975098
                ContentValues values2 = new ContentValues();
                values2.put(_table._lwdHistory._lwd_id, "A502");
                values2.put(_table._lwdHistory._ls_id, "A0001M");
                values2.put(_table._lwdHistory._data_origin, "[0042A504A50FA0047.996964^0107.584976^1581.30^10^20180404^010043^07.20^]");
                values2.put(_table._lwdHistory._data_latitude, "37.293615");
                values2.put(_table._lwdHistory._data_longitude, "126.975098");
                values2.put(_table._lwdHistory._data_altitude, "150.10");
                values2.put(_table._lwdHistory._data_satellite_cnt, "8");
                values2.put(_table._lwdHistory._data_time, "20180518101113");
                values2.put(_table._lwdHistory._data_battery, "7.19999980926514");
                values2.put(_table._lwdHistory._data_repeater, "0000");
                values2.put(_table._lwdHistory._user_latitude, "37.302443");
                values2.put(_table._lwdHistory._user_longitude, "127.014329");

                db.insert(_table._lwdHistory._table_name, null, values2);
                break;
            case "3":
                // samsung
                ContentValues values3 = new ContentValues();
                values3.put(_table._lwdHistory._lwd_id, "A505");
                values3.put(_table._lwdHistory._ls_id, "A0001M");
                values3.put(_table._lwdHistory._data_origin, "[0042A504A50FA0047.996964^0107.584976^1581.30^10^20180404^010043^07.20^]");
                values3.put(_table._lwdHistory._data_latitude, "37.253930");
                values3.put(_table._lwdHistory._data_longitude, "127.048457");
                values3.put(_table._lwdHistory._data_altitude, "150.10");
                values3.put(_table._lwdHistory._data_satellite_cnt, "8");
                values3.put(_table._lwdHistory._data_time, "20180518102013");
                values3.put(_table._lwdHistory._data_battery, "7.19999980926514");
                values3.put(_table._lwdHistory._data_repeater, "0000");
                values3.put(_table._lwdHistory._user_latitude, "37.302443");
                values3.put(_table._lwdHistory._user_longitude, "127.014329");

                db.insert(_table._lwdHistory._table_name, null, values3);
                break;
            default:
                break;
        }

        db.close();
        Log.d("DATABASE", "DB-SAMPLE INSERT DONE");
    }

    public void insertPin(int category, String pinName, double lat, double lon) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(_table._pinTable._pin_category, category);
        values.put(_table._pinTable._pin_name, pinName);
        values.put(_table._pinTable._pin_lat, lat);
        values.put(_table._pinTable._pin_lon, lon);

        db.insert(_table._pinTable._table_name, null, values);
        db.close();
        Log.d("DATABASE", "DB-PIN INSERT DONE");
    }

    public TreeSet<String> updatePinList() {
        SQLiteDatabase db = this.getReadableDatabase();

        TreeSet<String> pinKey = new TreeSet<>();
        String sql = "SELECT pin_name FROM pin_table ORDER BY pin_name";

        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                pinKey.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return pinKey;
    }

    public void deletePin(ArrayList pinList) {
        SQLiteDatabase db = this.getWritableDatabase();

        String sql = "DELETE FROM pin_table WHERE pin_name in (";


        for(int i = 0; i < pinList.size() - 1; i++) {
            sql += "'" + pinList.get(i) + "', ";
        }
        sql += "'" + pinList.get(pinList.size() - 1) + "')";

        db.execSQL(sql);
        db.close();
    }

    public JSONObject readLast() throws JSONException {
        Log.d("DATABASE", "DB READ LAST START");

        SQLiteDatabase db = this.getWritableDatabase();

        // read last data
        // if you want to change utc-time to local time : utc_time -> datetime(utc_time, 'localtime')
        String sql =
                " SELECT lwd_id, data_latitude, data_longitude, data_altitude, datetime(utc_time, 'localtime') local_time " +
                " FROM lwd_history a, " +
                "      (SELECT lwd_id id, MAX(utc_time) time " +
                "       FROM lwd_history" +
                "       GROUP BY lwd_id) b " +
                " WHERE a.lwd_id = b.id" +
                "   AND a.utc_time = b.time " +
                " ORDER BY lwd_id; ";

        JSONObject readData = new JSONObject();
        JSONArray key = new JSONArray();
        JSONObject data = new JSONObject();

        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                int i = 0;
                String lwd_id;
                JSONObject dataDetail = new JSONObject();

                lwd_id = cursor.getString(i++);
                key.put(lwd_id);
                dataDetail.put("lat", cursor.getString(i++));
                dataDetail.put("lon", cursor.getString(i++));
                dataDetail.put("alt", cursor.getString(i++));
                dataDetail.put("time", cursor.getString(i));
                data.put(lwd_id, dataDetail);

            } while (cursor.moveToNext());
        }

        cursor.close();


        // read pin data
        String sqlPin = "SELECT * FROM pin_table ORDER BY pin_name;";
        JSONArray pinKey = new JSONArray();
        JSONObject pinData = new JSONObject();

        cursor = db.rawQuery(sqlPin, null);
        if (cursor.moveToFirst()) {
            do {
                int i = 0;
                String pin_name;
                JSONObject dataDetail = new JSONObject();

                dataDetail.put("category", cursor.getString(i++));
                pin_name = cursor.getString(i++);
                dataDetail.put("lat", cursor.getString(i++));
                dataDetail.put("lon", cursor.getString(i));
                pinKey.put(pin_name);
                pinData.put(pin_name, dataDetail);

            } while (cursor.moveToNext());
        }


        readData.put("key", key);
        readData.put("data", data);
        readData.put("pinKey", pinKey);
        readData.put("pinData", pinData);

        db.close();
        Log.d("DATABASE", "DB READ-LAST DONE");
        Log.d("DATABASE", "------------------------------------------------------------------------------EOF");
        return readData;
    }

    public ArrayList read(JSONObject filter, boolean order) throws JSONException {

        String sql
                = " SELECT " +
                "     datetime(utc_time, 'localtime') local_time, lwd_id, " +
                "     (case data_repeater " +
                "           when '0000' then '-' " +
                "           else data_repeater " +
                "       end) data_repeater, " +
                "     lwd_id, ls_id, " +
                "     substr(data_time, 1, 4) || '/' || substr(data_time, 5, 2) || '/' || substr(data_time, 7, 2) || ' ' || " +
                "     substr(data_time, 9, 2) || ':' || substr(data_time, 11, 2) || ':' || substr(data_time, 13, 2) data_time, " +
                "     data_latitude, data_longitude, data_altitude, data_satellite_cnt, data_battery, data_origin ";
        sql = sql + "FROM lwd_history ";

        String sqlWhere = " WHERE 1 = 1 ";
        String sqlOrder;

        // device id filter
        if (!filter.isNull("device")) {
            JSONArray deviceFilter = filter.getJSONArray("device");
            sqlWhere += " AND ( ";

            for (int i = 0; i < deviceFilter.length(); i++) {
                sqlWhere += " lwd_id = '" + deviceFilter.getString(i);
                if (i + 1 < deviceFilter.length()) {
                    sqlWhere += "' OR ";
                }
            }

            sqlWhere += "') ";
        }

        // repeater filter
        if (!filter.isNull("repeater")) {
            switch (filter.getString("repeater")) {
                case "REPEATER":
                    sqlWhere += " AND data_repeater != '0000' ";
                    break;
                case "NO_REPEATER":
                    sqlWhere += " AND data_repeater = '0000' ";
                    break;
                case "ANYWHERE":
                default:
                    break;
            }
        }

        // true : order by desc   /   false : order by asc
        if (order) {
            sqlOrder = " ORDER BY utc_time desc; ";
        } else {
            sqlOrder = " ORDER BY utc_time asc; ";
        }

        sql = sql + sqlWhere + sqlOrder;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(sql, null);

        ArrayList readData = new ArrayList();
        ArrayList columnName = new ArrayList(Arrays.asList(cursor.getColumnNames()));

        if (cursor.moveToFirst()) {
            do {
                ArrayList rowData = new ArrayList();
                for (int i = 0; i < columnName.size(); i++) {
                    rowData.add(cursor.getString(i));
                }

                readData.add(rowData);

            } while(cursor.moveToNext());
        }
        cursor.close();

        readData.add(columnName);
        db.close();
        return readData;
    }
}

