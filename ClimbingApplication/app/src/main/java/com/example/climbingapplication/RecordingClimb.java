package com.example.climbingapplication;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.*;
import java.net.*;

import javax.xml.transform.Result;


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
    private int numHRPackets;
    private int numHMMPackets;
    private int numClimaPackets = 0;
    private LineChart mChart;
    private Thread thread;
    public boolean plotData = true;
    public String adl_scoring_url =  "http://700a2601-5dee-4509-8cd5-8a83683deb76.eastus2.azurecontainer.io/score";         //HMM URLS:
    public String fall_scoring_url = "http://4236faf9-996e-4af4-95ae-ec7e36bb82b2.eastus2.azurecontainer.io/score";
    public String hr_scoring_url = "http://4db72b57-36d8-4ec3-a793-ba1e365a7463.eastus2.azurecontainer.io/score";           //LSTM URL
    public int packetCount = 0;
    double inputData[][];
    public boolean climaFlag;
    public boolean graphFlag;
    double[] inputHR_array;
    public boolean zeroBPM = false;
    double hr_array_nums[] = new double[10];    //Array to store predicted HR Values
    public boolean hrFlag, fall;
    public double inputHMM_array[][]  = new double[15][5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_climb);

        //Get user data from firebase
        Context userContext = getApplicationContext();
        FirebaseApp.initializeApp(userContext);
        DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("User");
        userReference.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){
                age = Integer.parseInt(dataSnapshot.child("Age").getValue(String.class));
                fName = dataSnapshot.child("FirstName").getValue(String.class);
                lName = dataSnapshot.child("LastName").getValue(String.class);
                height = Integer.parseInt(dataSnapshot.child("Height").getValue(String.class));
                sex = dataSnapshot.child("sex").getValue(String.class);
                weight = Float.parseFloat(dataSnapshot.child("weight").getValue(String.class));
                System.out.println("User Information: First Name - "+fName+"\tLast Name - "+lName+"\tAge - "+age+"\tHeight - "+height+"\tSex - "+sex+"\tWeight - "+weight);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Problem obtaining user data due to: "+error.getMessage());
            }
        });


        //Graph view setup:
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.getDescription().setEnabled(false);
        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.cBlue, null));
        LineData hrGraphData = new LineData();
        hrGraphData.setValueTextColor(Color.WHITE);
        mChart.setData(hrGraphData);
        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setEnabled(false);
        l.setTextColor(Color.BLACK);
        XAxis xl = mChart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(220f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);

        startPlot();


        inputHR_array = new double[15];
        numHRPackets = 0;
        numHMMPackets = 0;
        numClimaPackets = 0;
        zeroBPM = false;
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
                        if(numClimaPackets % 180 == 0){
                            climaFlag = true;
                            WaitForGPS w = new WaitForGPS();
                            w.execute();
                        }
                        if(lati != 0.0 && longi != 0.0){
                            numClimaPackets++;
                        }
                        if(numHMMPackets == 15){
                            PredictFall fPred = new PredictFall();
                            fPred.execute();
                            WaitForBadFall wFall = new WaitForBadFall();
                            wFall.execute();
                            numHMMPackets = 0;
                        }
                        if(numHRPackets == 15){
                            System.out.println("15 Packets Received, Predicting...");
                            PredictHR hrPred = new PredictHR();
                            hrPred.execute();
                            WaitForBadHR wHR = new WaitForBadHR();
                            wHR.execute();
                            numHRPackets = 0;
                        }
                        if(HR == 0.0){              //If we received an invalid HR
                            zeroBPM = true;         //Set the flag to notify not to increment numHRPackets
                        } else{                                      //Otherwise store in array for LSTM and graph
                            inputHR_array[numHRPackets] = HR;
                            addHRPoint(HR);
                        }
                        inputHMM_array[numHMMPackets][0] = A_svm;
                        inputHMM_array[numHMMPackets][1] = theta;
                        inputHMM_array[numHMMPackets][2] = A_dsvm;
                        inputHMM_array[numHMMPackets][3] = A_gsvm;
                        inputHMM_array[numHMMPackets][4] = A_gdsvm;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(!zeroBPM){
                        numHRPackets++;
                    }
                    numHMMPackets++;
                    zeroBPM = false;
                }
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
                System.out.println("LATITUDE FOR MAPS: "+latitude+" LONGITUDE FOR MAPS: "+longitude);
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

        apiKey = "VCN07ZizEgitoq6L4I32o199HOojMIYj";
        tempText = findViewById(R.id.temp);
        humidityText = findViewById(R.id.humidityText);
        speedText = findViewById(R.id.speedText);
        visibilityText = findViewById(R.id.visibilityText);
        weatherImage = findViewById(R.id.weatherImage);
        weatherDescription = findViewById(R.id.weatherDescription);
        requestQueue = Volley.newRequestQueue(this);
        String url = "https://api.climacell.co/v4/locations?apikey=jFNGDUXBapyjnShPufxJHL6YsCvedU9v";


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



    public void requestWithSomeHttpHeaders() {                                                          //Methond used to access ClimaCell
        System.out.println("LATITUDE: "+latitude+" LONGITUDE: "+longitude);
        RequestQueue queue = Volley.newRequestQueue(this);
        //System.out.println("LATITUDE: "+latitude+" LONGITUDE: "+longitude);
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
        int temperatureInt = (int)weatherResponse[0];
        tempText.setText(Integer.toString(temperatureInt));                   //"\u00B0"+"C"
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

    class PredictHR extends AsyncTask<Void, Void, Void>{
        @Override
        protected  Void doInBackground(Void... params){
            try {       //HR prediction
                URL url = new URL(hr_scoring_url); //Enter URL here
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST"); // here you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
                httpURLConnection.setRequestProperty("Content-Type", "application/json"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
                httpURLConnection.connect();

                Map<String, Object> mymap = new HashMap<String,Object>();
                mymap.put("method", "predict");
                mymap.put("data", inputHR_array);
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
                hrFlag = false;
                String hr_string = sb.toString().substring(18, sb.toString().length() - 4);
                String[] hr_array = hr_string.split(",");
                System.out.print("\nNext 10 Predicted HRs: ");
                for (int i = 0; i < hr_array.length; i++){
                    hr_array_nums[i] = Double.parseDouble(hr_array[i]);
                    System.out.print(i+" : "+hr_array_nums[i]+"\t");
                    if(hr_array_nums[i] >= (211 - (0.64*age)) - 15){
                        hrFlag = true;
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }


    class PredictFall extends AsyncTask<Void,Void,Void>{

        double adl_score, fall_score;

        @Override
        protected Void doInBackground(Void... params) {

            try {       //ADL detection
                URL url = new URL(adl_scoring_url); //Enter URL here
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST"); // here you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
                httpURLConnection.setRequestProperty("Content-Type", "application/json"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
                httpURLConnection.connect();

                Map<String, Object> mymap = new HashMap<String,Object>();
                mymap.put("method", "predict");
                mymap.put("data", inputHMM_array);
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
                String adl_score_string;
                adl_score_string =  sb.toString().substring(12, sb.toString().length() - 2);
                adl_score = Double.parseDouble(adl_score_string);
                System.out.println("ADL SCORE: "+adl_score);
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

                Map<String, Object> mymap2 = new HashMap<String,Object>();
                mymap2.put("method", "predict");
                mymap2.put("data", inputHMM_array);
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
                String fall_score_string;
                fall_score_string =  sb.toString().substring(14, sb.toString().length() - 2);
                fall_score = Double.parseDouble(fall_score_string);
                System.out.println("FALL SCORE: "+fall_score);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            thresholdScores();
            return null;
        }

        public void thresholdScores(){
            //imbalanced thresholding here
            System.out.println("ADL SCORE INSIDE "+adl_score);
            System.out.println("FALL SCORE INSIDE "+fall_score);
            if ((fall_score < 0) && (adl_score < 0)){
                double fall_score2 = Math.exp(fall_score);
                double adl_score2 = Math.exp(adl_score);
                double percent_fall = (fall_score2)/(fall_score2 + adl_score2);
                if(percent_fall >= 0.55) fall = true;
                else fall = false;
            }
            else if((fall_score < 0) && (adl_score > 0)){            //if one 's neg
                fall = false;
            }
            else if((fall_score > 0) && (adl_score < 0)){
                fall = true;
            }
            else{
                double percent_fall = (double)(fall_score)/(double)(fall_score + adl_score);
                System.out.println("GOT IN HERE: "+percent_fall);
                if(percent_fall >= 0.55) fall = true;
                else  fall = false;
            }
            System.out.println(fall);
            if(fall){
                System.out.println("FALL DETECTED ##############");
            }
            return;
        }
    }

    class WaitForGPS extends AsyncTask<Void,Void,Void>{     //Used to see if GPS has been set before calling ClimaCell
        @Override
        protected Void doInBackground(Void... params) {
            System.out.println("CLIMA FLAG: "+climaFlag);
            while(!climaFlag){}
            requestWithSomeHttpHeaders();
            return null;
        }
    }

    class WaitForBadHR extends AsyncTask<Void,Void,Void>{     //Used to see if GPS has been set before calling ClimaCell
        @Override
        protected void onPostExecute(Void v) {
            if(!hrFlag){return;}
            PopUpClass p = new PopUpClass();
            p.showPopupWindowHR(findViewById(android.R.id.content));
            return;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }

    class WaitForBadFall extends AsyncTask<Void,Void,Void>{     //Used to see if GPS has been set before calling ClimaCell
        @Override
        protected void onPostExecute(Void v) {
            if(!fall){return;}
            System.out.println("GOT TO THIS PART");
            PopUpClass p2 = new PopUpClass();
            p2.showPopupWindowFall(findViewById(android.R.id.content));
            return;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }


}