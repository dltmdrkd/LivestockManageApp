package com.cosoros.www.network;

import com.cosoros.www.datastructure.LivestockInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dltmd on 3/16/2018.
 * Parser received packet from Receiver through Bluetooth module.
 * Format : [(1) LENGTH(4) SRC_ID(4) DST_ID(4) CMD(2) LATITUDE(12) ^(1) LONGTITUDE(12) ^(1) ALTITUDE(6) ^(1) CNT_SAT(2) ^(1) DATE(8) ^(1) TIME(6) ^(1) VOLTAGE(5) ^(1) ](1)
 * Example : [ 0048 0002 007f A0 000036.08055 ^ 000129.39787 ^ 027.00 ^ 03 ^ 20180316 ^ 151233 ^ 03.50 ^ ]
 */

public class Parser {
    public Parser() {
    }

    static final int posLatitude = 15, countLatitude = 12;
    static final int posLongtitude = 28, countLongtitude = 12;
    static final int posAltitude = 41, countAltitude = 6;
    static final int posSatelliteCount = 48, countSatelliteCount = 2;
    static final int posDate = 51, countDate = 8;
    static final int posTime = 60, countTime = 6;
    static final int posVoltage = 67, countVoltage = 5;

    protected static String split(String data, int pos, int length) {
        return data.substring(pos, pos + length);
    }

    public static LivestockInfo parse(String data) {
        LivestockInfo info = new LivestockInfo();

        if (data.length() == Integer.parseInt("4A", 16)) {
            // only process valid data length.
            double latitude = Double.parseDouble(split(data, posLatitude, countLatitude));
            double longtitude = Double.parseDouble(split(data, posLongtitude, countLongtitude));
            double altitude = Double.parseDouble(split(data, posAltitude, countAltitude));
            int satelliteCount = Integer.parseInt(split(data, posSatelliteCount, countSatelliteCount));
            String datetimeStr = split(data, posDate, countDate);
            datetimeStr = datetimeStr + split(data, posTime, countTime);
            SimpleDateFormat formatFromString = new SimpleDateFormat("yyyyMMddHHmmss");
            Date datetime;
            try {
                datetime = formatFromString.parse(datetimeStr);
            }
            catch(ParseException e) {
                e.printStackTrace();
                return info;
            }
            float voltage = Float.parseFloat(split(data, posVoltage, countVoltage));
            info.setValues(latitude, longtitude, altitude, satelliteCount, datetime, voltage);
        }
        return info;
    }
}
