package com.cosoros.www.database;

import android.annotation.TargetApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.cosoros.www.datastructure.LivestockInfo;
import com.cosoros.www.network.parser.Parser;

import java.util.ArrayList;
import java.util.Map;

public class DatabaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        Toolbar toolbar = findViewById(R.id.database_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        Log.d("DatabaseActivity", "------------------------------------------------------------------------------START");
//        Log.d("DatabaseActivity", "OnCreate");

//        String data = "[00480002007FA0000036.08055^000129.39787^027.00^03^20180316^151233^03.50^]";
//        LivestockInfo info = Parser.parse(data);
//        Database dbTest = Database.getInstance(this);
        DBTable table = new DBTable();

//        dbTest.insert(table.getNameLwdHistory(), data, info);
        this.readDB(table.getNameLwdHistory());
//        Log.d("DatabaseActivity", "------------------------------------------------------------------------------");
    }


    @TargetApi(23)
    private void readDB(String name) {
        Database dataBase = Database.getInstance(this);
        ArrayList readData = dataBase.read(name);
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);

        // draw table
        int row = readData.size();

        // add title row
        ArrayList columnName = (ArrayList) readData.get(row - 1);
        TableRow columnRow = new TableRow(this);
        for (int i = 0; i < columnName.size(); i++) {
            TextView textView = new TextView(this);
            textView.setBackgroundResource(R.drawable.border);
            textView.setTextAppearance(R.style.HeaderText);

            textView.setText((String) columnName.get(i));

            columnRow.addView(textView);
        }
        tableLayout.addView(columnRow);

        // add data columns
        for (int i = 0; i < row - 1; i++) {
            TableRow tableRow = new TableRow(this);

            ArrayList rowData = (ArrayList) readData.get(i);
            int col = rowData.size();
            for (int j = 0; j < col; j++) {
                TextView textView = new TextView(this);
                textView.setBackgroundResource(R.drawable.border);
                textView.setTextAppearance(R.style.BodyText);

                Map colData = (Map) rowData.get(j);
                textView.setText((String) colData.get("data"));

                tableRow.addView(textView);
            }

            tableLayout.addView(tableRow);
        }

        Log.d("DatabaseActivity", "Read End");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}