package com.cosoros.www.datastructure;

import java.util.Date;

/**
 * Created by dltmd on 3/15/2018.
 */

public class LivestockInfo {
    public LivestockInfo() {
        _valid = false;
    }

    public LivestockInfo(LivestockInfo src) {
        _valid = src.isValid();
        _latitude = src.latitude();
        _longtitude = src.longtitude();
        _altitude = src.altitude();
        _satelliteCount = src.satelliteCount();
        _timestamp = src.timestamp();
        _voltage = src.voltage();
    }

    private boolean _valid;
    private double  _latitude;
    private double  _longtitude;
    private double  _altitude;
    private int     _satelliteCount;
    private Date    _timestamp;
    private float   _voltage;

    public boolean isValid() {
        return _valid;
    }

    public double latitude() {
        return _latitude;
    }

    public double longtitude() {
        return _longtitude;
    }

    public double altitude() {
        return _altitude;
    }

    public int satelliteCount() {
        return _satelliteCount;
    }

    public Date timestamp() {
        return _timestamp;
    }

    public float voltage() {
        return _voltage;
    }

    public void setValues(double latitude, double longtitude, double altitude, int satelliteCount, Date timestamp, float voltage) {
        _latitude = latitude;
        _longtitude = longtitude;
        _altitude = altitude;
        _satelliteCount = satelliteCount;
        _timestamp = timestamp;
        _voltage = voltage;
        _valid = true;
    }
}