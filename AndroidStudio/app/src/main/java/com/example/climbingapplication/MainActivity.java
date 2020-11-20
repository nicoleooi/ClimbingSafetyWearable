package com.example.climbingapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.windowsazure.mobileservices.*;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainPage";
    private Button button;
    public Connection con;          //For database connection
    public Button testDbButton;
    public TextView testName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Make connection to AZURE app server
//        try {
//            MobileServiceClient mClient = new MobileServiceClient(
//                    "https://androiddata.azurewebsites.net", // Our site URL
//                    this);
//            AzureServiceAdapter.Initialize(this);
//        } catch (MalformedURLException e){
//            e.printStackTrace();
//        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testDbButton = findViewById(R.id.dbTestButton);
        testDbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testDB tdb = new testDB();
                tdb.execute("");
            }
        });

        button = findViewById(R.id.activityButton);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                openStatsPage();
            }
        });
    }

    public class testDB extends AsyncTask<String, String, String> {
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

    public void openStatsPage() {
        Intent intent = new Intent(this, StatsPage.class);
        startActivity(intent);
    }


    @SuppressLint("NewAPI")
    public Connection connectionclass() {   //Method for connecting to Azure Database
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