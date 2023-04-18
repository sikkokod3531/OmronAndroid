package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.fragments.ActivityDataFragment;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;

/**
 * Created by Omron HealthCare Inc
 */
public class ActivityDataActivity extends AppCompatActivity {

    private TextView mTvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        mTvTitle = (TextView) findViewById(R.id.textView);

        String itemName = getIntent().getExtras().getString(Constants.bundleKeys.KEY_ACTIVITY_DATA_TYPE);
        String itemKey = getIntent().getExtras().getString(Constants.bundleKeys.KEY_ACTIVITY_DATA_KEY);
        String localName = getIntent().getExtras().getString(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME);

        mTvTitle.setText(itemName);
        getSupportFragmentManager().beginTransaction().add(R.id.frameLayout, ActivityDataFragment.newInstance(itemKey,localName)).commit();
    }
}
