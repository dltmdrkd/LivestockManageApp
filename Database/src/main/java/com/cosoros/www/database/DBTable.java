package com.cosoros.www.database;

import java.util.ArrayList;

/**
 * Created by Jan on 2018-03-19.
 *
 * ls : Livestock
 * ht : Health
 * ds : Disease
 * vc : Vaccine
 *
 */

public class DBTable {
    LwdHistory _lwdHistory = new LwdHistory();
    LwdLsMatch _lwdLsMatch = new LwdLsMatch();
    LwdList _lwdList = new LwdList();
    LwdNotSent _lwdNotSent = new LwdNotSent();

    LsInfo _lsInfo = new LsInfo();
    LsVaccination _lsVaccination = new LsVaccination();
    LsFamily _lsFamily = new LsFamily();

    CodeLsType _codeLsType = new CodeLsType();
    CodeLsKind _codeLsKind = new CodeLsKind();
    CodeVaccine _codeVaccine = new CodeVaccine();

    public String getNameLwdHistory() { return _lwdHistory._table_name; }
    public String getNameLwdLsMatch() { return _lwdLsMatch._table_name; }
    public String getNameLwdList() { return _lwdList._table_name; }
    public String LwdNotSent() { return _lwdNotSent._table_name; }

    public String getNameLsInfo() { return _lsInfo._table_name; }
    public String getNameLsVaccination() { return _lsVaccination._table_name; }
    public String getNameLsFamily() { return _lsFamily._table_name; }

    public String getNameCodeLsType() { return _codeLsType._table_name; }
    public String getNameCodeLsKind() { return _codeLsKind._table_name; }
    public String getNameCodeVaccine() { return _codeVaccine._table_name; }

    public ArrayList getColumnName(String table) {
        ArrayList temp = new ArrayList();
        if (table.equals(_lwdHistory._table_name))
            temp = _lwdHistory.getColumnName();

        return temp;
    }

}

class LwdHistory {
    String _table_name = "lwd_history";
    String _lwd_id = "lwd_id";
    String _ls_id = "ls_id";
    String _data_origin = "data_origin";
    String _data_latitude = "data_latitude";
    String _data_longitude = "data_longitude";
    String _data_altitude = "data_altitude";
    String _data_satellite_cnt = "data_satellite_cnt";
    String _data_time = "data_time";
    String _data_battery = "data_battery";
    String _primary_key = "PRIMARY KEY (lwd_id, data_time)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _lwd_id + " TEXT NOT NULL, " + _ls_id + " TEXT, " + _data_origin + " TEXT, " +
                    _data_latitude + " REAL, " + _data_longitude + " REAL, " + _data_altitude + " REAL, " +
                    _data_satellite_cnt + " INTEGER, " + _data_time + " TEXT, " + _data_battery + " REAL, " +
                    _primary_key + ");";


    ArrayList getColumnName() {
        ArrayList columnName = new ArrayList();

        columnName.add(_lwd_id);
        columnName.add(_ls_id);
        columnName.add(_data_time);
        columnName.add(_data_latitude);
        columnName.add(_data_longitude);
        columnName.add(_data_altitude);
        columnName.add(_data_satellite_cnt);
        columnName.add(_data_battery);
        columnName.add(_data_origin);

        return columnName;
    }
}

class LwdLsMatch {
    String _table_name = "lwd_ls_match";
    String _lwd_id = "lwd_id";
    String _ls_id = "ls_id";
    String _lwd_time = "lwd_time";
    String _primary_key = "PRIMARY KEY (lwd_id, ls_id)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _lwd_id + " TEXT, " + _ls_id + " TEXT, " + _lwd_time + " TEXT, " +
                    _primary_key + ");";
}
class LwdList {
    String _table_name = "lwd_list";
    String _lwd_id = "lwd_id";
    String _lwd_name = "lwd_name";
    String _primary_key = "PRIMARY KEY (lwd_id, lwd_name)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _lwd_id + " TEXT, " + _lwd_name + " TEXT, " +
                    _primary_key + ");";
}

class LwdNotSent {
    String _table_name = "lwd_list";
    String _lwd_id = "lwd_id";
    String _lwd_time = "lwd_time";
    String _primary_key = "PRIMARY KEY (lwd_id, lwd_time)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _lwd_id + " TEXT, " + _lwd_time + " TEXT, " +
                    _primary_key + ");";
}

class LsInfo {
    String _table_name = "ls_info";
    String _ls_id = "ls_id";
    String _ls_type = "ls_type";
    String _ls_kind = "ls_kind";
    String _ls_birth = "ls_birth";
    String _ls_sex = "ls_sex";
    String _ls_name = "ls_name";
    String _ls_weight = "ls_weight";
    String _primary_key = "PRIMARY KEY (ls_id)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _ls_id + " TEXT , " + _ls_type + " INTEGER, " + _ls_kind + " INTEGER, " +
                    _ls_birth + " TEXT, " + _ls_sex + " INTEGER, " + _ls_name + " TEXT, " + _ls_weight + " REAL, " +
                    _primary_key + ");";
}

class LsVaccination {
    String _table_name = "ls_vaccination";
    String _ls_id = "ls_id";
    String _ls_code = "ls_code";
    String _ls_date = "ls_date";
    String _primary_key = "PRIMARY KEY (ls_id, ls_code, ls_date)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _ls_id + " TEXT, " + _ls_code + " INTEGER, " + _ls_date + " TEXT, " +
                    _primary_key + ");";
}

class LsFamily {
    String _table_name = "ls_family";
    String _ls_id = "ls_id";
    String _ls_father = "ls_father";
    String _ls_mother = "ls_mother";
    String _primary_key = "PRIMARY KEY (ls_id, ls_father, ls_mother)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _ls_id + " TEXT, " + _ls_father + " TEXT, " + _ls_mother + " TEXT, " +
                    _primary_key + ");";
}

class CodeLsType {
    String _table_name = "code_ls_type";
    String _ls_type_code = "ls_type_code";
    String _ls_type_name = "ls_type_name";
//    String _primary_key = "PRIMARY KEY (lwd_id, lwd_time)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _ls_type_code + " TEXT, " + _ls_type_name + " TEXT);";
}

class CodeLsKind {
    String _table_name = "code_ls_kind";
    String _ls_kind_code = "ls_kind_code";
    String _ls_kind_name = "ls_kind_name";
//    String _primary_key = "PRIMARY KEY (lwd_id, lwd_time)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _ls_kind_code + " TEXT, " + _ls_kind_name + " TEXT);";
}

class CodeVaccine {
    String _table_name = "code_vaccine";
    String _vc_code = "vc_code";
    String _vc_name = "vc_name";
//    String _primary_key = "PRIMARY KEY (lwd_id, lwd_time)";

    String _create_table =
            "CREATE TABLE IF NOT EXISTS " + _table_name + "(" +
                    _vc_code + " TEXT, " + _vc_name + " TEXT);";
}