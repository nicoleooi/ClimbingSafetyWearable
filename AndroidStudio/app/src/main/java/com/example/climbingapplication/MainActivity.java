package com.example.climbingapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.microsoft.windowsazure.mobileservices.*;

import java.net.MalformedURLException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainPage";
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Make connection to AZURE
        try {
            MobileServiceClient mClient = new MobileServiceClient(
                    "https://androiddata.azurewebsites.net", // Our site URL
                    this);
            AzureServiceAdapter.Initialize(this);
        } catch (MalformedURLException e){
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.activityButton);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                openStatsPage();
            }
        });
    }

    public void openStatsPage() {
        Intent intent = new Intent(this, StatsPage.class);
        startActivity(intent);
    }
}