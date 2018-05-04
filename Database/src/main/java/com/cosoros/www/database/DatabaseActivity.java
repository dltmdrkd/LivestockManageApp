package com.cosoros.www.database;

import android.os.Bundle;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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
//
//        dbTest.insert(table.getNameLwdHistory(), data, info);
        this.readDB(table.getNameLwdHistory());
//        Log.d("DatabaseActivity", "------------------------------------------------------------------------------");
    }

//    @TargetApi(23)
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
            TextViewCompat.setTextAppearance(textView, R.style.HeaderText);
            textView.setGravity(Gravity.CENTER);
//            textView.setTextAppearance(R.style.HeaderText);


            textView.setText((String) columnName.get(i));

            columnRow.addView(textView);
        }
        tableLayout.addView(columnRow);

        // add data columns
        if (row >  101) {
            row = 101;
        }
        for (int i = 0; i < row - 1; i++) {
            TableRow tableRow = new TableRow(this);

            ArrayList rowData = (ArrayList) readData.get(i);
            int col = rowData.size();
            for (int j = 0; j < col; j++) {
                TextView textView = new TextView(this);
                textView.setBackgroundResource(R.drawable.border);
                TextViewCompat.setTextAppearance(textView, R.style.BodyText);
                textView.setGravity(Gravity.CENTER);
//                textView.setTextAppearance(R.style.BodyText);

                Map colData = (Map) rowData.get(j);
                textView.setText((String) colData.get("data"));

                tableRow.addView(textView);
            }

            tableLayout.addView(tableRow);
        }

        Log.d("DatabaseActivity", "Read End");
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.file_export_menu, menu);
        return super.onCreateOptionsMenu(menu);
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

/*
    // export to sd card
    public void sqliteExport(){
        Log.d("SQLITE EXPORT", "start");
        Date todayDate = new Date();
        SimpleDateFormat todayFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String today = todayFormat.format(todayDate);
        String fileName = today + "Log.sqlite";
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "/data/com.cosoros.livstockmanageapp/databases/nomad_lwd.db";
                String backupDBPath = fileName;
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
                if(backupDB.exists()){
                    Toast.makeText(this, "DB Export Complete!!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("SQLITE EXPORT", "end");
    }
*/

}