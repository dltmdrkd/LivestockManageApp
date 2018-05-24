package com.cosoros.livestockmanageapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cosoros.www.database.Database;
import com.cosoros.www.database.DatabaseActivity;
import com.cosoros.www.datastructure.LivestockInfo;
import com.cosoros.www.network.bluetooth.BluetoothActivity;
import com.cosoros.www.network.bluetooth.BluetoothService;
import com.cosoros.www.network.bluetooth.Constants;
import com.cosoros.www.network.parser.Parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private int _state = BluetoothService.STATE_NONE;
    private String _receiveBuffer = "";
    private Pair<Double, Double> _myGpsLocation = new Pair<>(37.30362, 126.99712);
    private HashMap<String, LivestockInfo> _livestockInfoMap = new HashMap<>();
    private HashMap<String, Integer> _livestockInfoMapColor = new HashMap<>();
    private int colorSet[] = { Color.MAGENTA, Color.BLUE, Color.GRAY, Color.DKGRAY, Color.CYAN, Color.GREEN };
    private HashMap<String, PinInfo> _pinInfoMap = new HashMap<>();
    private CheckerThread _checkerThread;
    private ParserThread _parserThread;
    private Database _dataBase;
    private TreeSet<String> _pinKey = new TreeSet<>();
    private MapView _mapView;
    private float[] _gravity;
    private float[] _geomagnetic;
    private float _azimuth, _pitch, _roll;
    private MagneticSensorListener _magneticListener;
    private boolean _rotateRequested = false;

    private void startLocationService() {
        // 위치 관리자 객체 참조
        LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        // 위치 정보를 받을 리스너 생성
        GPSListener gpsListener = new GPSListener();
        long minTime = 60 * 1000; // 60 seconds.
        float minDistance = 5; // 5 meters.

        try {
            // GPS를 이용한 위치 요청
            manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTime,
                    minDistance,
                    gpsListener);

            Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                Double latitude = lastLocation.getLatitude();
                Double longitude = lastLocation.getLongitude();

                synchronized (_myGpsLocation) {
                    _myGpsLocation = Pair.create(latitude, longitude);
                }
            }
        } catch(SecurityException ex) {
            ex.printStackTrace();
        }
    }

    private void checkDangerousPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        int permissionCheck = PackageManager.PERMISSION_GRANTED;
        for (int i = 0; i < permissions.length; i++) {
            permissionCheck = ContextCompat.checkSelfPermission(this, permissions[i]);
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                break;
            }
        }

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                Toast.makeText(this, "권한 설명 필요함.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, permissions, 1);
            }
        }
    }

    private class GPSListener implements LocationListener {

        public void onLocationChanged(Location location) {
            synchronized (_myGpsLocation) {
                _myGpsLocation = Pair.create(location.getLatitude(), location.getLongitude());

            }
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

    private class MagneticSensorListener implements SensorEventListener {
        float _azimuths[] = new float[8];
        byte _index = 0;

        public MagneticSensorListener() {
            resetValues();
        }

        public void resetValues() {
            Arrays.fill(_azimuths, 0);
            _index = 0;
        }

        private float averageAzimuth() {
            float sum = 0;
            for (int i = 0; i < _azimuths.length; ++i) {
                sum += _azimuths[i];
            }
            return sum / _azimuths.length;
        }

        private void setAzimuthArray(float value, int exceptIndex) {
            for (int i = 0; i < _azimuths.length; ++i) {
                if (i == exceptIndex) continue;
                _azimuths[i] = value;
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                _gravity = event.values;
            }
            else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                _geomagnetic = event.values;
            }
            if (_gravity != null && _geomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, _gravity, _geomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    float azimuth = (int)Math.toDegrees(orientation[0]);
                    int previousIndex = _index - 1;
                    if (previousIndex < 0) previousIndex = 7;

                    if ((azimuth < 5 && azimuth > -5) != true &&
                        (_azimuths[previousIndex] < 0 && azimuth > 0) ||
                        (_azimuths[previousIndex] > 0 && azimuth < 0)) {
                        setAzimuthArray(azimuth, _index);
                    }
//                    if (azimuth >= -180 && azimuth < -170 ) {
//                        setAzimuthArray(azimuth, _index);
//                    }
//                    else if (azimuth <= 180 && azimuth > 170) {
//                        setAzimuthArray(azimuth, _index);
//                    }
                    _azimuths[_index++] = azimuth;
                    if (_index == 8) _index = 0;
                    _azimuth = averageAzimuth();
                    //_pitch = (float)Math.toDegrees(orientation[1]);
                    //_roll = (float)Math.toDegrees(orientation[2]);
                    Log.d("Azimuth","" + _azimuth);
                    _mapView.invalidate();
                }
                _gravity = null;
                _geomagnetic = null;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private final Handler _handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_STATE_CHANGE:
                _state = msg.arg1;
                invalidateOptionsMenu();
                break;
            case Constants.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                // TODO : write log.
                break;
            case Constants.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                synchronized (_receiveBuffer) {
                    _receiveBuffer = _receiveBuffer + readMessage;
                }
                break;
            case Constants.MESSAGE_DEVICE_NAME:
                // save the connected device's name
//                    _connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                    if (null != this.) {
//                        Toast.makeText(activity, "Connected to " + _connectedDeviceName, Toast.LENGTH_SHORT).show();
//                    }
//                    break;
            case Constants.MESSAGE_TOAST:
//                    if (null != activity) {
//                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
//                    }
                break;
        }
        }
    };


    private class CheckerThread extends Thread {

        public void run() {
            boolean _previous = false;
            while (true) {
                if (_rotateRequested != _previous) {
                    SensorManager manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
                    if (_rotateRequested) {
                        Sensor magnet = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                        Sensor accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                        manager.registerListener(_magneticListener, magnet, SensorManager.SENSOR_DELAY_NORMAL);
                        manager.registerListener(_magneticListener, accel, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                    else {
                        manager.unregisterListener(_magneticListener);
                        _azimuth = 0;
                        _magneticListener.resetValues();
                    }
                    _previous = _rotateRequested;
                }
            }
        }
    }

    private class ParserThread extends Thread {
        private boolean _quitRequested = false;

        public void run() {
            String data = "";
            int indexOfEnd = -1;
            while (_quitRequested != true) {
                data = "";
                synchronized (_receiveBuffer) {
                    if (_receiveBuffer.isEmpty() != true &&
                        _receiveBuffer.startsWith("[") != true &&
                        _receiveBuffer.indexOf("[") != -1) {
                        _receiveBuffer = _receiveBuffer.substring(_receiveBuffer.indexOf("["));
                    }
                    indexOfEnd = _receiveBuffer.indexOf("]");
                    if (_receiveBuffer.startsWith("[") && indexOfEnd != -1) {
                        data = _receiveBuffer.substring(0, indexOfEnd + 1);
                        if (_receiveBuffer.endsWith("]")) {
                            _receiveBuffer = "";
                        } else {
                            _receiveBuffer = _receiveBuffer.substring(indexOfEnd + 1);
                        }
                    }
                }
                if (data.isEmpty() != true) {
                    LivestockInfo info = Parser.parse(data);
                    synchronized (_livestockInfoMap) {
                        _dataBase.insert("lwd_history", data, info, _myGpsLocation);

                        _livestockInfoMap.put(info.source(), info);
                        if (!_livestockInfoMapColor.containsKey(info.source())) {
                            _livestockInfoMapColor.put(info.source(), colorSet[_livestockInfoMap.size() - 1]);
                        }
                        _mapView.invalidate();
                    }
                }
            }
        }

        public void quit() {
            _quitRequested = true;
        }
    }

    private class MapView extends View {
        protected int _scale = 40; // max scale. 100 pixel == 4km.
        protected int _centerX = 0, _centerY = 0;
        // touch event.
        static final int NONE = 0;
        static final int DRAG = 1;
        static final int ZOOM = 2;
        int mode = NONE;
        double _prevDistance = 0.0, _currentDistance = 0.0;
        int _dragPrevX = 0, _dragPrevY = 0;
        private Bitmap _bufferBitmap;
        private Canvas _bufferCanvas;
        private Matrix _rotateMatrix;
        private Paint _paint;
        private float[] _distance;
        private RectF _rect;
        private Path _triPath, _pinPath;

        //private Bitmap sun_image;
        public MapView(Context c) {
            super(c);
            //Resources r = c.getResources();
            //sun_image = BitmapFactory.decodeResource(r, R.drawable.sun);
            _bufferCanvas = new Canvas();
            _rotateMatrix = new Matrix();
            _paint = new Paint();
            _distance = new float[1];
            _rect = new RectF();
            _triPath = new Path();
            _pinPath = new Path();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (_bufferBitmap == null) {
                _bufferBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            }
            _bufferBitmap.eraseColor(Color.TRANSPARENT);
            _bufferCanvas.setBitmap(_bufferBitmap);
            int x = getWidth();
            int y = getHeight();
            int viewCenterX = _centerX + x / 2;
            int viewCenterY = _centerY + y / 2;

            // draw axis and circles.
            _paint.setStyle(Paint.Style.STROKE);
            _paint.setColor(Color.BLACK);
            _bufferCanvas.drawLine(0, viewCenterY, x, viewCenterY, _paint);
            _bufferCanvas.drawLine(viewCenterX, 0, viewCenterX, y, _paint);

            for (int i = 0; i < 5; ++i) {
                _bufferCanvas.drawCircle(viewCenterX, viewCenterY, (i + 1) * _scale * 2.5f, _paint);
            }
            // draw my location on center
            _paint.setStyle(Paint.Style.FILL);
            _paint.setColor(Color.RED);
            _paint.setTextSize(35);
            _bufferCanvas.drawCircle(viewCenterX, viewCenterY, 15, _paint);
            _bufferCanvas.drawText("N", viewCenterX - 17, viewCenterY - _scale * 12.5f, _paint);

            if (_rotateRequested) {
                // draw triangle
                int triLength = 20;

                _triPath.reset();
                _triPath.moveTo(viewCenterX - (triLength / 2), viewCenterY - 17);
                _triPath.lineTo(viewCenterX + (triLength / 2), viewCenterY - 17);
                _triPath.lineTo(viewCenterX, viewCenterY - 17 - 17);
                _triPath.close();

                _bufferCanvas.save();
                _bufferCanvas.rotate(_azimuth, viewCenterX, viewCenterY);
                _bufferCanvas.drawPath(_triPath, _paint);
                _bufferCanvas.restore();
            }

            // draw receivers` location
            for (String key : _livestockInfoMap.keySet()) {
                LivestockInfo info = _livestockInfoMap.get(key);
                Pair<Double, Double> point = getRelativePoint(_myGpsLocation, Pair.create(info.latitude(), info.longitude()));

                Location.distanceBetween(_myGpsLocation.first, _myGpsLocation.second, info.latitude(), info.longitude(), _distance);
                String name = "[" + info.source() + "]" + Float.toString((float) Math.round(_distance[0]) / 1000) + "km";

                float dx = (float) (x / 2) - (float) (point.first * _scale);
                float dy = (float) (y / 2) + (float) (point.second * _scale);

                long timeDiff = (System.currentTimeMillis() - info.timestamp().getTime()) / (1000 * 60);    // ms * 1000 * 60 = min

                _paint.setColor(_livestockInfoMapColor.get(key));
                _bufferCanvas.drawCircle(_centerX + dx, _centerY + dy, 15, _paint);
                _bufferCanvas.drawText(name, _centerX + dx + 30, _centerY + dy, _paint);
                _bufferCanvas.drawText(String.format("%d Minute(s) ago", timeDiff), _centerX + dx + 30, _centerY + dy + 35, _paint);
            }

            // draw pin
            _paint.setColor(Color.BLACK);
            for (String key : _pinInfoMap.keySet()) {
                PinInfo info = _pinInfoMap.get(key);
                Pair<Double, Double> point = getRelativePoint(_myGpsLocation, Pair.create(info.pinLat(), info.pinLon()));

                Location.distanceBetween(_myGpsLocation.first, _myGpsLocation.second, info.pinLat(), info.pinLon(), _distance);
                String name = "<" + info.pinName() + ">" + Float.toString((float) Math.round(_distance[0]) / 1000) + "km";

                float dx = (float) (x / 2) - (float) (point.first * _scale);
                float dy = (float) (y / 2) + (float) (point.second * _scale);
                int width = 15;

                _rect.set(_centerX + dx - width, _centerY + dy - width, _centerX + dx + width, _centerY + dy + width);
                switch(info.pinCategory()) {
                    case 0: // home
                        _bufferCanvas.drawRect(_rect, _paint);
                        break;
                    case 1: // repeater
                        _bufferCanvas.drawArc(_rect, 110, 320, true, _paint);
                        break;
                    case 2: // etc
                        // draw diamond
                        _pinPath.reset();
                        _pinPath.moveTo(_centerX + dx, _centerY + dy - width);
                        _pinPath.lineTo(_centerX + dx + width, _centerY + dy);
                        _pinPath.lineTo(_centerX + dx, _centerY + dy + width);
                        _pinPath.lineTo(_centerX + dx - width, _centerY + dy);
                        _pinPath.close();

                        _bufferCanvas.drawPath(_pinPath, _paint);
                        break;
                    default:
                        break;
                }
                _bufferCanvas.drawText(name, _centerX + dx + 30, _centerY + dy, _paint);
            }

            canvas.drawBitmap(_bufferBitmap, 0, 0, null);

        }

        public boolean onTouchEvent(MotionEvent event) {
            final int action = event.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {// single touch down.
                    _dragPrevX = (int) event.getX();
                    _dragPrevY = (int) event.getY();
                    mode = DRAG;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {// touch move.
                    if (mode == DRAG && _rotateRequested != true) {
                        _centerX -= _dragPrevX - (int) event.getX();
                        _centerY -= _dragPrevY - (int) event.getY();
                        _dragPrevX = (int) event.getX();
                        _dragPrevY = (int) event.getY();

                    } else if (mode == ZOOM) {
                        _currentDistance = spacing(event);
                        if (_currentDistance - _prevDistance > 10) {
                            // zoom in.
                            _scale += 5;
                            if (_scale > 400) _scale = 400;
                        } else if (_prevDistance - _currentDistance > 10) {
                            // zoom out.
                            _scale -= 5;
                            if (_scale < 40) _scale = 40;
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_POINTER_DOWN: {// multi touch down.
                    _prevDistance = spacing(event);
                    _currentDistance = spacing(event);
                    mode = ZOOM;
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP: {
                    _currentDistance = _prevDistance = 0;
                    mode = NONE;
                    break;
                }
            }

            invalidate();
            return true;
            //return super.onTouchEvent(event);
        }
        private double spacing(MotionEvent event) {
            double x = event.getX(0) - event.getX(1);
            double y = event.getY(0) - event.getY(1);
            return Math.sqrt(x * x + y * y);
        }
        private Pair<Double, Double> getRelativePoint(Pair<Double, Double> center, Pair<Double, Double> point) {
            Pair<Double, Double> c = getXYCoordinate(center.first, center.second);
            Pair<Double, Double> p = getXYCoordinate(point.first, point.second);
            return Pair.create(c.first - p.first, c.second - p.second);
        }
        private Pair<Double, Double> getXYCoordinate(double lat, double lon) {
            double lat_dist = 0.0, lon_dist = 0.0;
            switch (Math.abs((int)lat / 5)) {
                case 0:
                    lat_dist = 110.569;
                    lon_dist = 111.322;
                    break;
                case 1:
                    lat_dist = 110.578;
                    lon_dist = 110.902;
                    break;
                case 2:
                    lat_dist = 110.603;
                    lon_dist = 109.643;
                    break;
                case 3:
                    lat_dist = 110.644;
                    lon_dist = 117.553;
                    break;
                case 4:
                    lat_dist = 110.701;
                    lon_dist = 114.650;
                    break;
                case 5:
                    lat_dist = 110.770;
                    lon_dist = 100.953;
                    break;
                case 6:
                    lat_dist = 110.850;
                    lon_dist = 96.490;
                    break;
                case 7:
                    lat_dist = 110.941;
                    lon_dist = 91.290;
                    break;
                case 8:
                    lat_dist = 111.034;
                    lon_dist = 85.397;
                    break;
                case 9:
                    lat_dist = 111.132;
                    lon_dist = 78.850;
                    break;
                case 10:
                    lat_dist = 111.230;
                    lon_dist = 71.700;
                    break;
                case 11:
                    lat_dist = 111.327;
                    lon_dist = 63.997;
                    break;
                case 12:
                    lat_dist = 111.415;
                    lon_dist = 55.803;
                    break;
                case 13:
                    lat_dist = 111.497;
                    lon_dist = 47.178;
                    break;
                case 14:
                    lat_dist = 111.567;
                    lon_dist = 38.188;
                    break;
                case 15:
                    lat_dist = 111.625;
                    lon_dist = 28.904;
                    break;
                case 16:
                    lat_dist = 111.666;
                    lon_dist = 19.394;
                    break;
                case 17:
                    lat_dist = 111.692;
                    lon_dist = 9.735;
                    break;
                case 18:
                    lat_dist = 111.700;
                    lon_dist = 0.000;
                    break;
            }
            return Pair.create(lon_dist * lon, lat_dist * lat);
        }

        public void setCenter() {
            _centerX = 0;
            _centerY = 0;
            invalidate();
        }
    }

    private void setOpenTasks() {
        this.readLast();
        this.updatePinList();
    }

    private void updatePinList() {
        _pinKey = _dataBase.updatePinList();
    }

    private void readLast() {
        JSONObject lastData;
        JSONArray key, pinKey;
        JSONObject data, pinData;

        try {
            lastData = _dataBase.readLast();
            key = lastData.getJSONArray("key");
            data = lastData.getJSONObject("data");

            for (int i = 0; i < key.length(); i++) {
                String lwd_id;
                JSONObject dataDetail;

                lwd_id = key.getString(i);
                dataDetail = data.getJSONObject(lwd_id);
                _livestockInfoMap.put(lwd_id, Parser.parse(lwd_id, dataDetail));
                _livestockInfoMapColor.put(lwd_id, colorSet[_livestockInfoMap.size() - 1]);
            }

            pinKey = lastData.getJSONArray("pinKey");
            pinData = lastData.getJSONObject("pinData");

            for (int i = 0; i < pinKey.length(); i++) {
                String pinName;
                JSONObject dataDetail;

                pinName = pinKey.getString(i);
                dataDetail = pinData.getJSONObject(pinName);

                _pinKey.add(pinName);

                _pinInfoMap.put(pinName, new PinInfo(dataDetail.getInt("category"), pinName,
                                                     dataDetail.getDouble("lat"), dataDetail.getDouble("lon")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void pinSelectCategories() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final CharSequence[] first = { "Home", "Repeater", "ETC" };

        builder.setTitle("Categories")
                .setItems(first, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pinSetName(which);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void pinSetName(final int category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText getName = new EditText(this);

        builder.setView(getName)
                .setTitle("Place Name")
                .setMessage("Enter the place name")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            addPin(category, getName.getText().toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addPin(int category, String name) throws JSONException {
        _dataBase.insertPin(category, name, _myGpsLocation.first, _myGpsLocation.second);
        _pinKey.add(name);
        _pinInfoMap.put(name, new PinInfo(category, name, _myGpsLocation.first, _myGpsLocation.second));
        _mapView.invalidate();
    }

    private void pinSelectRemove() {
        final CharSequence[] pinList;
        pinList = _pinKey.toArray(new CharSequence[_pinKey.size()]);
        final ArrayList checkedPin = new ArrayList();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        Log.d("MainActivity", "pinSelectRemove-before builder");
        builder.setTitle("Device filter")
                .setMultiChoiceItems(pinList, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int index, boolean isChecked) {
                        if (isChecked) {
                            checkedPin.add(pinList[index]);
                        } else {
                            checkedPin.remove(pinList[index]);
                        }
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        for(int i = 0; i < checkedPin.size(); i++) {
                            _pinKey.remove(checkedPin.get(i));
                            _pinInfoMap.remove(checkedPin.get(i));
                        }

                        deletePin(checkedPin);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deletePin(ArrayList pinList) {
        _dataBase.deletePin(pinList);
        _mapView.invalidate();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _dataBase = Database.getInstance(this);
//        _dataBase.insertSample("1");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab_reset = (FloatingActionButton) findViewById(R.id.fab_reset);
        fab_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _mapView.setCenter();
            }
        });

        FloatingActionButton fab_rotate = (FloatingActionButton) findViewById(R.id.fab_rotate);
        fab_rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            _rotateRequested = !_rotateRequested;
            String msg;
            if (_rotateRequested) {
                _mapView.setCenter();
                msg = "Tracking North Requested";
            }
            else {
                _mapView.setCenter();
                msg = "Tracking North Disabled";
            }
            Snackbar.make(view, msg, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        LinearLayout layout = findViewById(R.id.layout_view);
        _mapView = new MapView(this);
        layout.addView(_mapView);

        BluetoothService.getInstance().addHandler(_handler);
        _parserThread = new ParserThread();
        _parserThread.start();
        checkDangerousPermissions();
        startLocationService();
        _magneticListener = new MagneticSensorListener();
        _checkerThread = new CheckerThread();
        _checkerThread.start();
        setOpenTasks();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.add_pin) {
            pinSelectCategories();
        }

        if (id == R.id.delete_pin) {
            pinSelectRemove();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_map) {
            // Handle the camera action
        } else if (id == R.id.nav_bluetooth) {
            Intent i = new Intent(MainActivity.this, BluetoothActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_database) {
            Intent i = new Intent(MainActivity.this, DatabaseActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (_state) {
            case BluetoothService.STATE_CONNECTED: {
                menu.findItem(R.id.action_bluetooth).setIcon(R.drawable.ic_bluetooth_connected_white_24dp);
                break;
            }
            case BluetoothService.STATE_CONNECTING: {
                //menu.findItem(R.id.action_bluetooth).setIcon(R.drawable.ic_bluetooth_connected_white_24dp);
                break;
            }
            case BluetoothService.STATE_LISTEN: {
                break;
            }
            case BluetoothService.STATE_NONE: {
                menu.findItem(R.id.action_bluetooth).setIcon(R.drawable.ic_bluetooth_disabled_white_24dp);
                break;
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (_rotateRequested) {
            SensorManager manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            Sensor magnet = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Sensor accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            manager.registerListener(_magneticListener, magnet, SensorManager.SENSOR_DELAY_NORMAL);
            manager.registerListener(_magneticListener, accel, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SensorManager manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        manager.unregisterListener(_magneticListener);
        _azimuth = 0;
        _magneticListener.resetValues();
    }

    private class PinInfo {
        final int _pinCategory;
        final String _pinName;
        final double _pinLat, _pinLon;

        PinInfo(int category, String name, double lat, double lon) {
            _pinCategory = category;
            _pinName = name;
            _pinLat = lat;
            _pinLon = lon;
        }

        public int pinCategory() { return _pinCategory; }
        public String pinName() { return _pinName; }
        public double pinLat() { return _pinLat; }
        public double pinLon() { return _pinLon; }
    }
}
