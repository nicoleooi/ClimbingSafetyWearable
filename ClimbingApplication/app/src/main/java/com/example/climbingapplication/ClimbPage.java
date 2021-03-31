package com.example.climbingapplication;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ClimbPage extends MainActivity {

    private Button startButton;
    public Boolean readyFlag;

    public double longitude;
    public double latitude;
    private Button homeButton;
    private Button profileButton;
    private Button mapsButton;
    private TextView waitGPSText;
    private ImageView gpsSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_climb_page);

        waitGPSText = findViewById(R.id.waitGPSText);
        readyFlag = false;
        startButton = findViewById(R.id.startBtn);
        startButton.setBackgroundColor(0xFFEE0000);
        startButton.setEnabled(false);
        startButton.setText("Not Ready");
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRecordClimbPage();
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

        gpsSymbol = findViewById(R.id.gpsSymbol);
        findViewById(R.id.gpsSymbol).setVisibility(View.GONE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        (new Handler()).postDelayed(this::onReady, 5000);
    }

    public void onReady() {
        waitGPSText.setText("Signal Acquired.");
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        findViewById(R.id.gpsSymbol).setVisibility(View.VISIBLE);
        startButton.setBackgroundColor(0xFF008000);
        startButton.setText("Ready to climb");
        startButton.setEnabled(true);
        readyFlag = true;

        Toast toast = Toast.makeText(ClimbPage.this, "GPS Signal Acquired!",
                Toast.LENGTH_LONG);
        toast.show();
    }

}