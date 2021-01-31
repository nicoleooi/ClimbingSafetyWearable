package com.example.climbingapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.TextView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RecordingClimb extends MainActivity {

    public Connection con;          //For database connection
    private Button startBtn;
    public Boolean climbingFlag;
    private Chronometer chronometer;
    private long timeClimbedFor;
    public CSVFile csvFile;
    public double longitude;
    public double latitude;
    private Button homeButton;
    private Button profileButton;
    private Button mapsButton;
    private RequestQueue requestQueue;
    private TextView testWeather;
    private ImageView weatherImage;
    private String weather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_climb);

        //Create and Fill Local Database
//        localDb = openOrCreateDatabase("incomingData",MODE_PRIVATE,null);
//        localDb.execSQL("CREATE TABLE IF NOT EXISTS Data(HR VARCHAR,AccelerationX VARCHAR,AccelerationY VARCHAR,AccelerationZ VARCHAR,GPSLong VARCHAR,GPSLat VARCHAR,Time VARCHAR);");
//        localDb.execSQL("INSERT INTO Data VALUES('92','9.81', '9.81', '9.81', '100', '200', '0');");
//
        //Database stuff for later
        String result = testRetrieve(localDb);
        RecordingClimb.testAzureDB tdb = new RecordingClimb.testAzureDB();
        tdb.execute("");

        weatherImage = findViewById(R.id.weatherImage);
        weather = "thunder";
        switch(weather){
            case("sunny"):
                weatherImage.setImageResource(R.drawable.sunny);
                break;
            case("rain"):
                weatherImage.setImageResource(R.drawable.rain);
                break;
            case("thunder"):
                weatherImage.setImageResource(R.drawable.thunder);
                break;
        }

        mapsButton = findViewById(R.id.mapsButton);
        mapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(RecordingClimb.this, MapsActivity.class);
                RecordingClimb.this.startActivity(myIntent);
            }
        });


        climbingFlag = false;
        chronometer = findViewById(R.id.chronometer);
        startBtn = findViewById(R.id.startBtn);
        startBtn.setBackgroundColor(0xFF008000);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!climbingFlag){
                    startBtn.setBackgroundColor(0xFFFF0000);
                    startBtn.setText("Stop Climbing");
                    startstopChronometer(v);
                }
                else{
                    startBtn.setBackgroundColor(0xFF008000);
                    startBtn.setText("Start Climbing");
                    startstopChronometer(v);
                }
            }
        });

        homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHomePage();
            }
        });

        profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfilePage();
            }
        });

        String apiKey = "jFNGDUXBapyjnShPufxJHL6YsCvedU9v";

        testWeather = findViewById(R.id.testWeather);
        requestQueue = Volley.newRequestQueue(this);
        String url = "https://api.climacell.co/v4/locations?apikey=jFNGDUXBapyjnShPufxJHL6YsCvedU9v";
        requestWithSomeHttpHeaders();
    }

    public void requestWithSomeHttpHeaders() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://data.climacell.co/v4/locations?apikey=jFNGDUXBapyjnShPufxJHL6YsCvedU9v";
        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        testWeather.setText(response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("ERROR","error => "+error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<String, String>();
                headers.put("apikey", "jFNGDUXBapyjnShPufxJHL6YsCvedU9v");
                headers.put("content-type","application/json");
                return headers;
            }
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("type","Point");
                params.put("Coordinates","[42.355440, -71.059910]");
                try {
                    String jsonString = new JSONObject()
                            .put("Coordinates", new JSONObject().put("coordinates", "[42.355440, -71.059910]"))
                            .toString();
                    params.put("Coordinates",jsonString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return params;
            }
        };
        queue.add(getRequest);
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