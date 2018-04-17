package com.cosoros.www.network.parser;

import android.util.Log;

import com.cosoros.www.datastructure.LivestockInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dltmd on 3/16/2018.
 * Parser received packet from Receiver through Bluetooth module.
 * Format : [(1) LENGTH(4) SRC_ID(4) DST_ID(4) CMD(2) LATITUDE(10) ^(1) LONGITUDE(11) ^(1) ALTITUDE(6) ^(1) CNT_SAT(2) ^(1) DATE(8) ^(1) TIME(6) ^(1) VOLTAGE(5) ^(1) ](1)
 *           0 1234 5678 9012 34 5678901234 5 67890123456 7 890123 4 56 7 89012345 6 789012 3 45678 9 0
 * Example : [ 0048 0002 007f A0 -36.008055 ^ -009.039787 ^ 027.00 ^ 03 ^ 20180316 ^ 151233 ^ 03.50 ^ ]
 *           [ 0042 0305 030F A0 048.105038 ^ 0106.751366 ^ 1307.00 ^ 09^20180402^084201^07.20^]
 */
//[00420305030FA0048.105038^0106.751366^1307.00^09^20180402^084201^07.20^]
public class Parser {
    public Parser() {
    }

    static final int countDelimiter = 1;

    static final int posStart = 0, countStart = 1;
    static final int posDataSize = posStart + countStart, countDatasize = 4;    // 1
    static final int posSrc = posDataSize + countDatasize, countSrc = 4;    // 5
    static final int posDst = posSrc + countSrc, countDst = 4;  // 9
    static final int posCmd = posDst + countDst, countCmd = 2;  // 13
    static final int posLatitude = posCmd + countCmd, countLatitude = 10;   // 15
    static final int posLongitude = countDelimiter + posLatitude + countLatitude, countLongitude = 11;   // 26
    static final int posAltitude = countDelimiter + posLongitude + countLongitude, countAltitude = 7;  // 38
    static final int posSatelliteCount = countDelimiter + posAltitude + countAltitude, countSatelliteCount = 2;  // 45
    static final int posDate = countDelimiter + posSatelliteCount + countSatelliteCount, countDate = 8;  // 48
    static final int posTime = countDelimiter + posDate + countDate, countTime = 6;   // 57
    static final int posVoltage = countDelimiter + posTime + countTime, countVoltage = 5;    // 64
    static final int posEnd = countDelimiter + posVoltage + countVoltage, countEnd = 1; // 70


    protected static String split(String data, int pos, int length) {
        return data.substring(pos, pos + length);
    }

    public static LivestockInfo parse(String packet) {
        LivestockInfo info = new LivestockInfo();
        int test = packet.length();

        if (packet.length() == posEnd + countEnd) {
            // only process valid packet length.
            String source = split(packet, posSrc, countSrc);
            String destination = split(packet, posDst, countDst);
            double latitude = Double.parseDouble(split(packet, posLatitude, countLatitude));
            double longitude = Double.parseDouble(split(packet, posLongitude, countLongitude));
            double altitude = Double.parseDouble(split(packet, posAltitude, countAltitude));
            int satelliteCount = Integer.parseInt(split(packet, posSatelliteCount, countSatelliteCount));
            String datetimeStr = split(packet, posDate, countDate);
            datetimeStr = datetimeStr + split(packet, posTime, countTime);
            SimpleDateFormat formatFromString = new SimpleDateFormat("yyyyMMddHHmmss");
            Date datetime;
            try {
                datetime = formatFromString.parse(datetimeStr);
            }
            catch(ParseException e) {
                e.printStackTrace();
                return info;
            }
            float voltage = Float.parseFloat(split(packet, posVoltage, countVoltage));
            info.setValues(source, destination, latitude, longitude, altitude, satelliteCount, datetime, voltage);
        } else {
            Log.d("PARSING-ERROR", "Length: " + test);
            Log.d("PARSING-ERROR", "Pakcet: " + packet);
        }
        return info;
    }
}
