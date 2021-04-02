package com.example.climbingapplication;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainPage";
    private Button climbingPageButton;
    private Toolbar mToolbar;
    private Button homeButton;
    private Button profileButton;
    public String fName, lName;
    public Integer height, age;
    public String sex;
    public float weight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setBackgroundColor(5);

        climbingPageButton = findViewById(R.id.startClimbButton);
        climbingPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openClimbingPage();
            }
        });

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
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
    }


    public void openClimbingPage() {
        Intent intent = new Intent(this, ClimbPage.class);
        startActivity(intent);
    }

    public void openHomePage() {
        if(!(this.getClass().equals(MainActivity.class))) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    public void openProfilePage() {
        if(!(this.getClass().equals(ProfilePage.class))) {
            Intent intent = new Intent(this, ProfilePage.class);
            startActivity(intent);
        }
    }

    public void openRecordClimbPage() {
        Intent intent = new Intent(this, RecordingClimb.class);
        startActivity(intent);
    }



}