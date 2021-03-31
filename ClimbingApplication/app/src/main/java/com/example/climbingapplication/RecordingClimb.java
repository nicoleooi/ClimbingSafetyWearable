package com.example.climbingapplication;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.Buffer;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.Random;
import org.json.*;



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
    private TextView tempText, humidityText, speedText, visibilityText, weatherDescription, hrValue;
    private ImageView weatherImage;
    private String apiKey;
    private float[] weatherResponse = new float[8];
    private float timeClimbedFor;
    private String fields[] = {"temperatureApparent", "humidity", "windSpeed", "precipitationIntensity", "precipitationProbability", "precipitationType", "visibility", "weatherCode"};
    private static final int REQUEST_CALL = 1;
    private ListView listView;
    private int numPackets;
    private LineChart mChart;
    public float hrForGraph;
    private Thread thread;
    public boolean plotData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_climb);


        //Graph view setup:
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.getDescription().setEnabled(false);
        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(Color.WHITE);
        LineData hrGraphData = new LineData();
        hrGraphData.setValueTextColor(Color.WHITE);
        mChart.setData(hrGraphData);

        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);
        startPlot();

        numPackets = 0;
        hrValue = findViewById(R.id.hrValue);
        //listView = findViewById(R.id.listView);         //Setting up the list view
        //ArrayList<String> list = new ArrayList<>();
        //ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.list_item, list);
        //listView.setAdapter(adapter);

        //Create Database Reference to firebase
        Context context = getApplicationContext();
        FirebaseApp.initializeApp(context);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Packets_"+Integer.toString(numPackets));
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()){
                    String pack = snap.getValue().toString();
                    try {
                        JSONObject obj = new JSONObject(pack);
                        float A_dsvm = Float.parseFloat(obj.getString("A_dsvm"));
                        float A_gdsvm = Float.parseFloat(obj.getString("A_gdsvm"));
                        float A_gsvm = Float.parseFloat(obj.getString("A_gsvm"));
                        float A_svm = Float.parseFloat(obj.getString("A_svm"));
                        float HR = Float.parseFloat(obj.getString("BPM"));
                        float latitude = Float.parseFloat(obj.getString("Latitude"));
                        float longitude = Float.parseFloat(obj.getString("Longitude"));
                        float theta = Float.parseFloat(obj.getString("Theta"));
                        //list.add("HR: "+HR+"\nA_svm: "+A_svm+"\nA_dsvm: "+A_dsvm+"\nA_gsvm: "+A_gsvm+"\nA_gdsvm: "+A_gdsvm+"\nTheta: "+theta+"\nLongitude: "+longitude+"\nLatitude: "+latitude);
                        hrValue.setText(Float.toString(HR));
                        hrForGraph = HR;
                        if (thread != null) {
                            thread.interrupt();
                        }
                        if(plotData){
                            addHRPoint(hrForGraph);
                            plotData = false;
                        }
                        System.out.println("New HR Value: "+HR);
                        System.out.println(plotData);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //adapter.notifyDataSetChanged();
                numPackets ++;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        latitude = 44.6295;
        longitude = -63.5875;

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

    private void startPlot(){
        if(thread!=null){
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    plotData = true;
                    try{
                        Thread.sleep(10);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onPostResume(){
        super.onPostResume();
    }

    @Override
    protected void onDestroy(){
        thread.interrupt();
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(thread != null){
            thread.interrupt();
        }
    }

    public void addHRPoint(float point){
        LineData data = mChart.getData();
        System.out.println("DATA: "+data);
        if(data != null){
            ILineDataSet set = data.getDataSetByIndex(0);
            System.out.println("AHHHHHHHHHHHHHHHH___Data was null" + set);
            System.out.println("DATA__1: "+data);
            if(set==null){
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), point), 0);
            System.out.println("AHHHHHHHHHHHHHHHH___Added Entry");
            System.out.println("DATA__2: "+data);
            mChart.notifyDataSetChanged();
            data.notifyDataChanged();
            mChart.setMaxVisibleValueCount(10);
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    public LineDataSet createSet(){
        System.out.println("AHHHHHHHHHHHHHHHH___Called function");
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3);
        set.setColor(Color.BLUE);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }



    public void requestWithSomeHttpHeaders() {                                                          //Access ClimaCell API
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

    public void makeToast(String toastMessage){
        Context context = getApplicationContext();
        CharSequence text = toastMessage;
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.TOP| Gravity.LEFT, 0, 0);
        toast.show();
    }

    public void emergencyCall(){
        if(ContextCompat.checkSelfPermission(RecordingClimb.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(RecordingClimb.this, new String[] {Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        }
        else{
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:+911"));
            startActivity(callIntent);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CALL){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                emergencyCall();
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
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

    public double getLongitude(){
        return longitude;
    }
    public double getLatitude(){
        return latitude;
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