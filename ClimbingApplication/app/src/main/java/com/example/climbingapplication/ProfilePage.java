package com.example.climbingapplication;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;


public class ProfilePage extends MainActivity {

    public Button homeButton;
    public Button profileButton;
    public Button savePageButton;
    public EditText fNameIn, lNameIn, heightIn, weightIn, ageIn, sexIn;
    public DatabaseReference reference;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        fNameIn = findViewById(R.id.firstName_id);
        lNameIn = findViewById(R.id.lastName_id);
        heightIn = findViewById(R.id.height_id);
        weightIn = findViewById(R.id.weight_id);
        ageIn = findViewById(R.id.age_id);
        sexIn = findViewById(R.id.sex_id);

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

        savePageButton = findViewById(R.id.saveButton_id);
        savePageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleTextInputs();
            }
        });


    }

    public void handleTextInputs() {
        fName = fNameIn.getText().toString();
        lName = lNameIn.getText().toString();
        height = Integer.parseInt(heightIn.getText().toString());
        age = Integer.parseInt(ageIn.getText().toString());
        sex = sexIn.getText().toString();
        weight = Float.parseFloat(weightIn.getText().toString());
        fNameIn.setHint(fName);
        lNameIn.setHint(lName);
        heightIn.setHint(Integer.toString(height));
        weightIn.setHint(Float.toString(weight));
        ageIn.setHint(Integer.toString(age));
        sexIn.setHint(sex);

        Context context = getApplicationContext();                      //Write user data to firebase
        FirebaseApp.initializeApp(context);
        reference = FirebaseDatabase.getInstance().getReference();
        reference.child("User").child("FirstName").setValue(fName);
        reference.child("User").child("LastName").setValue(lName);
        reference.child("User").child("Height").setValue(Integer.toString(height));
        reference.child("User").child("Age").setValue(Integer.toString(age));
        reference.child("User").child("sex").setValue(sex);
        reference.child("User").child("weight").setValue(Float.toString(weight));

    }


}