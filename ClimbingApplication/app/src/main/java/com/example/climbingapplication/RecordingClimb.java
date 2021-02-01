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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class RecordingClimb extends MainActivity {

    public Connection con;          //For database connection
    private Button startBtn;
    public Boolean climbingFlag;
    private Chronometer chronometer;
    public double longitude;
    public double latitude;
    private Button homeButton;
    private Button profileButton;
    private Button mapsButton;
    private RequestQueue requestQueue;
    private TextView tempText, humidityText, speedText, visibilityText, weatherDescription;
    private ImageView weatherImage;
    private String apiKey;
    private float[] weatherResponse = new float[8];
    private float timeClimbedFor;
    private String fields[] = {"temperatureApparent", "humidity", "windSpeed", "precipitationIntensity", "precipitationProbability", "precipitationType", "visibility", "weatherCode"};

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

        latitude = 44.23;
        longitude = -76.50;

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

        apiKey = "VCN07ZizEgitoq6L4I32o199HOojMIYj";
        tempText = findViewById(R.id.temp);
        humidityText = findViewById(R.id.humidityText);
        speedText = findViewById(R.id.speedText);
        visibilityText = findViewById(R.id.visibilityText);
        weatherImage = findViewById(R.id.weatherImage);
        weatherDescription = findViewById(R.id.weatherDescription);
        requestQueue = Volley.newRequestQueue(this);
        String url = "https://api.climacell.co/v4/locations?apikey=jFNGDUXBapyjnShPufxJHL6YsCvedU9v";
        requestWithSomeHttpHeaders();
    }

    public void requestWithSomeHttpHeaders() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://data.climacell.co/v4/timelines?location="+latitude+"%2C"+longitude;
        for (int i=0; i<fields.length; i++){
            url += "&fields="+fields[i];
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        String endAsISO = df.format(addHoursToJavaUtilDate(new Date(), 15));
        String startAsISO = df.format(new Date());
        url += "&timesteps=5m";
        url += "&endTime="+endAsISO;
        url += "&startTime="+startAsISO;
        url += "&apikey="+apiKey;
        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        parseResponse(response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ERROR","error => "+error.toString());
                    }
                }
        );
        queue.add(getRequest);
    }

    public void parseResponse(String response){
        System.out.println(response);
        for (int i=0; i<fields.length; i++){
            int startIndex, endIndex;
            if(i == fields.length - 1){
                startIndex = contains((CharSequence)fields[i], response);
                startIndex += fields[i].length() + 2;
                endIndex = contains((CharSequence)"}", response);
                char[] temp = new char[endIndex - startIndex];
                response.getChars(startIndex, endIndex, temp, 0);
                String tmp = new String(temp);
                weatherResponse[i] = Float.parseFloat(tmp);
            }
            else{
                startIndex = contains((CharSequence)fields[i], response);
                startIndex += fields[i].length() + 2;
                endIndex = contains((CharSequence)fields[i+1], response);
                char[] temp = new char[endIndex - 2 - startIndex];
                response.getChars(startIndex, endIndex - 2, temp, 0);
                String tmp = new String(temp);
                weatherResponse[i] = Float.parseFloat(tmp);
            }
        }
        handleWeatherResponse(weatherResponse);
    }

    public int contains(CharSequence sequence, String outerString)
    {
        return outerString.indexOf(sequence.toString());
    }

    public void handleWeatherResponse(float[] weatherResponse){
        tempText.setText(Float.toString(weatherResponse[0])+"\u00B0"+"C");
        humidityText.setText(Float.toString(weatherResponse[1])+"%");
        speedText.setText(Float.toString(weatherResponse[2])+" km/h");
        visibilityText.setText(Float.toString(weatherResponse[3])+" km");

        switch ((int)weatherResponse[7]){
            case 4201:
                weatherImage.setImageResource(R.drawable.rain_heavy);
                weatherDescription.setText("Heavy Rain");
                break;
            case 4001:
                weatherImage.setImageResource(R.drawable.rain);
                weatherDescription.setText("Rain");
                break;
            case 4200:
                weatherImage.setImageResource(R.drawable.rain_light);
                weatherDescription.setText("Light Rain");
                break;
            case 6201:
                weatherImage.setImageResource(R.drawable.freezing_rain_heavy);
                weatherDescription.setText("Heavy Freezing Rain");
                break;
            case 6001:
                weatherImage.setImageResource(R.drawable.freezing_rain);
                weatherDescription.setText("Freezing Rain");
                break;
            case 6200:
                weatherImage.setImageResource(R.drawable.freezing_rain_light);
                weatherDescription.setText("Light Freezing Rain");
                break;
            case 6000:
                weatherImage.setImageResource(R.drawable.freezing_drizzle);
                weatherDescription.setText("Freezing Drizzle");
                break;
            case 4000:
                weatherImage.setImageResource(R.drawable.drizzle);
                weatherDescription.setText("Drizzle");
                break;
            case 7101:
                weatherImage.setImageResource(R.drawable.ice_pellets_heavy);
                weatherDescription.setText("Heavy Ice Pellets");
                break;
            case 7000:
                weatherImage.setImageResource(R.drawable.ice_pellets);
                weatherDescription.setText("Ice Pellets");
                break;
            case 7102:
                weatherImage.setImageResource(R.drawable.ice_pellets_light);
                weatherDescription.setText("Light Ice Pellets");
                break;
            case 5101:
                weatherImage.setImageResource(R.drawable.snow_heavy);
                weatherDescription.setText("Heavy Snow");
                break;
            case 5000:
                weatherImage.setImageResource(R.drawable.snow);
                weatherDescription.setText("Snow");
                break;
            case 5100:
                weatherImage.setImageResource(R.drawable.snow_light);
                weatherDescription.setText("Light Snow");
                break;
            case 5001:
                weatherImage.setImageResource(R.drawable.flurries);
                weatherDescription.setText("Flurries");
                break;
            case 8000:
                weatherImage.setImageResource(R.drawable.tstorm);
                weatherDescription.setText("Thunder Storm");
                break;
            case 2100:
                weatherImage.setImageResource(R.drawable.fog_light);
                weatherDescription.setText("Light Fog");
                break;
            case 2000:
                weatherImage.setImageResource(R.drawable.fog);
                weatherDescription.setText("Fog");
                break;
            case 1001:
                weatherImage.setImageResource(R.drawable.cloudy);
                weatherDescription.setText("Cloudy");
                break;
            case 1102:
                weatherImage.setImageResource(R.drawable.mostly_cloudy);
                weatherDescription.setText("Mostly Cloudy");
                break;
            case 1101:
                weatherImage.setImageResource(R.drawable.partly_cloudy_day);
                weatherDescription.setText("Partly Cloudy");
                break;
            case 1100:
                weatherImage.setImageResource(R.drawable.mostly_clear_day);
                weatherDescription.setText("Mostly Clear");
                break;
            case 1000:
                weatherImage.setImageResource(R.drawable.clear_day);
                weatherDescription.setText("Clear");
                break;
            default:
                weatherImage.setImageResource(R.drawable.logo);
                weatherDescription.setText("No Weather Info");
        }
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

    public Date addHoursToJavaUtilDate(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
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