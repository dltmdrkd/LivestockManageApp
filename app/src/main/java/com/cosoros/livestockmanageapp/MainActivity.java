package com.cosoros.livestockmanageapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

enum DrawingType {
    DRAW_DEFAULT, DRAW_DIRECTION;
}
enum RunType {
    APP_START, APP_SLEEP
}

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private int _state = BluetoothService.STATE_NONE;
    private String _receiveBuffer = "";
    private DrawingType _drawingMode = DrawingType.DRAW_DEFAULT;
    private RunType _runMode = RunType.APP_START;
    private Pair<Double, Double> _myGpsLocation = new Pair<>(37.30362, 126.99712);
    private Pair<Double, Double> _myLastGpsLocation = new Pair<>(37.30362, 126.99712);
    private HashMap<String, LivestockInfo> _livestockInfoMap = new HashMap<>();
    private ParserThread _parserThread;
    private Database _dataBase;
    private MapView _mapView;


    private void startLocationService() {
        // 위치 관리자 객체 참조
        LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        // 위치 정보를 받을 리스너 생성
        GPSListener gpsListener = new GPSListener();
        long minTime = 10000;
        float minDistance = 0;

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
            Toast.makeText(this, "권한 있음", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "권한 없음", Toast.LENGTH_LONG).show();

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
                _myLastGpsLocation = _myGpsLocation;
                _myGpsLocation = Pair.create(location.getLatitude(), location.getLongitude());

                _drawingMode = DrawingType.DRAW_DIRECTION;
                _mapView.invalidate();
            }
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
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
                        _livestockInfoMap.put(info.source(), info);
                        _mapView.invalidate();

                        _drawingMode = DrawingType.DRAW_DEFAULT;
                        _dataBase.insert("lwd_history", data, info, _myGpsLocation);
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
        protected int _centerX  = 0, _centerY = 0;
        // touch event.
        static final int NONE = 0;
        static final int DRAG = 1;
        static final int ZOOM = 2;
        int mode = NONE;
        double _prevDistance = 0.0, _currentDistance = 0.0;
        int _dragPrevX = 0, _dragPrevY = 0;

        //private Bitmap sun_image;
        public MapView(Context c) {
            super(c);
            //Resources r = c.getResources();
            //sun_image = BitmapFactory.decodeResource(r, R.drawable.sun);
        }
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int x = getWidth();
            int y = getHeight();
            int viewCenterX = _centerX + x / 2;
            int viewCenterY = _centerY + y / 2;
            Paint paint = new Paint();

            // draw axis and circles.
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            canvas.drawLine(0, viewCenterY, x, viewCenterY, paint);
            canvas.drawLine(viewCenterX, 0, viewCenterX, y, paint);
            for (int i = 0; i < 5; ++i) {
                canvas.drawCircle(viewCenterX, viewCenterY, (i + 1) * _scale * 2.5f, paint);
            }
            Rect dst = new Rect((int)(viewCenterX + 5 * 2.5f * _scale - 50),
                    (int)(viewCenterY - 50),
                    (int)(viewCenterX + 5 * 2.5f * _scale + 50),
                    (int)(viewCenterY + 50));
            //canvas.drawBitmap(sun_image, null, dst, null);

            // draw my location on center
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.RED);
            paint.setTextSize(35);
            canvas.drawCircle(viewCenterX, viewCenterY, 15, paint);

            if (_drawingMode == DrawingType.DRAW_DIRECTION) {
//                 draw my direction
                float direction[] = new float[2];
                Location.distanceBetween(_myLastGpsLocation.first, _myLastGpsLocation.second, _myGpsLocation.first, _myGpsLocation.second, direction);

                // draw triangle
                Path path = new Path();
                int triLength = 20;

                path.moveTo(viewCenterX - (triLength / 2), viewCenterY + 17);
                path.lineTo(viewCenterX + (triLength / 2), viewCenterY + 17);
                path.lineTo(viewCenterX, viewCenterY + 17 + 17);
                path.close();

                canvas.save();
                canvas.rotate(-direction[1], viewCenterX, viewCenterY);
                canvas.drawPath(path, paint);
                canvas.restore();
                _drawingMode = DrawingType.DRAW_DEFAULT;
            }


            if (_runMode == RunType.APP_START) {
                JSONObject lastData;
                JSONArray key;
                JSONObject data;

                try {
                    lastData = _dataBase.readLast();
                    key = lastData.getJSONArray("key");
                    data = lastData.getJSONObject("data");

                    for (int i = 0; i < key.length(); i++) {
                        Double lat, lon, alt;
                        String lwd_id, utcTime;
                        JSONObject dataDetail;

                        dataDetail = data.getJSONObject(key.getString(i));
                        lwd_id = key.getString(i);
                        _livestockInfoMap.put(lwd_id, Parser.parse(lwd_id, dataDetail));

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                _runMode = RunType.APP_SLEEP;
            }

            int cnt = 0;
            for (String key : _livestockInfoMap.keySet()){
                LivestockInfo info = _livestockInfoMap.get(key);

                Pair<Double, Double> point = getRelativePoint(_myGpsLocation, Pair.create(info.latitude(), info.longitude()));
                float distance[] = new float[2];
                Location.distanceBetween(_myGpsLocation.first, _myGpsLocation.second, info.latitude(), info.longitude(), distance);
                String name = "[" + info.source() + "]" + Float.toString((float)Math.round(distance[0]) / 1000) + "km";

                float dx = (float)(x / 2) - (float)(point.first * _scale);
                float dy = (float)(y / 2) + (float)(point.second * _scale);

//                String angle = "[Angle]" + Integer.toString(Math.round(distance[1]));
//                canvas.drawText(angle, _centerX + dx + 40, _centerY + dy + 40 , paint);

                cnt = (cnt + 1) % 4;
                switch (cnt) {
                    case 0:
                        paint.setColor(Color.BLUE);
                        canvas.drawText("Last time: [" + info.source() + "]" + info.timestamp(), _centerX + 40, _centerY + 40, paint);
                        break;
                    case 1:
                        paint.setColor(Color.MAGENTA);
                        canvas.drawText("Last time: [" + info.source() + "]" + info.timestamp(), _centerX + 40, _centerY + 80, paint);
                        break;
                    case 2:
                        paint.setColor(Color.BLACK);
                        canvas.drawText("Last time: [" + info.source() + "]" + info.timestamp(), _centerX + 40, _centerY + 120, paint);
                        break;
                    case 3:
                        paint.setColor(Color.GRAY);
                        canvas.drawText("Last time: [" + info.source() + "]" + info.timestamp(), _centerX + 40, _centerY + 160, paint);
                        break;
                    default:
                        break;
                }

                canvas.drawText(name, _centerX + dx + 40, _centerY + dy, paint);
                canvas.drawCircle(_centerX + dx, _centerY + dy, 15, paint);
            }

//            PointDouble p1 = getRelativePoint(animalList.get(0), animalList.get(1));
//            float dx = (float)(x / 2) - (float)(p1.x * scale);
//            float dy = (float)(y / 2) + (float)(p1.y * scale);
//            canvas.drawCircle(center_x + dx, center_y + dy, 15, paint);
//            int dist = (int)(animalList.get(1)._dis * 1000);
//            String name = "[" + animalList.get(1)._name + "] : " + Integer.toString(dist) + " m";
//            canvas.drawText(name, center_x + dx + 20, center_y + dy, paint);
//
//            paint.setColor(Color.BLUE);
//            PointDouble p2 = get_relative_point(animalList.get(0), animalList.get(2));
//            float dx2 = (float)(x / 2) - (float)(p2.x * scale);
//            float dy2 = (float)(y / 2) + (float)(p2.y * scale);
//            canvas.drawCircle(center_x + dx2, center_y + dy2, 15, paint);
//            dist = (int)(animalList.get(2)._dis * 1000);
//            name = "[" + animalList.get(2)._name + "] : " + Integer.toString(dist) + " m";
//            canvas.drawText(name, center_x + dx2 + 20, center_y + dy2, paint);
//
//            paint.setColor(Color.GREEN);
//            PointDouble p3 = get_relative_point(animalList.get(0), animalList.get(3));
//            float dx3 = (float)(x / 2) - (float)(p3.x * scale);
//            float dy3 = (float)(y / 2) + (float)(p3.y * scale);
//            canvas.drawCircle(center_x + dx3, center_y + dy3, 15, paint);
//            dist = (int)(animalList.get(3)._dis * 1000);
//            name = "[" + animalList.get(3)._name + "] : " + Integer.toString(dist) + " m";
//            canvas.drawText(name, center_x + dx3 + 20, center_y + dy3, paint);
            //canvas.drawPoint(x / 2, y / 2, paint);
            float left, top, right, bottom;
            String scaleText;
            if (_scale * 2.5f < 1200) {
                left = x - _scale * 2.5f / 2 - 50;
                scaleText = "1000 m";
            }
            else {
                left = x - _scale * 2.5f / 20 - 50;
                scaleText = "100 m";
            }
            top = y - 100;
            right = x - 50;
            bottom = y - 50;
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            canvas.drawText(scaleText, x - 250, y - 120, paint);
            paint.setStrokeWidth(5);
            canvas.drawRect(left, top, right, bottom, paint);
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
                    if (mode == DRAG) {
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
                        } else if (_prevDistance - _currentDistance > 10){
                            // zoom out.
                            _scale -= 5;
                            if (_scale < 40) _scale = 40;
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_POINTER_DOWN: {// multi tocuh down.
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

            _drawingMode = DrawingType.DRAW_DEFAULT;
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _dataBase = Database.getInstance(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
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
}
