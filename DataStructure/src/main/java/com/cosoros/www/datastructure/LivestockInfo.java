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
        _source = src.source();
        _destination = src.destination();
        _latitude = src.latitude();
        _longtitude = src.longitude();
        _altitude = src.altitude();
        _satelliteCount = src.satelliteCount();
        _timestamp = src.timestamp();
        _voltage = src.voltage();
    }

    private String  _source;
    private String  _destination;
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

    public String source() { return _source; }
    public String destination() { return _destination; }
    public double latitude() { return _latitude; }
    public double longitude() {
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

    public void setValues(String source, String destination, double latitude, double longitude, double altitude, int satelliteCount, Date timestamp, float voltage) {
        _source = source;
        _destination = destination;
        _latitude = latitude;
        _longtitude = longitude;
        _altitude = altitude;
        _satelliteCount = satelliteCount;
        _timestamp = timestamp;
        _voltage = voltage;
        _valid = true;
    }
}