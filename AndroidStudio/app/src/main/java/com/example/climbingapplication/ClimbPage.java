package com.example.climbingapplication;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class ClimbPage extends AppCompatActivity {

    private Button localDBButton;
    private TextView hrText;
    private TextView timeClimbed;
    private static final String TAG = "ClimbPage";
    public Connection con;          //For database connection
    public Button azureDBButton;
    public TextView testName;
    private Button startButton;
    public Boolean climbingFlag;
    private Chronometer chronometer;
    private long timeClimbedFor;
    public static SQLiteDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_climb_page);

        //Create and Fill Local Database
        localDb = openOrCreateDatabase("incomingData",MODE_PRIVATE,null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS Data(HR VARCHAR,AccelerationX VARCHAR,AccelerationY VARCHAR,AccelerationZ VARCHAR,GPSLong VARCHAR,GPSLat VARCHAR,Time VARCHAR);");
        localDb.execSQL("INSERT INTO Data VALUES('92','9.81', '9.81', '9.81', '100', '200', '0');");

        localDBButton = findViewById(R.id.dbButton);
        hrText = findViewById(R.id.hrValue);
        localDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String result = testRetrieve(localDb);
                hrText.setText(result);
            }
        });

        azureDBButton = findViewById(R.id.azureDBButton);
        azureDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClimbPage.testAzureDB tdb = new ClimbPage.testAzureDB();
                tdb.execute("");
            }
        });

        climbingFlag = false;
        chronometer = findViewById(R.id.chronometer);
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (climbingFlag == false){
                    startButton.setBackgroundColor(0xFFFF0000);
                    startButton.setText("Stop Climbing");
                    startstopChronometer(v);
                }
                else{
                    startButton.setBackgroundColor(0xFF6200EE);
                    startButton.setText("Start Climbing");
                    startstopChronometer(v);
                }
            }
        });

    }
    public void startstopChronometer(View v){
        if (!climbingFlag){
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            climbingFlag = true;
        }
        else{
            chronometer.stop();
            timeClimbedFor = SystemClock.elapsedRealtime() - chronometer.getBase();
            climbingFlag = false;
        }
    }

    public String testRetrieve(SQLiteDatabase localDb) {        //Method used to access local database
        Cursor resultSet = localDb.rawQuery("Select * from Data",null);
        resultSet.moveToFirst();
        String HR = resultSet.getString(0);
        return HR;
    }

    public class testAzureDB extends AsyncTask<String, String, String> {     //Class to access azure DB
        String z = "";
        Boolean isSuccess = false;
        String name1 = "";

        @Override
        protected void onPostExecute(String r){
            if(isSuccess){
                testName = (TextView) findViewById(R.id.dbTestText);
                testName.setText(name1);
            }
        }
        @Override
        protected String doInBackground(String... params) {
            try{
                con = connectionclass();
                if (con == null) {
                    z = "Check your Internet Access!";
                }
                else{
                    String query = "select * from Persons";
                    Statement stmt = con.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    if(rs.next()) {
                        name1 = rs.getString("FirstName");
                        z = "query successful";
                        isSuccess = true;
                        con.close();
                    }
                    else{
                        z = "Invalid Query!";
                        isSuccess = false;
                    }
                }
            } catch(Exception e) {
                isSuccess = false;
                z = e.getMessage();
                Log.d("sql error", z);
            }
            return z;
        }
    }

    @SuppressLint("NewAPI")
    public Connection connectionclass() {   //Method for connecting to Azure API
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Connection connection = null;
        String ConnectionURL = null;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            ConnectionURL = "jdbc:jtds:sqlserver://climbingdatabaseserver.database.windows.net:1433;DatabaseName=climbingdata;user=Alex@climbingdatabaseserver;password=Emoipo10;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";
            connection = DriverManager.getConnection(ConnectionURL);
        } catch (SQLException se) {
            Log.e("error here 1: ", se.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("error here 2: ", e.getMessage());
        } catch (Exception e) {
            Log.e("error here 3: ", e.getMessage());
        }
        return connection;
    }
}