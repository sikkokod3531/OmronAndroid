package com.omronhealthcare.OmronConnectivitySample.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.DeviceConfiguration.OmronPeripheralManagerConfig;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerRecordSignalListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerRecordListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.LibraryManager.OmronPeripheralManager;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronErrorInfo;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronPeripheral;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.App;
import com.omronhealthcare.OmronConnectivitySample.BuildConfig;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class TemperatureRecordingActivity extends AppCompatActivity {

    private String TAG = "TemperatureRecordingActivity";

    private TextView mTvTImeStamp, mTvTemperature, mTvDistance, mTvDisclaimer;
    private Button scanBtn;

    private static final int REQUEST_CODE = 1;
    private static final String[] PERMISSIONS;
    private Runnable mRunnablePostPermissionGranted = null;

    HashMap<String, String> device = null;

    boolean isRecording = false;

    static {
        PERMISSIONS = new String[] { Manifest.permission.RECORD_AUDIO };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_recording);

        // Selected device
        device = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_DEVICE);

        initViews();
        resetView();
        initClickListeners();

        startOmronPeripheralManager();
    }

    @Override
    protected void onResume() {
        super.onResume();

        requestMicrophonePermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void requestMicrophonePermission() {

//        for (String permission : PERMISSIONS) {
//            if (PermissionChecker.checkSelfPermission(this, permission) != PermissionChecker.PERMISSION_GRANTED) {
//                showGoToSettingsDialog();
//                return;
//            }
//        }
//        postPermissionGranted();

        requestPermission(null);
    }

    protected void requestPermission(Runnable runnablePostPermissionGranted) {
        mRunnablePostPermissionGranted = runnablePostPermissionGranted;

        final List<String> permissionList = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(this, permission) != PermissionChecker.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }
        if (permissionList.size() <= 0) {
            postPermissionGranted();
            return;
        }

        final String[] permissions = new String[permissionList.size()];
        permissionList.toArray(permissions);
        permissionList.clear();

        new AlertDialog.Builder(this)
                .setTitle(R.string.audio_title_permission_required)
                .setMessage(R.string.audio_message_permission)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        ActivityCompat.requestPermissions(TemperatureRecordingActivity.this, permissions, REQUEST_CODE);
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

    private void postPermissionGranted() {
        if (mRunnablePostPermissionGranted != null) {
            mRunnablePostPermissionGranted.run();
            mRunnablePostPermissionGranted = null;
        }
    }

    private void showGoToSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.audio_title_permission_required)
                .setMessage(R.string.audio_message_go_to_settings)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        //intent.putExtra("extra_prefs_show_button_bar", true);
                        //intent.putExtra("extra_prefs_set_next_text", "");
                        intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                        startActivity(intent);
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PermissionChecker.PERMISSION_GRANTED) {
                    showGoToSettingsDialog();
                    return;
                }
            }
            postPermissionGranted();
        }
    }

    private void startOmronPeripheralManager() {

        OmronPeripheralManagerConfig peripheralConfig = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getConfiguration();
        Log.d(TAG, "Library Identifier : " + peripheralConfig.getLibraryIdentifier());

        // Filter device to scan and connect (optional)
        if (device != null && device.get(OmronConstants.OMRONBLEConfigDevice.GroupID) != null && device.get(OmronConstants.OMRONBLEConfigDevice.GroupIncludedGroupID) != null) {

            // Add item
            List<HashMap<String, String>> filterDevices = new ArrayList<>();
            filterDevices.add(device);
            peripheralConfig.deviceFilters = filterDevices;
        }

        // Set Scan timeout interval (optional)
        peripheralConfig.timeoutInterval = Constants.CONNECTION_TIMEOUT;


        // Set configuration for OmronPeripheralManager
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).setConfiguration(peripheralConfig);

        //Initialize the connection process.
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).startManager();

    }

    private void startScan() {

        if(isRecording) {
            scanBtn.setText("Start");
            stopRecording();
            resetView();
        }else {
            scanBtn.setText("Stop");
            startRecording();
        }

        isRecording = !isRecording;
    }


    private void startRecording() {

        mTvDisclaimer.setText("Turn ON Thermometer and place near microphone. Transferring in progress...");

        OmronPeripheral peripheral = new OmronPeripheral(OmronConstants.OMRONThermometerMC280B, "");
        HashMap<String, String> deviceInformation = peripheral.getDeviceInformation();
        Log.d(TAG, "Device Information : " + deviceInformation);


        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).startRecording(peripheral, new OmronPeripheralManagerRecordSignalListener() {
            @Override
            public void onSignalStrength(double signalLevel) {

                mTvDistance.setText(signalLevel(signalLevel) + " meter(s) away");
            }
        }, new OmronPeripheralManagerRecordListener() {
            @Override
            public void onRecord(OmronPeripheral peripheral, OmronErrorInfo errorInfo) {

                if (errorInfo != null) {
                    Log.d(TAG, "Error : " + errorInfo.getResultCode());
                    Log.d(TAG, "Error : " +  errorInfo.getDetailInfo());
                    Log.d(TAG, "Error : " + errorInfo.getMessageInfo());

                    mTvDisclaimer.setText(errorInfo.getMessageInfo());

                }else {

                    // Get vital data for previously selected user using OmronPeripheral
                    Object output = peripheral.getVitalData();

                    HashMap<String, Object> vitalData = (HashMap<String, Object>) output;

                    if (vitalData != null) {
                        // Temperature Data
                        final ArrayList<HashMap<String, Object>> temperatureIemList = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataTemperatureKey);
                        if (temperatureIemList != null) {
                            updateUIWithData(temperatureIemList);
                        }else {
                            // No readings
                            mTvDisclaimer.setText("Turn OFF Thermometer");
                        }
                    }else {
                        // No readings
                        mTvDisclaimer.setText("Turn OFF Thermometer");
                    }

                    stopRecording();
                }

                scanBtn.setText("Start");
                isRecording = !isRecording;
            }
        });
    }


    private void stopRecording() {

        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).stopRecording(null);
    }

    private int signalLevel(double signal) {
        int level;
        if (signal > 30) {
            level = 5;
        } else if (signal > 26) {
            level = 4;
        } else if (signal > 22) {
            level = 3;
        } else if (signal > 18) {
            level = 2;
        } else if (signal > 14) {
            level = 1;
        } else {
            level = 0;
        }
        return level;
    }

    private void updateUIWithData(final ArrayList<HashMap<String, Object>> vitalData) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (vitalData.size() == 0) {

                    mTvDisclaimer.setText("No readings. Turn OFF Thermometer.");
                    mTvDistance.setText("-");
                    mTvTImeStamp.setText("-");

                } else {

                    HashMap<String, Object> temperatureItem = vitalData.get(vitalData.size() - 1);

                    Log.d(TAG, "Temperature data " + temperatureItem.toString());

                    mTvDisclaimer.setText("Turn OFF Thermometer.");

                    if (temperatureItem.get(OmronConstants.OMRONTemperatureData.TemperatureLevelKey) != null) {
                        if ((int)temperatureItem.get(OmronConstants.OMRONTemperatureData.TemperatureLevelKey) == OmronConstants.OMRONTemperatureLevelTypeKey.High) {
                            mTvTemperature.setText("Temperature High. Record again.");
                        }else if ((int)temperatureItem.get(OmronConstants.OMRONTemperatureData.TemperatureLevelKey) == OmronConstants.OMRONTemperatureLevelTypeKey.Low) {
                            mTvTemperature.setText("Temperature Low. Record again.");
                        }
                    }else {
                        int unit = (int) temperatureItem.get(OmronConstants.OMRONTemperatureData.TemperatureUnitKey);
                        String unitString = unit == OmronConstants.OMRONTemperatureUnitTypeKey.Celsius ? "°C" : "°F";
                        mTvTemperature.setText(String.valueOf(temperatureItem.get(OmronConstants.OMRONTemperatureData.TemperatureKey)) + "\t " + unitString);
                    }

                    SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis((Long) temperatureItem.get(OmronConstants.OMRONTemperatureData.StartDateKey));
                    mTvTImeStamp.setText(format.format(calendar.getTime()));
                }
            }
        });

    }

    private void initViews() {


        mTvTImeStamp = (TextView) findViewById(R.id.tv_timestamp_value);
        mTvTemperature = (TextView) findViewById(R.id.tv_temperature_value);
        mTvDistance = (TextView) findViewById(R.id.tv_distance_value);
        mTvDisclaimer = (TextView) findViewById(R.id.tv_disclaimer_value);

        scanBtn = (Button) findViewById(R.id.btn_scan);
    }

    private void resetView() {

        mTvTImeStamp.setText("-");
        mTvTemperature.setText("-");
        mTvDistance.setText("-");
        mTvDisclaimer.setText("Turn ON Thermometer.\n Begin transferring temperature reading by placing Thermometer near microphone of smartphone.");
    }

    private void initClickListeners() {

        // To perform scan process.
        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                startScan();
            }
        });

    }

}