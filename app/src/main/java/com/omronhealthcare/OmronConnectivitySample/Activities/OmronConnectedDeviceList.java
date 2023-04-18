package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.LibraryManager.OmronPeripheralManager;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.App;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.adapter.ConnectedDeviceAdapter;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;
import com.omronhealthcare.OmronConnectivitySample.utility.PreferencesManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Omron HealthCare Inc
 */
public class OmronConnectedDeviceList extends BaseActivity {

    final String TAG = "DeviceList";

    private ListView mListView;
    ConnectedDeviceAdapter mConnectedDeviceAdapter;
    private Context mContext;
    List<HashMap<String, String>> fullDeviceList;

    private PreferencesManager preferencesManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_omron_connected_devices);

        mContext = this;
        fullDeviceList = new ArrayList<HashMap<String, String>>();

        if (preferencesManager == null)
            preferencesManager = new PreferencesManager(App.AppContext);

        initViews();
        initClickListeners();

        if (preferencesManager.getPartnerKey().isEmpty()) {
            showLibraryKeyDialog();
        } else {
            reloadConfiguration();
        }

        if(Build.VERSION.SDK_INT < 31){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION
                                , Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        100);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN
                                , Manifest.permission.BLUETOOTH_CONNECT
                        },
                        100);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted,
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                    }

                } else {

                }
                break;
            }
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (mMessageReceiver != null)
                LocalBroadcastManager.getInstance(OmronConnectedDeviceList.this).unregisterReceiver(mMessageReceiver);

            // Get extra data included in the Intent
            final int status = intent.getIntExtra(OmronConstants.OMRONConfigurationStatusKey, 0);

            if (status == OmronConstants.OMRONConfigurationStatus.OMRONConfigurationFileSuccess) {
                Log.d(TAG, "Config File Extract Success");
                loadDeviceList();
            } else if (status == OmronConstants.OMRONConfigurationStatus.OMRONConfigurationFileError) {
                Log.d(TAG, "Config File Extract Failure");
                showErrorLoadingDevices();
            } else if (status == OmronConstants.OMRONConfigurationStatus.OMRONConfigurationFileUpdateError) {
                Log.d(TAG, "Config File Update Failure");
                showErrorLoadingDevices();
            }
        }
    };

    // UI initializers

    private void initViews() {

        mListView = (ListView) findViewById(R.id.lv_devicelist);
    }

    private void initClickListeners() {

        findViewById(R.id.iv_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showLibraryKeyDialog();
            }
        });

        findViewById(R.id.iv_vital_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent toVitalData = new Intent(OmronConnectedDeviceList.this, DataDeviceListingActivity.class);
                startActivity(toVitalData);
            }
        });
    }

    /**
     * Load devices
     */
    private void loadDeviceList() {

        fullDeviceList = new ArrayList<HashMap<String, String>>();
        Context ctx = App.getInstance().getApplicationContext();

        if (OmronPeripheralManager.sharedManager(ctx).retrieveManagerConfiguration(ctx) != null) {
            fullDeviceList = (List<HashMap<String, String>>) OmronPeripheralManager.sharedManager(ctx).retrieveManagerConfiguration(ctx).get(OmronConstants.OMRONBLEConfigDeviceKey);

            mConnectedDeviceAdapter = new ConnectedDeviceAdapter(mContext, fullDeviceList);
            mListView.setAdapter(mConnectedDeviceAdapter);
            mListView.setDivider(null);

            mConnectedDeviceAdapter.notifyDataSetChanged();
        }

        showErrorLoadingDevices();
    }

    /**
     * Device selected and screen navigation
     *
     * @param item - Device information
     */
    public void selectDevice(HashMap<String, String> item) {

        if (Integer.parseInt(item.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.BLOODPRESSURE) {

            int noOfUsers = Integer.parseInt(item.get(OmronConstants.OMRONBLEConfigDevice.Users));
            // Category 0 is for Blood Pressure Devices
            if (noOfUsers > 1) {

                Intent toMain = new Intent(OmronConnectedDeviceList.this, SelectUserActivity.class);
                toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, item);
                startActivity(toMain);

            } else if (noOfUsers == 1) {

                Intent toMain = new Intent(OmronConnectedDeviceList.this, MainActivity.class);
                toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, item);

                startActivity(toMain);
            }

        } else if (Integer.parseInt(item.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {

            // Category 2 is for Activity Trackers
            Intent toMain = new Intent(OmronConnectedDeviceList.this, SelectUserActivity.class);
            toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, item);
            startActivity(toMain);

        } else if (Integer.parseInt(item.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.BODYCOMPOSITION) {

            int noOfUsers = Integer.parseInt(item.get(OmronConstants.OMRONBLEConfigDevice.Users));
            if (noOfUsers > 1) {

                Intent toMain = new Intent(OmronConnectedDeviceList.this, SelectUserActivity.class);
                toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, item);
                startActivity(toMain);

            } else if (noOfUsers == 1) {

                Intent toMain = new Intent(OmronConnectedDeviceList.this, UserPersonalSettingsActivity.class);
                toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, item);

                startActivity(toMain);
            }

        } else if (Integer.parseInt(item.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.WHEEZE) {

            Intent toMain = new Intent(OmronConnectedDeviceList.this, MainActivity.class);
            toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, item);

            startActivity(toMain);

        } else if (Integer.parseInt(item.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.TEMPERATURE) {

            Intent toMain = new Intent(OmronConnectedDeviceList.this, TemperatureRecordingActivity.class);
            toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, item);

            startActivity(toMain);

        } else if (Integer.parseInt(item.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.PULSEOXIMETER) {

            Intent toMain = new Intent(OmronConnectedDeviceList.this, PulseOxymeterMainActivity.class);
            toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, item);

            startActivity(toMain);
        }
    }

    public void showLibraryKeyDialog() {

        Context ctx = App.getInstance().getApplicationContext();
        String libraryVersion = OmronPeripheralManager.sharedManager(ctx).libVersion();
        PackageManager manager = OmronConnectedDeviceList.this.getPackageManager();
        try {

            PackageInfo info = manager.getPackageInfo(OmronConnectedDeviceList.this.getPackageName(), 0);
            String message = "\nSample App version : " + info.versionName + " (" + info.versionCode + ")\nLibrary Version : " + libraryVersion + "\n\nTap ⓘ to configure later\n";

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
            alertDialogBuilder.setTitle("Configure OMRON Library Key");
            alertDialogBuilder.setMessage(message);

            // Set up the input
            final EditText inputField = new EditText(mContext);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            inputField.setInputType(InputType.TYPE_CLASS_TEXT);
            inputField.setHint("Enter API Key");
            alertDialogBuilder.setView(inputField);
            // Set up the buttons
            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferencesManager.savePartnerKey(inputField.getText().toString());
                    reloadConfiguration();
                }
            });
            alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    loadDeviceList();
                }
            });

            alertDialogBuilder.show();


        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void showDeviceInfo(HashMap<String, String> item) {

        // TODO: Navigate to new screen for device details
        String deviceInfo = "Category : " + item.get(OmronConstants.OMRONBLEConfigDevice.GroupID) + "\nModel Type : " + item.get(OmronConstants.OMRONBLEConfigDevice.GroupIncludedGroupID);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Info");

        alertDialogBuilder.setMessage(deviceInfo);
        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showErrorLoadingDevices() {

        if (fullDeviceList.size() == 0) {
            String information = "Invalid Library API key configured\nOR\nNo devices supported for API Key\n\nPlease try again using ⓘ button. ";
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
            alertDialogBuilder.setTitle("Info");

            alertDialogBuilder.setMessage(information);
            alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {

                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    private void reloadConfiguration() {

        if (!preferencesManager.getPartnerKey().isEmpty()) {
            // OmronConnectivityLibrary initialization and Api key setup.
            OmronPeripheralManager.sharedManager(this).setAPIKey(preferencesManager.getPartnerKey(), null);

            // Notification Listener for Configuration Availability
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                    new IntentFilter(OmronConstants.OMRONBLEConfigDeviceAvailabilityNotification));
        } else {
            showLibraryKeyDialog();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:

                    return false;

                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
