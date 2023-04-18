package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.content.Intent;
import android.os.Bundle;

import com.omronhealthcare.OmronConnectivitySample.R;

/**
 * Created by Omron HealthCare Inc
 */
public class SplashScreen extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Intent toDeviceList = new Intent(SplashScreen.this, OmronConnectedDeviceList.class);
        startActivity(toDeviceList);
        finish();
    }
}
