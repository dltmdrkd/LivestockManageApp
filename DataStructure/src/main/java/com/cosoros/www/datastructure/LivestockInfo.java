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
        _repeater = src.repeater();
        _destination = src.destination();
        _latitude = src.latitude();
        _longtitude = src.longitude();
        _altitude = src.altitude();
        _satelliteCount = src.satelliteCount();
        _timestamp = src.timestamp();
        _voltage = src.voltage();
        _hwVersion = src.hwVersion();
        _fwVersion = src.fwVersion();
    }

    private String  _source;
    private String  _repeater;
    private String  _destination;
    private boolean _valid;
    private double  _latitude;
    private double  _longtitude;
    private double  _altitude;
    private int     _satelliteCount;
    private Date    _timestamp;
    private float   _voltage;
    private Version _hwVersion;
    private Version _fwVersion;

    public boolean isValid() {
        return _valid;
    }

    public String source() { return _source; }
    public String repeater() { return _repeater; }
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
    public Version hwVersion() { return _hwVersion; }
    public Version fwVersion() { return _fwVersion; }

    public void setValues(String source, String repeater, String destination, double latitude, double longitude, double altitude, int satelliteCount, Date timestamp, float voltage, Version hwVersion, Version fwVersion) {
        _source = source;
        _repeater = repeater;
        _destination = destination;
        _latitude = latitude;
        _longtitude = longitude;
        _altitude = altitude;
        _satelliteCount = satelliteCount;
        _timestamp = timestamp;
        _voltage = voltage;
        _hwVersion = hwVersion;
        _fwVersion = fwVersion;
        _valid = true;
    }
}