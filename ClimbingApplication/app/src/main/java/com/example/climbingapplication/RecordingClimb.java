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
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
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
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.json.*;
import java.net.*;
import com.example.climbingapplication.AzureMLClient;


public class RecordingClimb extends MainActivity {

    private Button startBtn;
    public Boolean climbingFlag;
    private Chronometer chronometer;
    public float longitude;
    public float latitude;
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
    private int numPackets;
    private LineChart mChart;
    public float hrForGraphCurr;
    public float hrForGraphNew;
    private Thread thread;
    public boolean plotData = true;
    public String adl_scoring_url =  "http://700a2601-5dee-4509-8cd5-8a83683deb76.eastus2.azurecontainer.io/score";     //HMM URLS:
    public String fall_scoring_url = "http://e7697dac-5e32-4b3d-bf31-12d0779da73c.eastus2.azurecontainer.io/score";
    public int packetCount = 0;
    double inputData[][];
    public boolean climaFlag;
    public boolean graphFlag;

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
        startPlot();

        numPackets = 0;
        hrValue = findViewById(R.id.hrValue);
        //Create Database Reference to firebase
        Context context = getApplicationContext();
        FirebaseApp.initializeApp(context);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Packets_0");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()){
                    String pack = snap.getValue().toString();
                    float A_dsvm, A_gdsvm, A_gsvm, A_svm, HR, lati, longi, theta;
                    try {
                        JSONObject obj = new JSONObject(pack);
                        A_dsvm = Float.parseFloat(obj.getString("A_dsvm"));
                        A_gdsvm = Float.parseFloat(obj.getString("A_gdsvm"));
                        A_gsvm = Float.parseFloat(obj.getString("A_gsvm"));
                        A_svm = Float.parseFloat(obj.getString("A_svm"));
                        HR = Float.parseFloat(obj.getString("BPM"));
                        lati = Float.parseFloat(obj.getString("Latitude"));
                        longi = Float.parseFloat(obj.getString("Longitude"));
                        theta = Float.parseFloat(obj.getString("Theta"));
                        hrValue.setText(Float.toString(HR));
                        setLatitude(lati);            //Needed for ClimaCell GPS Weather Info
                        setLongitude(longi);
                        hrForGraphNew = HR;
                        System.out.println("NEW HR IS: "+HR);
                        addHRPoint(HR);
                        if(lati != 0.0 && longi != 0.0){
                            climaFlag = true;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        mapsButton = findViewById(R.id.mapsButton);
        mapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(RecordingClimb.this, MapsActivity.class);
                myIntent.putExtra("Latitude", latitude);
                myIntent.putExtra("Longitude", longitude);
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

        climaFlag = false;
        apiKey = "VCN07ZizEgitoq6L4I32o199HOojMIYj";
        tempText = findViewById(R.id.temp);
        humidityText = findViewById(R.id.humidityText);
        speedText = findViewById(R.id.speedText);
        visibilityText = findViewById(R.id.visibilityText);
        weatherImage = findViewById(R.id.weatherImage);
        weatherDescription = findViewById(R.id.weatherDescription);
        requestQueue = Volley.newRequestQueue(this);
        String url = "https://api.climacell.co/v4/locations?apikey=jFNGDUXBapyjnShPufxJHL6YsCvedU9v";


        WaitForGPS w = new WaitForGPS();    //Call ClimaCell Task (Will wait for GPS coords)
        w.execute();

        PredictFall fPred = new PredictFall();
        fPred.execute();

    }


    private void startPlot(){               //Method used for plotting HR data on live graph
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

    public void addHRPoint(float point){            //Method used to add a point to the HR graph
        LineData data = mChart.getData();
        if(data != null){
            ILineDataSet set = data.getDataSetByIndex(0);
            if(set==null){
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), point), 0);
            mChart.notifyDataSetChanged();
            data.notifyDataChanged();
            mChart.setMaxVisibleValueCount(10);
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    public LineDataSet createSet(){                                     //Method used to create datasets for HR graph
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



    public void requestWithSomeHttpHeaders() {                                                          //Methond used to access ClimaCell API
        RequestQueue queue = Volley.newRequestQueue(this);
        //System.out.println("LATITUDE: "+latitude+" LONGITUDE: "+longitude);
        String url = "https://data.climacell.co/v4/timelines?location="+longitude+"%2C"+latitude;
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
                        parseClimaResponse(response);
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

    public void makeToast(String toastMessage){                 //Method used to create toasts and notify user of something
        Context context = getApplicationContext();
        CharSequence text = toastMessage;
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.TOP| Gravity.LEFT, 0, 0);
        toast.show();
    }

    public void emergencyCall(){                    //Method used to simulate call to 911
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

    public void parseClimaResponse(String response){                //Method used to parse the response from ClimaCell API
        //System.out.println(response);
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
        visibilityText.setText(Float.toString(weatherResponse[6])+" km");

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

    public void startstopChronometer(View v){                   //Method used for starting and stopping the climbing timer
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
        return this.longitude;
    }
    public double getLatitude(){
        return this.latitude;
    }
    private void setLongitude(float val){
        this.longitude = val;
    }
    private void setLatitude(float val){
        this.latitude = val;
    }

    public Date addHoursToJavaUtilDate(Date date, int minutes) {        //Method used to format ClimaCell Time Data
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }


    class PredictFall extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {

            try {       //ADL detection
                URL url = new URL(adl_scoring_url); //Enter URL here
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST"); // here you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
                httpURLConnection.setRequestProperty("Content-Type", "application/json"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
                httpURLConnection.connect();

                double[][] input_array = {{1.038291243f,    -45.00022028f,   0.038669902f,    -0.519148163f,   -0.019335046f},
                        {1.0548177f,      -45.0075678f,    0.038472101f,    -0.527497546f,   -0.019239285f},
                        {1.078606097f,    -45.01275791f,	0.030757843f,	-0.539455946f,	-0.015383282f},
                        {1.092423861f,    -45.01555917f,	0.030757843f,	-0.546400788f,	-0.015384239f},
                        {1.104315598f,	-45.00470017f,	0.019918045f,	-0.552215471f,	-0.009960063f},
                        {1.127878533f,	-45.00018012f,	0.028437929f,	-0.563941524f,	-0.014219022f},
                        {1.110921293f,	-45.01522295f,	0.045554312f,	-0.555648552f,	-0.022784861f},
                        {1.055880402f,	-45.04084103f,	0.059241214f,	-0.528419348f,	-0.02964749f},
                        {1.086731106f,	-45.00948369f,	0.05496581f,     -0.543480067f,	-0.027488697f},
                        {1.083947423f,	-45.03817694f,	0.027621359f,	-0.542433509f,	-0.013822396f},
                        {1.124911835f,	-45.07865654f,	0.064659943f,	-0.563439047f,	-0.032386482f},
                        {1.133095331f,	-45.00017635f,	0.080907481f,	-0.566549886f,	-0.040453899f},
                        {1.124687999f,   	-45,        	0.009568319f,   	-0.562344f,      -0.00478416f},
                        {1.107268577f,	-45.00165604f,	0.022777156f,	-0.553654663f,	-0.011388997f},
                        {1.090641509f,	-45.02289999f,	0.036851489f,	-0.545598262f,	-0.018435121f}};


                Map<String, Object> mymap = new HashMap<String,Object>();
                mymap.put("method", "predict");
                mymap.put("data", input_array);
                Gson gson = new Gson();
                String json = gson.toJson(mymap);

                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                wr.writeBytes(json);
                wr.flush();
                wr.close();

                InputStream response = httpURLConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                StringBuilder sb = new StringBuilder();
                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("ADL RESPONSE IS: "+sb.toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {       //Fall detection
                URL url2 = new URL(fall_scoring_url); //Enter URL here
                HttpURLConnection httpURLConnection2 = (HttpURLConnection)url2.openConnection();
                httpURLConnection2.setDoOutput(true);
                httpURLConnection2.setRequestMethod("POST"); // here you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
                httpURLConnection2.setRequestProperty("Content-Type", "application/json"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
                httpURLConnection2.connect();

                double[][] input_array = {{1.038291243f,    -45.00022028f,   0.038669902f,    -0.519148163f,   -0.019335046f},
                        {1.0548177f,      -45.0075678f,    0.038472101f,    -0.527497546f,   -0.019239285f},
                        {1.078606097f,    -45.01275791f,	0.030757843f,	-0.539455946f,	-0.015383282f},
                        {1.092423861f,    -45.01555917f,	0.030757843f,	-0.546400788f,	-0.015384239f},
                        {1.104315598f,	-45.00470017f,	0.019918045f,	-0.552215471f,	-0.009960063f},
                        {1.127878533f,	-45.00018012f,	0.028437929f,	-0.563941524f,	-0.014219022f},
                        {1.110921293f,	-45.01522295f,	0.045554312f,	-0.555648552f,	-0.022784861f},
                        {1.055880402f,	-45.04084103f,	0.059241214f,	-0.528419348f,	-0.02964749f},
                        {1.086731106f,	-45.00948369f,	0.05496581f,     -0.543480067f,	-0.027488697f},
                        {1.083947423f,	-45.03817694f,	0.027621359f,	-0.542433509f,	-0.013822396f},
                        {1.124911835f,	-45.07865654f,	0.064659943f,	-0.563439047f,	-0.032386482f},
                        {1.133095331f,	-45.00017635f,	0.080907481f,	-0.566549886f,	-0.040453899f},
                        {1.124687999f,   	-45,        	0.009568319f,   	-0.562344f,      -0.00478416f},
                        {1.107268577f,	-45.00165604f,	0.022777156f,	-0.553654663f,	-0.011388997f},
                        {1.090641509f,	-45.02289999f,	0.036851489f,	-0.545598262f,	-0.018435121f}};


                Map<String, Object> mymap2 = new HashMap<String,Object>();
                mymap2.put("method", "predict");
                mymap2.put("data", input_array);
                Gson gson = new Gson();
                String json = gson.toJson(mymap2);

                DataOutputStream wr = new DataOutputStream(httpURLConnection2.getOutputStream());
                wr.writeBytes(json);
                wr.flush();
                wr.close();

                InputStream response = httpURLConnection2.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                StringBuilder sb = new StringBuilder();
                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("FALL RESPONSE IS: "+sb.toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }


    }

    class WaitForGPS extends AsyncTask<Void,Void,Void>{     //Used to see if GPS has been set before calling ClimaCell
        @Override
        protected Void doInBackground(Void... params) {
            while(!climaFlag){}
            requestWithSomeHttpHeaders();
            return null;
        }
    }

}