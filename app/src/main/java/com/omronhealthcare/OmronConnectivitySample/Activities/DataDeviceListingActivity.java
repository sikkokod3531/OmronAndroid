package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.adapter.DataSavedDevicesListAdapter;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;
import com.omronhealthcare.OmronConnectivitySample.utility.PreferencesManager;

/**
 * Created by Omron HealthCare Inc
 */
public class DataDeviceListingActivity extends AppCompatActivity {

    private RecyclerView rvSavedDevices;

    private PreferencesManager preferencesManager;
    private DataSavedDevicesListAdapter dataSavedDevicesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_device_listing);

        preferencesManager = new PreferencesManager(DataDeviceListingActivity.this);
        rvSavedDevices = (RecyclerView) findViewById(R.id.rv_saved_devices);

        dataSavedDevicesListAdapter = new DataSavedDevicesListAdapter(DataDeviceListingActivity.this, preferencesManager.getDataStoredDeviceList(), new DataSavedDevicesListAdapter.DeviceItemSelect() {
            @Override
            public void onItemSelect(String localName,String model, Integer category, int position) {
                if (category == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) { // HeartVue or Activity
                    Intent toVitalData = new Intent(DataDeviceListingActivity.this, DataListingActivity.class);
                    toVitalData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME,localName);
                    startActivity(toVitalData);
                } else if(category == OmronConstants.OMRONBLEDeviceCategory.PULSEOXIMETER) {
                    Intent toVitalData = new Intent(DataDeviceListingActivity.this, PulseOxymeterDataListingActivity.class);
                    toVitalData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME,localName);
                    startActivity(toVitalData);
                } else {
                    Intent toVitalData = new Intent(DataDeviceListingActivity.this, VitalDataListingActivity.class);
                    toVitalData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME,localName);
                    startActivity(toVitalData);
                }
            }
        });


        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(DataDeviceListingActivity.this, LinearLayoutManager.VERTICAL, false);
        rvSavedDevices.setLayoutManager(linearLayoutManager);
        rvSavedDevices.setAdapter(dataSavedDevicesListAdapter);

    }
}
