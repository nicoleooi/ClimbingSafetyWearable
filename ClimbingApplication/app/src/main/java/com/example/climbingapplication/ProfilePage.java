package com.example.climbingapplication;
import androidx.appcompat.app.AppCompatActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.TimeUnit;


public class ProfilePage extends MainActivity {

    private Button homeButton;
    private Button profileButton;
    private Button savePageButton;
    private EditText fNameIn, lNameIn, heightIn, weightIn, ageIn, sexIn;
    public String fName, lName;
    public Integer height, age;
    public char sex;
    public float weight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

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

        fNameIn = findViewById(R.id.firstName_id);
        lNameIn = findViewById(R.id.lastName_id);
        heightIn = findViewById(R.id.height_id);
        weightIn = findViewById(R.id.weight_id);
        ageIn = findViewById(R.id.age_id);
        sexIn = findViewById(R.id.sex_id);

        Cursor resultSet = userDb.rawQuery("Select * from Data",null);        //NEED TO FIX!!!!!
        resultSet.moveToFirst();
        String flag = resultSet.getString(0);
        if(!(flag.isEmpty())){
            fNameIn.setText("First Name: "+flag);
            lNameIn.setText("Last Name: "+resultSet.getString(1));
            heightIn.setText("Height: "+resultSet.getString(2));
            weightIn.setText("Weight: "+resultSet.getString(3));
            ageIn.setText("Age: "+resultSet.getString(4));
            sexIn.setText("Sex: "+resultSet.getString(5));
        }


    }

    public void handleTextInputs() {
        fName = fNameIn.getText().toString();
        lName = lNameIn.getText().toString();
        height = Integer.parseInt(heightIn.getText().toString());
        age = Integer.parseInt(ageIn.getText().toString());
        sex = sexIn.getText().toString().charAt(0);
        weight = Float.parseFloat(weightIn.getText().toString());
        userDb.execSQL("INSERT INTO Data VALUES('"+fName+"','"+lName+"','"+age+"','"+weight+"',"+height+",'"+sex+"')");
        fNameIn.setText("");
        lNameIn.setText("");
        heightIn.setText("");
        weightIn.setText("");
        ageIn.setText("");
        sexIn.setText("");
        fNameIn.setHint(fName);
        lNameIn.setHint(lName);
        heightIn.setHint(""+height);
        weightIn.setHint(""+weight);
        ageIn.setHint(""+age);
        sexIn.setHint(""+sex);

    }


}