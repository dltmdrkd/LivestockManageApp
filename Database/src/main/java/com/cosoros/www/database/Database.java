package com.cosoros.www.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Date;

public class Database extends SQLiteOpenHelper {

    private static final int _DATABASE_VERSION = 2;
    private static final String _DB_NAME = "nomad_lwd.db";
    private static final DBTable _table = new DBTable();

    public Database(Context context) {
        super(context, _DB_NAME, null, _DATABASE_VERSION);
        Log.d("DATABASE", "DB CONSTRUCT START");
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
        db.execSQL("DROP TABLE IF EXISTS " + _table._lwdList._create_table);
        db.execSQL("DROP TABLE IF EXISTS " + _table._lwdNotSent._create_table);

        db.execSQL("DROP TABLE IF EXISTS " + _table._lsInfo._create_table);
        db.execSQL("DROP TABLE IF EXISTS " + _table._lsVaccination._create_table);
        db.execSQL("DROP TABLE IF EXISTS " + _table._lsFamily._create_table);

        db.execSQL("DROP TABLE IF EXISTS " + _table._codeLsType._create_table);
        db.execSQL("DROP TABLE IF EXISTS " + _table._codeLsKind._create_table);
        db.execSQL("DROP TABLE IF EXISTS " + _table._codeVaccine._create_table);
        onCreate(db);

        Log.d("DATABASE", "DB UPDATE DONE");
    }

    public void add() {
        Log.d("DATABASE", "DB INSERT START");

//        SQLiteDatabase db = this.getWritableDatabase();
//        Date t = new Date();
//        String sql = "INSERT INTO lwd_history (lwd_id, ls_id, data_time) VALUES ('101F', 'A0001M', '" + t.getTime() + "');";
//
//        db.execSQL(sql);


        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        Date t = new Date();
        values.put(_table._lwdHistory._lwd_id, "102F");
        values.put(_table._lwdHistory._ls_id, "A0001M");
        values.put(_table._lwdHistory._data_time, t.getTime());

        db.insert(_table._lwdHistory._table_name, null, values);
        Log.d("DATABASE", "DB INSERT DONE");
    }

    public void read() {
        Log.d("DATABASE", "DB READ START");
        SQLiteDatabase db = this.getWritableDatabase();

        String sql2 = "SELECT lwd_id, ls_id, data_time FROM lwd_history ORDER BY data_time;";
        Cursor cursor = db.rawQuery(sql2, null);

        if (cursor.moveToFirst()) {
            do {

                Log.d("DATABASE", "lwd_id : " + cursor.getString(0));
                Log.d("DATABASE", "ls_id : " + cursor.getString(1));
                Log.d("DATABASE", "data_time : " + cursor.getString(2));

            } while (cursor.moveToNext());
        }
        Log.d("DATABASE", "DB READ DONE");
    }

//    public List<Drink> getAll() {
//
//        DBTable table = new DBTable();
//        String name = table._lwdHistory._name;
//
//
//        List<Drink> drinkList = new ArrayList<Drink>();
//
//        String SELECT_ALL = "SELECT * FROM " + TABLE_DRINK;
//
//        SQLiteDatabase db = this.getWritableDatabase();
//        Cursor cursor = db.rawQuery(SELECT_ALL, null);
//
//        if (cursor.moveToFirst()) {
//            do {
//                Drink drink = new Drink();
//                drink.setId(Integer.parseInt(cursor.getString(0)));
//                drink.setName(cursor.getString(1));
//                drinkList.add(drink);
//            } while (cursor.moveToNext());
//        }
//
//        return drinkList;
//    }

}
