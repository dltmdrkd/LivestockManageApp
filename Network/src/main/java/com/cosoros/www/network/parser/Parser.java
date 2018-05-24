package com.cosoros.www.network.parser;

import com.cosoros.www.datastructure.LivestockInfo;
import com.cosoros.www.datastructure.Version;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

/*
 * Created by dltmd on 3/16/2018.
 * Parser received packet from Receiver through Bluetooth module.
 * Format : [(1) LENGTH(4) SRC_ID(4) RPT_ID(4) DST_ID(4) CMD(2) LATITUDE(V) ^(1) LONGITUDE(V) ^(1) ALTITUDE(V) ^(1) CNT_SAT(V) ^(1) DATE(8) ^(1) TIME(6) ^(1) VOLTAGE(V) ^(1) HARDWARE_VER(V) ^(1) SOFTWARE_VER(V) ^(1) ](1)
 */

public class Parser {
    public Parser() {
    }

    static final String _startChar = "[";
    static final String _endChar = "]";
    static final String _delimiter = "^";
    static final String _verDelimiter = ".";

    static final int _countDatasize = 4;
    static final int _countSrc = 4, _countRpt = 4, _countDst = 4;
    static final int _countCmd = 2;
    static final int _headerSize = _countDatasize + _countSrc + _countRpt + _countDst + _countCmd;

    enum DATAINDEX {
        LAT,     // latitude.
        LON,     // longitude.
        ALT,     // altitude.
        CNTSAT, // count of satellite.
        DATE,   // date.
        TIME,   // time.
        VOL,    // voltage.
        HWVER,  // hardware version.
        FWVER,  // firmware version.
        INDEXCOUNT,
    }

    protected static String split(String data, int pos, int length) {
        return data.substring(pos, pos + length);
    }

    protected static String getString(String[] tokens, DATAINDEX index) {
        return tokens[index.ordinal()];
    }

    public static LivestockInfo parse(String packet) {
        LivestockInfo info = new LivestockInfo();

        if (packet.startsWith(_startChar) == true && packet.endsWith(_endChar) == true) {
            packet = packet.substring(1, packet.length() - 1); // remove "[" and "]".
        }
        else {
            // invalid packet.
            return info;
        }

        String header = packet.substring(0, _headerSize);
        String data = packet.substring(_headerSize, packet.length());

        // parse header info.
        String source = header.substring(4, 8);
        String repeater = header.substring(8, 12);
        String destination = header.substring(12, 16);
        String command = header.substring(16, 18);

        StringTokenizer tokenizer = new StringTokenizer(data, _delimiter);

        if (tokenizer.hasMoreTokens() != true) return info;
        int tokenCount = tokenizer.countTokens();
        if (tokenCount <= 0) return info;

        String [] tokens = new String[tokenCount];

        for (int i = 0; i < tokenCount; ++i) {
            tokens[i] = tokenizer.nextToken();
        }

        if (tokenCount < DATAINDEX.INDEXCOUNT.ordinal()) return info;

        double latitude = Double.parseDouble(getString(tokens, DATAINDEX.LAT));
        double longitude = Double.parseDouble(getString(tokens, DATAINDEX.LON));
        double altitude = Double.parseDouble(getString(tokens, DATAINDEX.ALT));
        int satelliteCount = Integer.parseInt(getString(tokens, DATAINDEX.CNTSAT));
        String datetimeStr = getString(tokens, DATAINDEX.DATE) + getString(tokens, DATAINDEX.TIME);
        SimpleDateFormat formatFromString = new SimpleDateFormat("yyyyMMddHHmmss");
        formatFromString.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date datetime;
        try {
            datetime = formatFromString.parse(datetimeStr);
        }
        catch(ParseException e) {
            e.printStackTrace();
            return info;
        }
        float voltage = Float.parseFloat(getString(tokens, DATAINDEX.VOL));

        String hwVersionStr = getString(tokens, DATAINDEX.HWVER);
        StringTokenizer hwverTokenizer = new StringTokenizer(hwVersionStr, _verDelimiter);
        if (hwverTokenizer.countTokens() != 3) return info;

        Version hwVersion = new Version(Integer.parseInt(hwverTokenizer.nextToken()),
                                        Integer.parseInt(hwverTokenizer.nextToken()),
                                        Integer.parseInt(hwverTokenizer.nextToken()));

        String fwVersionStr = getString(tokens, DATAINDEX.FWVER);
        StringTokenizer fwverTokenizer = new StringTokenizer(fwVersionStr, _verDelimiter);
        if (fwverTokenizer.countTokens() != 3) return info;

        Version fwVersion = new Version(Integer.parseInt(fwverTokenizer.nextToken()),
                                        Integer.parseInt(fwverTokenizer.nextToken()),
                                        Integer.parseInt(fwverTokenizer.nextToken()));

        info.setValues(source, repeater, destination, latitude, longitude, altitude, satelliteCount, datetime, voltage, hwVersion, fwVersion);
        return info;
    }

    public static LivestockInfo parse(String source, JSONObject dataDetail) throws JSONException {
        LivestockInfo info = new LivestockInfo();
        Double latitude, longitude, altitude;
        String utcTime;

        latitude = dataDetail.getDouble("lat");
        longitude = dataDetail.getDouble("lon");
        altitude = dataDetail.getDouble("alt");
        utcTime = dataDetail.getString("time");

        SimpleDateFormat formatFromString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date datetime;
        try {
            datetime = formatFromString.parse(utcTime);
        }
        catch(ParseException e) {
            e.printStackTrace();
            return info;
        }

        info.setValues(source, "", "", latitude, longitude, altitude, 0, datetime, 0.0f, new Version(0,0,0), new Version(0,0,0));
        return info;
    }
}
