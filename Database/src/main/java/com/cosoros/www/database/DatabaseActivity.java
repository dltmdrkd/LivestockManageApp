package com.cosoros.www.database;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.TreeSet;

public class DatabaseActivity extends AppCompatActivity {
    private JSONArray _device = new JSONArray();
    private JSONObject _filter = new JSONObject();
    private final String[] _repeaterOptions = {"REPEATER", "NO_REPEATER", "ANYWHERE"};
    private String _repeater = _repeaterOptions[2];
    private boolean _order = true;      // true : order by desc   /   false : order by asc
    private TreeSet<String> _key = new TreeSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        Toolbar toolbar = findViewById(R.id.database_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        DBTable table = new DBTable();
        try {
            this.readDB();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setFilterRepeater() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Repeater filter")
                .setItems(R.array.filter_repeater_options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                _repeater = _repeaterOptions[index];
                try {
                    readDB();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setFilterDevice() {
        final CharSequence[] deviceList;
        deviceList = _key.toArray(new CharSequence[_key.size()]);
        final ArrayList checkedDevice = new ArrayList();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Device filter")
                .setMultiChoiceItems(deviceList, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index, boolean isChecked) {
                if (isChecked) {
                    checkedDevice.add(deviceList[index]);
                } else {
                    checkedDevice.remove(deviceList[index]);
                }
            }})
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        _device = new JSONArray();
                        for(int i = 0; i < checkedDevice.size(); i++) {
                            _device.put(checkedDevice.get(i));
                        }

                        try {
                            readDB();
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

    private void buildFilter() throws JSONException {
        if (!_device.isNull(0)) {
            _filter.put("device", _device);
        }
        _filter.put("repeater", _repeater);
    }

    private void cleanTable(TableLayout table) {
        int childCount = table.getChildCount();

        // Remove all rows
        if (childCount > 1) {
            table.removeViews(0, childCount);
        }
    }

    private void readDB() throws JSONException {
        Database dataBase = Database.getInstance(this);

        buildFilter();
        ArrayList readData = dataBase.read(_filter, _order);

        TableLayout tableLayout = findViewById(R.id.tableLayout);
        cleanTable(tableLayout);

        // draw table
        int row = readData.size();

        // add title row
        ArrayList columnName = (ArrayList) readData.get(row - 1);
        TableRow columnRow = new TableRow(this);
        for (int i = 0; i < columnName.size(); i++) {
            TextView textView = new TextView(this);
            textView.setBackgroundResource(R.drawable.border);
            TextViewCompat.setTextAppearance(textView, R.style.HeaderText);
            textView.setGravity(Gravity.CENTER);

            textView.setText((String) columnName.get(i));

            columnRow.addView(textView);
        }
        tableLayout.addView(columnRow);

        if (row >  101) {
            row = 101;
        }

        // add data columns
        for (int i = 0; i < row - 1; i++) {
            TableRow tableRow = new TableRow(this);

            ArrayList rowData = (ArrayList) readData.get(i);
            _key.add(rowData.get(1).toString());
            int col = rowData.size();
            for (int j = 0; j < col; j++) {
                TextView textView = new TextView(this);
                textView.setBackgroundResource(R.drawable.border);
                TextViewCompat.setTextAppearance(textView, R.style.BodyText);
                textView.setGravity(Gravity.CENTER);
//                textView.setTextAppearance(R.style.BodyText);

//                Map colData = (Map) rowData.get(j);
                textView.setText(rowData.get(j).toString());

                tableRow.addView(textView);
            }

            tableLayout.addView(tableRow);
        }

        Log.d("DatabaseActivity", "Read End");
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.db_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;

        } else if (id == R.id.db_refresh) {
            try {
                readDB();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (id == R.id.sort_by_time) {
            _order = !_order;
            try {
                readDB();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (id == R.id.filter_repeater) {
            setFilterRepeater();

        } else if (id == R.id.filter_device) {
            setFilterDevice();
        }

        return super.onOptionsItemSelected(item);
    }

}