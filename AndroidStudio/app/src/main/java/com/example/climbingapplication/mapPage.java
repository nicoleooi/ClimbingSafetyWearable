package com.example.climbingapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class mapPage extends AppCompatActivity implements OnMapReadyCallback{

    public double longitude;
    public double latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_page);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.map, mapFragment)
                .commit();

        latitude = 43.793430;
        longitude = -79.137750;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        if(latitude!=0 && longitude!=0) {
            map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Marker"));
        }
    }
}