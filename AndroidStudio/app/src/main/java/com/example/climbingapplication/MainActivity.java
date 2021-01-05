package com.example.climbingapplication;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;



public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainPage";
    private Button statsPageButton;
    private Button climbingPageButton;

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


        climbingPageButton = findViewById(R.id.startClimbButton);
        climbingPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openClimbingPage();
            }
        });

        statsPageButton = findViewById(R.id.activityButton);
        statsPageButton.setOnClickListener(new View.OnClickListener(){
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

    public void openClimbingPage() {
        Intent intent = new Intent(this, ClimbPage.class);
        startActivity(intent);
    }


}