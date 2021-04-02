package com.example.climbingapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

public class CountDownDialog extends DialogFragment {

    public boolean dontCall = false;
    private TextView mCountdownView;

    public CountDownDialog(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.countdown_dialog, container);
        mCountdownView = (TextView) view.findViewById(R.id.countdownTimer);
        Button buttonEdit = view.findViewById(R.id.messageButton);
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dontCall = true;
                dismiss();
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);



        CountDownTimer gameTimer = new CountDownTimer(11000, 1000) {
            @Override
            public void onTick(long l) {
                mCountdownView.setText(""+((int)Math.round(l/1000.0)-1));
            }

            @Override
            public void onFinish() {
                if(!dontCall){
                    emergencyCall();
                }
                dismiss();
            }
        };
        gameTimer.start();
    }

    public void emergencyCall(){                    //Method used to simulate call to 911
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:+911"));
        startActivity(callIntent);
    }


}
