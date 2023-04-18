package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.DeviceConfiguration.OmronPeripheralManagerConfig;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerConnectListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerConnectStateListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerDataTransferListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerDisconnectListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerScanListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerStopScanListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerUpdateListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.LibraryManager.OmronPeripheralManager;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronErrorInfo;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronPeripheral;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.App;
import com.omronhealthcare.OmronConnectivitySample.Database.OmronDBConstans;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.adapter.ScannedDevicesAdapter;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;
import com.omronhealthcare.OmronConnectivitySample.utility.PreferencesManager;
import com.omronhealthcare.OmronConnectivitySample.utility.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Main DashBoard activity, where perform the pairing and data transfer.
 */

public class MainActivity extends BaseActivity {

    private Context mContext;
    private String TAG = "OmronSampleApp";

    private ListView mLvScannedList;
    private ArrayList<OmronPeripheral> mPeripheralList;
    private ScannedDevicesAdapter mScannedDevicesAdapter;
    private OmronPeripheral mSelectedPeripheral;

    private ArrayList<Integer> selectedUsers = new ArrayList<>();

    private RelativeLayout mRlDeviceListView, mRlTransferView;
    private TextView mTvTImeStamp, mTvSystolic, mTvDiastolic, mTvPulseRate, mTvUserSelected;
    private TextView mTvDeviceInfo, mTvDeviceLocalName, mTvDeviceUuid, mTvStatusLabel, mTvErrorCode, mTvErrorDesc;
    private ProgressBar mProgressBar;

    private Button scanBtn;
    private Button transferBtn;

    private PreferencesManager preferencesManager = null;

    HashMap<String, String> device = null;
    HashMap<String, String> personalSettings = null;

    private Boolean isScan;

    private final int TIME_INTERVAL = 1000;

    Handler mHandler;
    Runnable mRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        isScan = false;

        if (preferencesManager == null)
            preferencesManager = new PreferencesManager(MainActivity.this);

        // Selected users
        selectedUsers = (ArrayList<Integer>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_USER);
        if(selectedUsers == null) {
            selectedUsers = new ArrayList<>();
            selectedUsers.add(1);
        }
        // Selected device
        device = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_DEVICE);

        //Personal settings like height, weight etc for activity devices.
        personalSettings = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_PERSONAL_SETTINGS);

        initViews();
        showDeviceListView();
        initClickListeners();
        initLists();

        // Permissions for HeartGuide devices
        requestPermissions();

        // Start OmronPeripheralManager
        startOmronPeripheralManager(false, true);
    }

    @Override
    protected void onPause() {

        super.onPause();

        enableDisableButton(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Activity Tracker - Testing of notification
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {
            Utilities.scheduleNotification(Utilities.getNotification("Test Notification"), 5000);
        }
    }

    /**
     * Permissions for activity device
     */
    private void requestPermissions() {

        // Activity Tracker
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                        1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        if (grantResults == null || grantResults.length <= 0) {
            return;
        }
        switch (requestCode) {
            case 1:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},
                            2);
                }
                break;
            case 2:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                            3);
                }
                break;
            case 3:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.READ_SMS},
                            4);
                }
                break;
            case 4:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS},
                            5);
                }
                break;
        }
    }

    /**
     * Configure library functionalities
     */
    private void startOmronPeripheralManager(boolean isHistoricDataRead, boolean isPairing) {

        OmronPeripheralManagerConfig peripheralConfig = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getConfiguration();
        Log.d(TAG, "Library Identifier : " + peripheralConfig.getLibraryIdentifier());

        // Filter device to scan and connect (optional)
        if (device != null && device.get(OmronConstants.OMRONBLEConfigDevice.GroupID) != null && device.get(OmronConstants.OMRONBLEConfigDevice.GroupIncludedGroupID) != null) {

            // Add item
            List<HashMap<String, String>> filterDevices = new ArrayList<>();
            filterDevices.add(device);
            peripheralConfig.deviceFilters = filterDevices;
        }

        ArrayList<HashMap> deviceSettings = new ArrayList<>();

        // Blood pressure settings (optional)
        deviceSettings = getBloodPressureSettings(deviceSettings, isPairing);

        // Activity device settings (optional)
        deviceSettings = getActivitySettings(deviceSettings);

        // BCM device settings (optional)
        deviceSettings = getBCMSettings(deviceSettings);

        peripheralConfig.deviceSettings = deviceSettings;

        // Set Scan timeout interval (optional)
        peripheralConfig.timeoutInterval = Constants.CONNECTION_TIMEOUT;
        // Set User Hash Id (mandatory)
        peripheralConfig.userHashId = "<email_address_of_user>"; // Set logged in user email

        // Disclaimer: Read definition before usage
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) != OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {
            // Reads all data from device.
            peripheralConfig.enableAllDataRead = isHistoricDataRead;
        }

        // Pass the last sequence number of reading  tracked by app - "SequenceKey" for each vital data
        HashMap<Integer, Integer> sequenceNumbersForTransfer = new HashMap<>();
        sequenceNumbersForTransfer.put(1, 42);
        sequenceNumbersForTransfer.put(2, 8);
        peripheralConfig.sequenceNumbersForTransfer = sequenceNumbersForTransfer;

        // Set configuration for OmronPeripheralManager
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).setConfiguration(peripheralConfig);

        //Initialize the connection process.
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).startManager();

        // Notification Listener for BLE State Change
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(OmronConstants.OMRONBLECentralManagerDidUpdateStateNotification));
    }

    private void startScanning() {

        // Start OmronPeripheralManager
        startOmronPeripheralManager(false, true);

        // Set State Change Listener
        setStateChanges();

        if (isScan) {

            scanBtn.setText("SCAN");

            // Stop Scanning for Devices using OmronPeripheralManager
            OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).stopScanPeripherals(new OmronPeripheralManagerStopScanListener() {
                @Override
                public void onStopScanCompleted(final OmronErrorInfo resultInfo) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (resultInfo.getResultCode() == 0) {

                                resetDeviceList();

                            } else {

                                Toast.makeText(MainActivity.this, "Error Code : " + resultInfo.getResultCode() + "\nError Detail Code : " + resultInfo.getDetailInfo(), Toast.LENGTH_LONG).show();
                            }

                            enableDisableButton(true);
                        }
                    });
                }
            });

        } else {

            resetVitalDataResult();
            showDeviceListView();
            resetErrorMessage();

            mProgressBar.setVisibility(View.VISIBLE);
            mTvDeviceLocalName.setText("");
            mTvDeviceUuid.setText("");

            scanBtn.setText("STOP SCAN");

            // Start Scanning for Devices using OmronPeripheralManager
            OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).startScanPeripherals(new OmronPeripheralManagerScanListener() {

                @Override
                public void onScanCompleted(final ArrayList<OmronPeripheral> peripheralList, final OmronErrorInfo resultInfo) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (resultInfo.getResultCode() == 0) {

                                mPeripheralList = peripheralList;

                                if (mScannedDevicesAdapter != null) {
                                    mScannedDevicesAdapter.setPeripheralList(mPeripheralList);
                                    mScannedDevicesAdapter.notifyDataSetChanged();
                                }

                            } else {

                                isScan = !isScan;

                                scanBtn.setText("SCAN");

                                showTransferView();
                                resetErrorMessage();
                                enableDisableButton(true);
                                resetDeviceList();


                                mTvErrorCode.setText(resultInfo.getResultCode() + " / " + resultInfo.getDetailInfo());
                                mTvErrorDesc.setText(resultInfo.getMessageInfo());
                            }

                            enableDisableButton(true);
                        }
                    });
                }
            });
        }

        isScan = !isScan;
    }


    private void stopOmronPeripheralManager() {

        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).stopManager();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Get extra data included in the Intent
            int status = intent.getIntExtra(OmronConstants.OMRONBLEBluetoothStateKey, 0);

            if (status == OmronConstants.OMRONBLEBluetoothState.OMRONBLEBluetoothStateUnknown) {

                Log.d(TAG, "Bluetooth is in unknown state");

            } else if (status == OmronConstants.OMRONBLEBluetoothState.OMRONBLEBluetoothStateOff) {

                Log.d(TAG, "Bluetooth is currently powered off");

            } else if (status == OmronConstants.OMRONBLEBluetoothState.OMRONBLEBluetoothStateOn) {

                Log.d(TAG, "Bluetooth is currently powered on");
            }
        }
    };

    private void setStateChanges() {

        // Listen to Device state changes using OmronPeripheralManager
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).onConnectStateChange(new OmronPeripheralManagerConnectStateListener() {

            @Override
            public void onConnectStateChange(final int state) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        String status = "-";

                        if (state == OmronConstants.OMRONBLEConnectionState.CONNECTING) {
                            status = "Connecting...";
                        } else if (state == OmronConstants.OMRONBLEConnectionState.CONNECTED) {
                            status = "Connected";
                        } else if (state == OmronConstants.OMRONBLEConnectionState.DISCONNECTING) {
                            status = "Disconnecting...";
                        } else if (state == OmronConstants.OMRONBLEConnectionState.DISCONNECTED) {
                            status = "Disconnected";
                        }
                        setStatus(status);
                    }
                });
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:

                    // Stop Scanning for Devices using OmronPeripheralManager
                    OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).stopScanPeripherals(new OmronPeripheralManagerStopScanListener() {
                        @Override
                        public void onStopScanCompleted(final OmronErrorInfo resultInfo) {

                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resetDeviceList();
                                }
                            });
                        }
                    });
                    break;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }


    /**
     * Method to connect to the device
     *
     * @param omronPeripheral
     */
    private void connectPeripheral(final OmronPeripheral omronPeripheral) {

        initLists();

        isScan = false;

        mSelectedPeripheral = omronPeripheral;

        scanBtn.setText("SCAN");

        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {

                              showTransferView();
                              resetErrorMessage();
                              resetVitalDataResult();
                              enableDisableButton(false);

                              setStatus("Connecting...");

                              // Pair to Device using OmronPeripheralManager
                              OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).connectPeripheral(omronPeripheral, new OmronPeripheralManagerConnectListener() {

                                          @Override
                                          public void onConnectCompleted(final OmronPeripheral peripheral, final OmronErrorInfo resultInfo) {

                                              connectionUpdateWithPeripheral(peripheral, resultInfo, false);
                                          }
                                      }
                              );
                          }
                      }
        );
    }

    /**
     * Method  to connect to device. Method has option to have few seconds wait to select the user
     *
     * @param omronPeripheral
     */
    private void connectPeripheralWithWait(final OmronPeripheral omronPeripheral) {

        initLists();

        isScan = false;

        mSelectedPeripheral = omronPeripheral;

        scanBtn.setText("SCAN");
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {

                              showTransferView();
                              resetErrorMessage();
                              resetVitalDataResult();
                              enableDisableButton(false);

                              setStatus("Connecting...");

                              // Pair to Device using OmronPeripheralManager
                              OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).connectPeripheral(omronPeripheral, true, new OmronPeripheralManagerConnectListener() {

                                          @Override
                                          public void onConnectCompleted(final OmronPeripheral peripheral, final OmronErrorInfo resultInfo) {

                                              connectionUpdateWithPeripheral(peripheral, resultInfo, true);
                                          }
                                      }
                              );
                          }
                      }
        );
    }

    /**
     * Method to resume the connection after the wait is over
     *
     * @param omronPeripheral
     */
    private void resumeConnection(final OmronPeripheral omronPeripheral) {

        if (selectedUsers.size() > 1) {
            OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).resumeConnectPeripheral(
                    omronPeripheral, selectedUsers, new OmronPeripheralManagerConnectListener() {
                        @Override
                        public void onConnectCompleted(final OmronPeripheral peripheral, final OmronErrorInfo resultInfo) {

                            connectionUpdateWithPeripheral(peripheral, resultInfo, false);
                        }

                    });
        } else {

            OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).resumeConnectPeripheral(
                    omronPeripheral, new ArrayList<>(Arrays.asList(selectedUsers.get(0))), new OmronPeripheralManagerConnectListener() {
                        @Override
                        public void onConnectCompleted(final OmronPeripheral peripheral, final OmronErrorInfo resultInfo) {

                            connectionUpdateWithPeripheral(peripheral, resultInfo, false);
                        }

                    });
        }
    }

    private void connectionUpdateWithPeripheral(final OmronPeripheral peripheral, final OmronErrorInfo resultInfo, final boolean wait) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (resultInfo.getResultCode() == 0 && peripheral != null) {

                    mSelectedPeripheral = peripheral;

                    if (null != peripheral.getLocalName()) {

                        mTvDeviceLocalName.setText(peripheral.getLocalName());
                        mTvDeviceUuid.setText(peripheral.getUuid());

                        HashMap<String, String> deviceInformation = peripheral.getDeviceInformation();
                        Log.d(TAG, "Device Information : " + deviceInformation);

                        ArrayList<HashMap> deviceSettings = mSelectedPeripheral.getDeviceSettings();
                        if (deviceSettings != null) {
                            Log.d(TAG, "Device Settings:" + deviceSettings.toString());
                        }

                        OmronPeripheralManagerConfig peripheralConfig = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getConfiguration();
                        Log.d(TAG, "Device Config :  " + peripheralConfig.getDeviceConfigGroupIdAndGroupIncludedId(peripheral.getDeviceGroupIDKey(), peripheral.getDeviceGroupIncludedGroupIDKey()));

                        if (wait) {
                            mHandler = new Handler();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    resumeConnection(peripheral);
                                }
                            }, 5000);
                        } else {

                            if(peripheral.getVitalData() != null) {
                                Log.d(TAG, "Vital data - " + peripheral.getVitalData().toString());
                            }

                            showMessage(getString(R.string.device_connected), getString(R.string.device_paired));
                        }
                    }
                } else {

                    setStatus("-");
                    mTvErrorCode.setText(resultInfo.getDetailInfo());
                    mTvErrorDesc.setText(resultInfo.getMessageInfo());
                }

                enableDisableButton(true);
            }
        });
    }

    /**
     * Method to end the connection with device
     */
    private void endConnection() {
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).endConnectPeripheral(
                new OmronPeripheralManagerConnectListener() {
                    @Override
                    public void onConnectCompleted(final OmronPeripheral peripheral,
                                                   final OmronErrorInfo resultInfo) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (resultInfo.getResultCode() == 0 && peripheral != null) {

                                    mSelectedPeripheral = peripheral;

                                    if (null != peripheral.getLocalName()) {
                                        mTvDeviceLocalName.setText(peripheral.getLocalName());
                                        mTvDeviceUuid.setText(peripheral.getUuid());
                                        showMessage(getString(R.string.device_connected), getString(R.string.device_paired));
                                    }
                                } else {

                                    setStatus("-");
                                    mTvErrorCode.setText(resultInfo.getDetailInfo());
                                    mTvErrorDesc.setText(resultInfo.getMessageInfo());
                                }

                                enableDisableButton(true);
                            }
                        });

                    }

                });

    }

    private void disconnectDevice() {

        // Disconnect device using OmronPeripheralManager
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).disconnectPeripheral(mSelectedPeripheral, new OmronPeripheralManagerDisconnectListener() {
            @Override
            public void onDisconnectCompleted(OmronPeripheral peripheral, OmronErrorInfo resultInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show();

                        enableDisableButton(true);
                    }
                });
            }
        });
    }

    /*
        OmronPeripheralManager Transfer Function
     */

    private void transferData() {

        resetErrorMessage();
        enableDisableButton(false);
        resetVitalDataResult();

        if (mSelectedPeripheral == null) {
            mTvErrorDesc.setText("Device Not Paired");
            return;
        }

        // Disclaimer: Read definition before usage
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY || Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.PULSEOXIMETER) {
            startOmronPeripheralManager(false, false);
            performDataTransfer();
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
            alertDialogBuilder.setTitle("Transfer");
            alertDialogBuilder.setMessage("Do you want to transfer all historic readings from device?");
            alertDialogBuilder.setPositiveButton("No",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                            startOmronPeripheralManager(false, false);
                            performDataTransfer();
                        }
                    });
            alertDialogBuilder.setNegativeButton("Yes",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            startOmronPeripheralManager(true, false);
                            performDataTransfer();
                        }
                    });
            alertDialogBuilder.setCancelable(true);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    private void performDataTransfer() {

        // Set State Change Listener
        setStateChanges();

        //Create peripheral object with localname and UUID
        OmronPeripheral peripheralLocal = new OmronPeripheral(mSelectedPeripheral.getLocalName(), mSelectedPeripheral.getUuid());

        if (selectedUsers.size() > 1) {
            transferUsersDataWithPeripheral(peripheralLocal);
        } else {
            transferUserDataWithPeripheral(peripheralLocal);
        }
    }

    // Single User data transfer
    private void transferUserDataWithPeripheral(OmronPeripheral peripheral) {

        // Data Transfer from Device using OmronPeripheralManager
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).startDataTransferFromPeripheral(peripheral, selectedUsers.get(0), true, OmronConstants.OMRONVitalDataTransferCategory.BloodPressure, new OmronPeripheralManagerDataTransferListener() {
            @Override
            public void onDataTransferCompleted(OmronPeripheral peripheral, final OmronErrorInfo resultInfo) {

                if (resultInfo.getResultCode() == 0 && peripheral != null) {

                    HashMap<String, String> deviceInformation = peripheral.getDeviceInformation();
                    Log.d(TAG, "Device Information : " + deviceInformation);

                    ArrayList<HashMap> allSettings = (ArrayList<HashMap>) peripheral.getDeviceSettings();
                    Log.i(TAG, "Device settings : " + allSettings.toString());

                    mSelectedPeripheral = peripheral; // Saving for Transfer Function

                    // Save Device to List
                    // To change based on data available
                    preferencesManager.addDataStoredDeviceList(peripheral.getLocalName(), Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)), peripheral.getModelName());

                    // Get vital data for previously selected user using OmronPeripheral
                    Object output = peripheral.getVitalData();

                    if (output instanceof OmronErrorInfo) {

                        final OmronErrorInfo errorInfo = (OmronErrorInfo) output;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                mTvErrorCode.setText(errorInfo.getResultCode() + " / " + errorInfo.getDetailInfo());
                                mTvErrorDesc.setText(errorInfo.getMessageInfo());

                                enableDisableButton(true);

                            }
                        });

                        disconnectDevice();

                    } else {

                        HashMap<String, Object> vitalData = (HashMap<String, Object>) output;

                        if (vitalData != null) {
                            uploadData(vitalData, peripheral, true);
                        }
                    }

                } else {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            setStatus("-");
                            mTvErrorCode.setText(resultInfo.getResultCode() + " / " + resultInfo.getDetailInfo());
                            mTvErrorDesc.setText(resultInfo.getMessageInfo());

                            if (mHandler != null)
                                mHandler.removeCallbacks(mRunnable);

                            enableDisableButton(true);
                        }
                    });
                }
            }
        });
    }

    // Data transfer with multiple users
    private void transferUsersDataWithPeripheral(OmronPeripheral peripheral) {

        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).startDataTransferFromPeripheral(peripheral, selectedUsers, true, new OmronPeripheralManagerDataTransferListener() {
            @Override
            public void onDataTransferCompleted(OmronPeripheral peripheral, final OmronErrorInfo resultInfo) {

                if (resultInfo.getResultCode() == 0 && peripheral != null) {

                    HashMap<String, String> deviceInformation = peripheral.getDeviceInformation();
                    Log.d(TAG, "Device Information : " + deviceInformation);

                    ArrayList<HashMap> allSettings = (ArrayList<HashMap>) peripheral.getDeviceSettings();
                    Log.i(TAG, "Device settings : " + allSettings.toString());

                    mSelectedPeripheral = peripheral; // Saving for Transfer Function

                    // Save Device to List
                    // To change based on data available
                    preferencesManager.addDataStoredDeviceList(peripheral.getLocalName(), Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)), peripheral.getModelName());

                    // Get vital data for previously selected user using OmronPeripheral
                    Object output = peripheral.getVitalData();

                    if (output instanceof OmronErrorInfo) {

                        final OmronErrorInfo errorInfo = (OmronErrorInfo) output;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                mTvErrorCode.setText(errorInfo.getResultCode() + " / " + errorInfo.getDetailInfo());
                                mTvErrorDesc.setText(errorInfo.getMessageInfo());

                                enableDisableButton(true);

                            }
                        });

                        disconnectDevice();

                    } else {

                        HashMap<String, Object> vitalData = (HashMap<String, Object>) output;

                        if (vitalData != null) {
                            uploadData(vitalData, peripheral, true);
                        }
                    }

                } else {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            setStatus("-");
                            mTvErrorCode.setText(resultInfo.getResultCode() + " / " + resultInfo.getDetailInfo());
                            mTvErrorDesc.setText(resultInfo.getMessageInfo());

                            if (mHandler != null)
                                mHandler.removeCallbacks(mRunnable);


                            enableDisableButton(true);
                        }
                    });
                }
            }

        });
    }

    // Vital Data Save

    private void uploadData(HashMap<String, Object> vitalData, OmronPeripheral peripheral, boolean isWait) {

        HashMap<String, String> deviceInfo = peripheral.getDeviceInformation();

        // Blood Pressure Data
        final ArrayList<HashMap<String, Object>> bloodPressureItemList = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataBloodPressureKey);
        if (bloodPressureItemList != null) {

            for (HashMap<String, Object> bpItem : bloodPressureItemList) {
                Log.d("Blood Pressure - ", bpItem.toString());
            }
            insertVitalDataToDB(bloodPressureItemList, deviceInfo);
        }

        // Activity Data
        ArrayList<HashMap<String, Object>> activityList = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataActivityKey);
        if (activityList != null) {

            for (HashMap<String, Object> activityItem : activityList) {

                List<String> list = new ArrayList<String>(activityItem.keySet());

                for (String key : list) {

                    Log.d("Activity key - ", key);
                    Log.d("Activity Data - ", activityItem.get(key).toString());

                    if(key.equalsIgnoreCase(OmronConstants.OMRONActivityData.AerobicStepsPerDay) || key.equalsIgnoreCase(OmronConstants.OMRONActivityData.StepsPerDay) || key.equalsIgnoreCase(OmronConstants.OMRONActivityData.DistancePerDay) || key.equalsIgnoreCase(OmronConstants.OMRONActivityData.WalkingCaloriesPerDay)) {
                        insertActivityToDB((HashMap<String, Object>) activityItem.get(key), deviceInfo, key);
                    }
                }
            }
        }

        // Sleep Data
        ArrayList<HashMap<String, Object>> sleepingData = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataSleepKey);
        if (sleepingData != null) {

            for (HashMap<String, Object> sleepitem : sleepingData) {
                Log.d("Sleep - ", sleepitem.toString());
            }
            insertSleepToDB(sleepingData, deviceInfo);
        }

        // Records Data
        ArrayList<HashMap<String, Object>> recordData = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataRecordKey);
        if (recordData != null) {

            for (HashMap<String, Object> recordItem : recordData) {
                Log.d("Record - ", recordItem.toString());
            }
            insertRecordToDB(recordData, deviceInfo);
        }

        // Weight Data
        ArrayList<HashMap<String, Object>> weightData = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataWeightKey);
        if (weightData != null) {

            for (HashMap<String, Object> weightItem : weightData) {
                Log.d("Weight - ", weightItem.toString());
            }
            insertRecordToDB(recordData, deviceInfo);
        }

        // Pulse oxximeter Data
        ArrayList<HashMap<String, Object>> pulseOximeterData = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataPulseOximeterKey);
        if (pulseOximeterData != null) {

            for (HashMap<String, Object> pulseOximeterItem : pulseOximeterData) {
                Log.d("Pulse Oximeter - ", pulseOximeterItem.toString());
            }
        }

        if (isWait) {

            mHandler = new Handler();
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    continueDataTransfer();
                }
            };

            mHandler.postDelayed(mRunnable, TIME_INTERVAL);

        } else {

            if (mHandler != null)
                mHandler.removeCallbacks(mRunnable);

            continueDataTransfer();
        }
    }

    private void continueDataTransfer() {

        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).endDataTransferFromPeripheral(new OmronPeripheralManagerDataTransferListener() {
            @Override
            public void onDataTransferCompleted(final OmronPeripheral peripheral, final OmronErrorInfo errorInfo) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        enableDisableButton(true);

                        if (errorInfo.getResultCode() == 0 && peripheral != null) {

                            HashMap<String, String> deviceInformation = peripheral.getDeviceInformation();
                            Log.d(TAG, "Device Information : " + deviceInformation);

                            ArrayList<HashMap> allSettings = (ArrayList<HashMap>) peripheral.getDeviceSettings();
                            Log.i(TAG, "Device settings : " + allSettings.toString());


                            // Get vital data for previously selected user using OmronPeripheral
                            Object output = peripheral.getVitalData();

                            if (output instanceof OmronErrorInfo) {

                                final OmronErrorInfo errorInfo = (OmronErrorInfo) output;

                                mTvErrorCode.setText(errorInfo.getResultCode() + " / " + errorInfo.getDetailInfo());
                                mTvErrorDesc.setText(errorInfo.getMessageInfo());

                            } else {

                                HashMap<String, Object> vitalData = (HashMap<String, Object>) output;

                                if (vitalData != null) {

                                    // Blood Pressure Data
                                    final ArrayList<HashMap<String, Object>> bloodPressureItemList = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataBloodPressureKey);
                                    if (bloodPressureItemList != null) {

                                        showVitalDataResult(bloodPressureItemList);
                                    }
                                }
                            }

                        } else {

                            setStatus("-");
                            mTvErrorCode.setText(errorInfo.getResultCode() + " / " + errorInfo.getDetailInfo());
                            mTvErrorDesc.setText(errorInfo.getMessageInfo());

                        }
                    }
                });
            }
        });
    }

    private void insertVitalDataToDB(ArrayList<HashMap<String, Object>> dataList, HashMap<String, String> deviceInfo) {

        for (HashMap<String, Object> bloodPressureItem : dataList) {

            ContentValues cv = new ContentValues();
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataArtifactDetectionKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.ArtifactDetectionKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataCuffFlagKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.CuffFlagKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataDiastolicKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.DiastolicKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataIHBDetectionKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.IHBDetectionKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataIrregularFlagKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.IrregularFlagKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataMeasurementDateKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.DateKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataMovementFlagKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.MovementFlagKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataPulseKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.PulseKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataSystolicKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.SystolicKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataMeasurementDateUTCKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.StartDateKey)));

            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataAtrialFibrillationDetectionFlagKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.AtrialFibrillationDetectionFlagKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataDisplayedErrorCodeNightModeKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.DisplayedErrorCodeNightModeKey)));
            cv.put(OmronDBConstans.VITAL_DATA_OMRONVitalDataMeasurementModeKey, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.MeasurementModeKey)));

            cv.put(OmronDBConstans.DEVICE_SELECTED_USER, String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.UserIdKey)));
            cv.put(OmronDBConstans.DEVICE_LOCAL_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.LocalNameKey).toLowerCase());
            cv.put(OmronDBConstans.DEVICE_DISPLAY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.DisplayNameKey));
            cv.put(OmronDBConstans.DEVICE_IDENTITY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.IdentityNameKey));
            cv.put(OmronDBConstans.DEVICE_CATEGORY, device.get(OmronConstants.OMRONBLEConfigDevice.Category));

            Uri uri = getContentResolver().insert(OmronDBConstans.VITAL_DATA_CONTENT_URI, cv);
            if (uri != null) {
                //TODO successful insert
            }
        }
    }

    /*******************************************************************************************/
    /************************ Section for Activity Device / HeartVue **************************/
    /*******************************************************************************************/

    /**
     * Insert Activity data
     */
    private void insertActivityToDB(HashMap<String, Object> stepData, HashMap<String, String> deviceInfo, String type) {

        ContentValues cv = new ContentValues();

        cv.put(OmronDBConstans.ACTIVITY_DATA_StartDateUTCKey, String.valueOf(stepData.get(OmronConstants.OMRONActivityData.StartDateKey)));
        cv.put(OmronDBConstans.ACTIVITY_DATA_EndDateUTCKey, String.valueOf(stepData.get(OmronConstants.OMRONActivityData.EndDateKey)));
        cv.put(OmronDBConstans.ACTIVITY_DATA_MeasurementValueKey, String.valueOf(stepData.get(OmronConstants.OMRONActivityData.MeasurementKey)));
        cv.put(OmronDBConstans.ACTIVITY_DATA_SeqNumKey, String.valueOf(stepData.get(OmronConstants.OMRONActivityData.SequenceKey)));
        cv.put(OmronDBConstans.ACTIVITY_DATA_Type, type);

        cv.put(OmronDBConstans.DEVICE_LOCAL_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.LocalNameKey).toLowerCase());
        cv.put(OmronDBConstans.DEVICE_DISPLAY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.DisplayNameKey));
        cv.put(OmronDBConstans.DEVICE_IDENTITY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.IdentityNameKey));
        cv.put(OmronDBConstans.DEVICE_CATEGORY, device.get(OmronConstants.OMRONBLEConfigDevice.Category));

        Uri uri = getContentResolver().insert(OmronDBConstans.ACTIVITY_DATA_CONTENT_URI, cv);
        if (uri != null) {

            ArrayList<HashMap<String, Object>> individualData = (ArrayList<HashMap<String, Object>>) stepData.get(OmronConstants.OMRONActivityData.DividedDataKey);
            if (individualData != null) {
                for (HashMap<String, Object> activityIndividual : individualData) {

                    ContentValues dividedCV = new ContentValues();

                    dividedCV.put(OmronDBConstans.ACTIVITY_DIVIDED_DATA_MainStartDateUTCKey, String.valueOf(stepData.get(OmronConstants.OMRONActivityData.StartDateKey)));
                    dividedCV.put(OmronDBConstans.ACTIVITY_DIVIDED_DATA_StartDateUTCKey, String.valueOf(activityIndividual.get(OmronConstants.OMRONActivityData.DividedDataStartDateKey)));
                    dividedCV.put(OmronDBConstans.ACTIVITY_DIVIDED_DATA_StartDateUTCKey, String.valueOf(activityIndividual.get(OmronConstants.OMRONActivityData.DividedDataStartDateKey)));
                    dividedCV.put(OmronDBConstans.ACTIVITY_DIVIDED_DATA_MeasurementValueKey, String.valueOf(activityIndividual.get(OmronConstants.OMRONActivityData.DividedDataMeasurementKey)));
                    dividedCV.put(OmronDBConstans.ACTIVITY_DIVIDED_DATA_SeqNumKey, String.valueOf(stepData.get(OmronConstants.OMRONActivityData.SequenceKey)));
                    dividedCV.put(OmronDBConstans.ACTIVITY_DIVIDED_DATA_Type, type);
                    dividedCV.put(OmronDBConstans.DEVICE_LOCAL_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.LocalNameKey).toLowerCase());

                    Uri uriDivided = getContentResolver().insert(OmronDBConstans.ACTIVITY_DIVIDED_DATA_CONTENT_URI, dividedCV);

                    if (uriDivided != null) {

                    }
                }
            }
        }
    }

    /**
     * Insert sleep data
     */
    private void insertSleepToDB(ArrayList<HashMap<String, Object>> dataList, HashMap<String, String> deviceInfo) {

        for (HashMap<String, Object> sleepingDataItem : dataList) {

            ContentValues cv = new ContentValues();

            cv.put(OmronDBConstans.SLEEP_DATA_SleepStartTimeKey, String.valueOf(sleepingDataItem.get(OmronConstants.OMRONSleepData.TimeInBedKey)));
            cv.put(OmronDBConstans.SLEEP_DATA_SleepOnSetTimeKey, String.valueOf(sleepingDataItem.get(OmronConstants.OMRONSleepData.SleepOnsetTimeKey)));
            cv.put(OmronDBConstans.SLEEP_DATA_WakeUpTimeKey, String.valueOf(sleepingDataItem.get(OmronConstants.OMRONSleepData.WakeTimeKey)));
            cv.put(OmronDBConstans.SLEEP_DATA_SleepingTimeKey, String.valueOf(sleepingDataItem.get(OmronConstants.OMRONSleepData.TotalSleepTimeKey)));
            cv.put(OmronDBConstans.SLEEP_DATA_SleepEfficiencyKey, String.valueOf(sleepingDataItem.get(OmronConstants.OMRONSleepData.SleepEfficiencyKey)));
            cv.put(OmronDBConstans.SLEEP_DATA_SleepArousalTimeKey, String.valueOf(sleepingDataItem.get(OmronConstants.OMRONSleepData.ArousalDuringSleepTimeKey)));
            cv.put(OmronDBConstans.SLEEP_DATA_SleepBodyMovementKey, sleepingDataItem.get(OmronConstants.OMRONSleepData.BodyMotionLevelKey).toString());

            cv.put(OmronDBConstans.SLEEP_DATA_StartDateUTCKey, String.valueOf(sleepingDataItem.get(OmronConstants.OMRONSleepData.StartDateKey)));
            cv.put(OmronDBConstans.SLEEP_DATA_StartEndDateUTCKey, String.valueOf(sleepingDataItem.get(OmronConstants.OMRONSleepData.EndDateKey)));


            cv.put(OmronDBConstans.DEVICE_LOCAL_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.LocalNameKey).toLowerCase());
            cv.put(OmronDBConstans.DEVICE_DISPLAY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.DisplayNameKey));
            cv.put(OmronDBConstans.DEVICE_IDENTITY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.IdentityNameKey));
            cv.put(OmronDBConstans.DEVICE_CATEGORY, device.get(OmronConstants.OMRONBLEConfigDevice.Category));

            Uri uri = getContentResolver().insert(OmronDBConstans.SLEEP_DATA_CONTENT_URI, cv);
            if (uri != null) {
                //TODO successful insert
            }
        }
    }

    /**
     * Insert record details
     */
    private void insertRecordToDB(ArrayList<HashMap<String, Object>> dataList, HashMap<String, String> deviceInfo) {

        for (HashMap<String, Object> recordDataItem : dataList) {

            ContentValues cv = new ContentValues();
            cv.put(OmronDBConstans.RECORD_DATA_StartDateUTCKey, String.valueOf(recordDataItem.get(OmronConstants.OMRONRecordData.DateKey)));

            cv.put(OmronDBConstans.DEVICE_LOCAL_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.LocalNameKey).toLowerCase());
            cv.put(OmronDBConstans.DEVICE_DISPLAY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.DisplayNameKey));
            cv.put(OmronDBConstans.DEVICE_IDENTITY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.IdentityNameKey));
            cv.put(OmronDBConstans.DEVICE_CATEGORY, device.get(OmronConstants.OMRONBLEConfigDevice.Category));

            Uri uri = getContentResolver().insert(OmronDBConstans.RECORD_DATA_CONTENT_URI, cv);
            if (uri != null) {
                //TODO successful insert
            }
        }
    }

    // Settings update for Connectivity library

    private void updateSettings() {

        if (mSelectedPeripheral == null) {
            mTvErrorDesc.setText("Device Not Paired");
            return;
        }

        resetErrorMessage();
        enableDisableButton(false);
        resetVitalDataResult();

        setStatus("Connecting...");


        OmronPeripheralManagerConfig peripheralConfig = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getConfiguration();

        // Filter device to scan and connect (optional)
        if (device != null && device.get(OmronConstants.OMRONBLEConfigDevice.GroupID) != null && device.get(OmronConstants.OMRONBLEConfigDevice.GroupIncludedGroupID) != null) {

            // Add item
            List<HashMap<String, String>> filterDevices = new ArrayList<>();
            filterDevices.add(device);
            peripheralConfig.deviceFilters = filterDevices;
        }


        HashMap<String, String> settingsModel = new HashMap<String, String>();
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserHeightKey, "17200");
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserWeightKey, "6500");
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserStrideKey, "1500");
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.TargetSleepKey, "60");
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.TargetStepsKey, "2000");

        HashMap<String, HashMap> userSettings = new HashMap<>();
        userSettings.put(OmronConstants.OMRONDevicePersonalSettingsKey, settingsModel);

        // Test Functions
        // Date Format
        HashMap<String, Object> dateFormatSettings = new HashMap<String, Object>();
        dateFormatSettings.put(OmronConstants.OMRONDeviceDateSettings.FormatKey, OmronConstants.OMRONDeviceDateFormat.DayMonth);
        HashMap<String, HashMap> dateSettings = new HashMap<>();
        dateSettings.put(OmronConstants.OMRONDeviceDateSettingsKey, dateFormatSettings);

        // Distance Unit Format
        HashMap<String, Object> dateUnitSettings = new HashMap<String, Object>();
        dateUnitSettings.put(OmronConstants.OMRONDeviceDistanceSettings.UnitKey, OmronConstants.OMRONDeviceDistanceUnit.Kilometer);
        HashMap<String, HashMap> distanceSettings = new HashMap<>();
        distanceSettings.put(OmronConstants.OMRONDeviceDistanceSettingsKey, dateUnitSettings);

        // Time Format
        HashMap<String, Object> timeFormatSettings = new HashMap<String, Object>();
        timeFormatSettings.put(OmronConstants.OMRONDeviceTimeSettings.FormatKey, OmronConstants.OMRONDeviceTimeFormat.Time24Hour);
        HashMap<String, HashMap> timeSettings = new HashMap<>();
        timeSettings.put(OmronConstants.OMRONDeviceTimeSettingsKey, timeFormatSettings);

        // Sleep Settings
        HashMap<String, Object> sleepTimeSettings = new HashMap<String, Object>();
        sleepTimeSettings.put(OmronConstants.OMRONDeviceSleepSettings.AutomaticKey, OmronConstants.OMRONDeviceSleepAutomatic.On);
        sleepTimeSettings.put(OmronConstants.OMRONDeviceSleepSettings.StartTimeKey, "19");
        sleepTimeSettings.put(OmronConstants.OMRONDeviceSleepSettings.StopTimeKey, "20");
        HashMap<String, HashMap> sleepSettings = new HashMap<>();
        sleepSettings.put(OmronConstants.OMRONDeviceSleepSettingsKey, sleepTimeSettings);

        // Alarm Settings
        // Alarm 1 Time
        HashMap<String, Object> alarmTime1 = new HashMap<String, Object>();
        alarmTime1.put(OmronConstants.OMRONDeviceAlarmSettings.HourKey, "15");
        alarmTime1.put(OmronConstants.OMRONDeviceAlarmSettings.MinuteKey, "33");
        // Alarm 1 Day (SUN-SAT)
        HashMap<String, Object> alarmDays1 = new HashMap<String, Object>();
        alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.SundayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.MondayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.FridayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        HashMap<String, Object> alarm1 = new HashMap<>();
        alarm1.put(OmronConstants.OMRONDeviceAlarmSettings.DaysKey, alarmDays1);
        alarm1.put(OmronConstants.OMRONDeviceAlarmSettings.TimeKey, alarmTime1);
        alarm1.put(OmronConstants.OMRONDeviceAlarmSettings.TypeKey, OmronConstants.OMRONDeviceAlarmType.Measure);


        // Alarm 2 Time
        HashMap<String, Object> alarmTime2 = new HashMap<String, Object>();
        alarmTime2.put(OmronConstants.OMRONDeviceAlarmSettings.HourKey, "15");
        alarmTime2.put(OmronConstants.OMRONDeviceAlarmSettings.MinuteKey, "34");
        // Alarm 2 Day (SUN-SAT)
        HashMap<String, Object> alarmDays2 = new HashMap<String, Object>();
        alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.SundayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.MondayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.FridayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
        HashMap<String, Object> alarm2 = new HashMap<>();
        alarm2.put(OmronConstants.OMRONDeviceAlarmSettings.DaysKey, alarmDays2);
        alarm2.put(OmronConstants.OMRONDeviceAlarmSettings.TimeKey, alarmTime2);
        alarm2.put(OmronConstants.OMRONDeviceAlarmSettings.TypeKey, OmronConstants.OMRONDeviceAlarmType.Medication);

        // Add Alarm1, Alarm2, Alarm3 to List
        ArrayList<HashMap> alarms = new ArrayList<>();
        alarms.add(alarm1);
        alarms.add(alarm2);
        HashMap<String, Object> alarmSettings = new HashMap<>();
        alarmSettings.put(OmronConstants.OMRONDeviceAlarmSettingsKey, alarms);


        // Notification settings
        ArrayList<String> notificationsAvailable = new ArrayList<>();
        notificationsAvailable.add("android.intent.action.PHONE_STATE");
        notificationsAvailable.add("com.google.android.gm");
        notificationsAvailable.add("android.provider.Telephony.SMS_RECEIVED");
        HashMap<String, Object> notificationSettings = new HashMap<String, Object>();
        notificationSettings.put(OmronConstants.OMRONDeviceNotificationSettingsKey, notificationsAvailable);

        ArrayList<HashMap> deviceSettings = new ArrayList<>();
        deviceSettings.add(userSettings);
        deviceSettings.add(dateSettings);
        deviceSettings.add(distanceSettings);
        deviceSettings.add(timeSettings);
        deviceSettings.add(sleepSettings);
        deviceSettings.add(alarmSettings);
        deviceSettings.add(notificationSettings);

        peripheralConfig.deviceSettings = deviceSettings;


        // Set Scan timeout interval (optional)
        peripheralConfig.timeoutInterval = Constants.CONNECTION_TIMEOUT;

        // Set User Hash Id (mandatory)
        peripheralConfig.userHashId = "<email_address_of_user>"; // Set logged in user email

        peripheralConfig.enableAllDataRead = true;

        // Set configuration for OmronPeripheralManager
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).setConfiguration(peripheralConfig);

        //Create peripheral object with localname and UUID
        OmronPeripheral peripheral = new OmronPeripheral(mSelectedPeripheral.getLocalName(), mSelectedPeripheral.getUuid());

        //Call to update the settings
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).updatePeripheral(peripheral, new OmronPeripheralManagerUpdateListener() {
            @Override
            public void onUpdateCompleted(final OmronPeripheral peripheral, final OmronErrorInfo resultInfo) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (resultInfo.getResultCode() == 0 && peripheral != null) {

                            mSelectedPeripheral = peripheral;

                            if (null != peripheral.getLocalName()) {

                                mTvDeviceLocalName.setText(peripheral.getLocalName());
                                mTvDeviceUuid.setText(peripheral.getUuid());
                                showMessage(getString(R.string.device_connected), getString(R.string.update_success));

                                HashMap<String, String> deviceInformation = peripheral.getDeviceInformation();
                                Log.d(TAG, "Device Information : " + deviceInformation);

                                OmronPeripheralManagerConfig peripheralConfig = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getConfiguration();
                                Log.d(TAG, "Device Config :  " + peripheralConfig.getDeviceConfigGroupIdAndGroupIncludedId(peripheral.getDeviceGroupIDKey(), peripheral.getDeviceGroupIncludedGroupIDKey()));
                            }
                        } else {

                            setStatus("-");
                            mTvErrorCode.setText(resultInfo.getDetailInfo());
                            mTvErrorDesc.setText(resultInfo.getMessageInfo());
                        }

                        enableDisableButton(true);
                    }
                });

            }
        });
    }


    private ArrayList<HashMap> getBloodPressureSettings(ArrayList<HashMap> deviceSettings, boolean isPairing) {

        // Blood Pressure
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.BLOODPRESSURE) {
            HashMap<String, Object> bloodPressurePersonalSettings = new HashMap<>();
            bloodPressurePersonalSettings.put(OmronConstants.OMRONDevicePersonalSettings.BloodPressureTruReadEnableKey, OmronConstants.OMRONDevicePersonalSettingsBloodPressureTruReadStatus.On);
            bloodPressurePersonalSettings.put(OmronConstants.OMRONDevicePersonalSettings.BloodPressureTruReadIntervalKey, OmronConstants.OMRONDevicePersonalSettingsBloodPressureTruReadInterval.Interval30);
            HashMap<String, Object> settings = new HashMap<>();
            settings.put(OmronConstants.OMRONDevicePersonalSettings.BloodPressureKey, bloodPressurePersonalSettings);
            HashMap<String, HashMap> personalSettings = new HashMap<>();
            personalSettings.put(OmronConstants.OMRONDevicePersonalSettingsKey, settings);

            HashMap<String, Object> transferModeSettings = new HashMap<>();
            HashMap<String, HashMap> transferSettings = new HashMap<>();
            if(isPairing) {
                transferModeSettings.put(OmronConstants.OMRONDeviceScanSettings.ModeKey, OmronConstants.OMRONDeviceScanSettingsMode.Pairing);
            }else {
                transferModeSettings.put(OmronConstants.OMRONDeviceScanSettings.ModeKey, OmronConstants.OMRONDeviceScanSettingsMode.MismatchSequence);
            }
            transferSettings.put(OmronConstants.OMRONDeviceScanSettingsKey, transferModeSettings);

            // Personal settings for device
            deviceSettings.add(personalSettings);

            deviceSettings.add(transferSettings);
        }

        return deviceSettings;
    }

    private ArrayList<HashMap> getActivitySettings(ArrayList<HashMap> deviceSettings) {

        // Activity Tracker
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {

            // Set Personal Settings in Configuration (mandatory for Activity devices)
            if (personalSettings != null) {

                HashMap<String, String> settingsModel = new HashMap<String, String>();
                settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserHeightKey, personalSettings.get("personalHeight"));
                settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserWeightKey, personalSettings.get("personalWeight"));
                settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserStrideKey, personalSettings.get("personalStride"));
                settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.TargetSleepKey, "120");
                settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.TargetStepsKey, "2000");

                HashMap<String, HashMap> userSettings = new HashMap<>();
                userSettings.put(OmronConstants.OMRONDevicePersonalSettingsKey, settingsModel);

                // Notification settings
                ArrayList<String> notificationsAvailable = new ArrayList<>();
                notificationsAvailable.add("android.intent.action.PHONE_STATE");
                notificationsAvailable.add("com.google.android.gm");
                notificationsAvailable.add("android.provider.Telephony.SMS_RECEIVED");
                notificationsAvailable.add("com.omronhealthcare.OmronConnectivitySample");
                HashMap<String, Object> notificationSettings = new HashMap<String, Object>();
                notificationSettings.put(OmronConstants.OMRONDeviceNotificationSettingsKey, notificationsAvailable);

                // Time Format
                HashMap<String, Object> timeFormatSettings = new HashMap<String, Object>();
                timeFormatSettings.put(OmronConstants.OMRONDeviceTimeSettings.FormatKey, OmronConstants.OMRONDeviceTimeFormat.Time12Hour);
                HashMap<String, HashMap> timeSettings = new HashMap<>();
                timeSettings.put(OmronConstants.OMRONDeviceTimeSettingsKey, timeFormatSettings);


                // Sleep Settings
                HashMap<String, Object> sleepTimeSettings = new HashMap<String, Object>();
                sleepTimeSettings.put(OmronConstants.OMRONDeviceSleepSettings.AutomaticKey, OmronConstants.OMRONDeviceSleepAutomatic.Off);
                sleepTimeSettings.put(OmronConstants.OMRONDeviceSleepSettings.StartTimeKey, "19");
                sleepTimeSettings.put(OmronConstants.OMRONDeviceSleepSettings.StopTimeKey, "20");
                HashMap<String, HashMap> sleepSettings = new HashMap<>();
                sleepSettings.put(OmronConstants.OMRONDeviceSleepSettingsKey, sleepTimeSettings);


                // Alarm Settings
                // Alarm 1 Time
                HashMap<String, Object> alarmTime1 = new HashMap<String, Object>();
                alarmTime1.put(OmronConstants.OMRONDeviceAlarmSettings.HourKey, "15");
                alarmTime1.put(OmronConstants.OMRONDeviceAlarmSettings.MinuteKey, "33");
                // Alarm 1 Day (SUN-SAT)
                HashMap<String, Object> alarmDays1 = new HashMap<String, Object>();
                alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.SundayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.MondayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey, OmronConstants.OMRONDeviceAlarmStatus.On);
                alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.FridayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays1.put(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                HashMap<String, Object> alarm1 = new HashMap<>();
                alarm1.put(OmronConstants.OMRONDeviceAlarmSettings.DaysKey, alarmDays1);
                alarm1.put(OmronConstants.OMRONDeviceAlarmSettings.TimeKey, alarmTime1);
                alarm1.put(OmronConstants.OMRONDeviceAlarmSettings.TypeKey, OmronConstants.OMRONDeviceAlarmType.Measure);


                // Alarm 2 Time
                HashMap<String, Object> alarmTime2 = new HashMap<String, Object>();
                alarmTime2.put(OmronConstants.OMRONDeviceAlarmSettings.HourKey, "15");
                alarmTime2.put(OmronConstants.OMRONDeviceAlarmSettings.MinuteKey, "34");
                // Alarm 2 Day (SUN-SAT)
                HashMap<String, Object> alarmDays2 = new HashMap<String, Object>();
                alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.SundayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.MondayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey, OmronConstants.OMRONDeviceAlarmStatus.On);
                alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.FridayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                alarmDays2.put(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey, OmronConstants.OMRONDeviceAlarmStatus.Off);
                HashMap<String, Object> alarm2 = new HashMap<>();
                alarm2.put(OmronConstants.OMRONDeviceAlarmSettings.DaysKey, alarmDays2);
                alarm2.put(OmronConstants.OMRONDeviceAlarmSettings.TimeKey, alarmTime2);
                alarm2.put(OmronConstants.OMRONDeviceAlarmSettings.TypeKey, OmronConstants.OMRONDeviceAlarmType.Medication);

                // Add Alarm1, Alarm2, Alarm3 to List
                ArrayList<HashMap> alarms = new ArrayList<>();
                alarms.add(alarm1);
                alarms.add(alarm2);
                HashMap<String, Object> alarmSettings = new HashMap<>();
                alarmSettings.put(OmronConstants.OMRONDeviceAlarmSettingsKey, alarms);


                // Notification enable settings
                HashMap<String, Object> notificationEnableSettings = new HashMap<String, Object>();
                notificationEnableSettings.put(OmronConstants.OMRONDeviceNotificationStatusKey, OmronConstants.OMRONDeviceNotificationStatus.On);
                HashMap<String, HashMap> notificationStatusSettings = new HashMap<>();
                notificationStatusSettings.put(OmronConstants.OMRONDeviceNotificationEnableSettingsKey, notificationEnableSettings);


                deviceSettings.add(userSettings);
                deviceSettings.add(notificationSettings);
                deviceSettings.add(alarmSettings);
                deviceSettings.add(timeSettings);
                deviceSettings.add(sleepSettings);
                deviceSettings.add(notificationStatusSettings);
            }
        }

        return deviceSettings;
    }

    private ArrayList<HashMap> getBCMSettings(ArrayList<HashMap> deviceSettings) {

        // body composition
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.BODYCOMPOSITION) {

            //Weight settings
            HashMap<String, Object> weightPersonalSettings = new HashMap<>();
            weightPersonalSettings.put(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey, 100);

            HashMap<String, Object> settings = new HashMap<>();
            settings.put(OmronConstants.OMRONDevicePersonalSettings.UserHeightKey, "17000");
            settings.put(OmronConstants.OMRONDevicePersonalSettings.UserGenderKey, OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Male);
            settings.put(OmronConstants.OMRONDevicePersonalSettings.UserDateOfBirthKey, "19001010");
            settings.put(OmronConstants.OMRONDevicePersonalSettings.WeightKey, weightPersonalSettings);

            HashMap<String, HashMap> personalSettings = new HashMap<>();
            personalSettings.put(OmronConstants.OMRONDevicePersonalSettingsKey, settings);

            // Weight Settings
            // Add other weight common settings if any
            HashMap<String, Object> weightCommonSettings = new HashMap<>();
            weightCommonSettings.put(OmronConstants.OMRONDeviceWeightSettings.UnitKey, OmronConstants.OMRONDeviceWeightUnit.Lbs);
            HashMap<String, Object> weightSettings = new HashMap<>();
            weightSettings.put(OmronConstants.OMRONDeviceWeightSettingsKey, weightCommonSettings);

            deviceSettings.add(personalSettings);
            deviceSettings.add(weightSettings);
        }

        return deviceSettings;
    }

    // UI Functions

    private void initClickListeners() {

        // To perform scan process.
        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                startScanning();
            }
        });

        findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                disconnectDevice();
            }
        });

        //Data transfer
        findViewById(R.id.btn_transfer).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                transferData();
            }
        });

        //Add device for the connected device list.
        findViewById(R.id.iv_add_device).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        //Open Vital data activity
        findViewById(R.id.iv_vital_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedPeripheral != null)
                    if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {
                        Intent toVitalData = new Intent(MainActivity.this, DataListingActivity.class);
                        toVitalData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, mSelectedPeripheral.getLocalName());
                        startActivity(toVitalData);
                    } else {
                        Intent toVitalData = new Intent(MainActivity.this, VitalDataListingActivity.class);
                        toVitalData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, mSelectedPeripheral.getLocalName());
                        startActivity(toVitalData);
                    }
            }
        });

        //Open Settings update activity
        findViewById(R.id.iv_device_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (mSelectedPeripheral != null) {
//                    PeripheralDevice peripheralDevice = new PeripheralDevice(mSelectedPeripheral.getLocalName(), mSelectedPeripheral.getUuid(), mSelectedPeripheral.getSelectedUser(), Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)));
//                    peripheralDevice.setModelName(mSelectedPeripheral.getModelName());
//                    peripheralDevice.setModelSeries(mSelectedPeripheral.getModelSeries());
//                    Intent intent = new Intent(MainActivity.this, ReminderActivity.class);
//                    intent.putExtra(ReminderActivity.ARG_DEVICE, peripheralDevice);
//                    if (personalSettings != null)
//                        intent.putExtra(Constants.extraKeys.KEY_PERSONAL_SETTINGS, personalSettings);
//
//                    if (device != null)
//                        intent.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, device);
//
//                    startActivity(intent);
//                }
            }
        });
    }


    private void initViews() {

        mLvScannedList = (ListView) findViewById(R.id.lv_scannedlist);
        mTvTImeStamp = (TextView) findViewById(R.id.tv_timestamp_value);
        mTvSystolic = (TextView) findViewById(R.id.tv_sys_value);
        mTvDiastolic = (TextView) findViewById(R.id.tv_dia_value);
        mTvPulseRate = (TextView) findViewById(R.id.tv_pulse_value);
        mTvUserSelected = (TextView) findViewById(R.id.tv_userselected);


        mTvDeviceInfo = (TextView) findViewById(R.id.device_info);
        mTvDeviceLocalName = (TextView) findViewById(R.id.tv_device_name);
        mTvDeviceUuid = (TextView) findViewById(R.id.tv_device_uuid);
        mTvStatusLabel = (TextView) findViewById(R.id.tv_status_value);
        mTvErrorCode = (TextView) findViewById(R.id.tv_error_value);
        mTvErrorDesc = (TextView) findViewById(R.id.tv_error_desc);

        mRlDeviceListView = (RelativeLayout) findViewById(R.id.rl_device_list);
        mRlTransferView = (RelativeLayout) findViewById(R.id.rl_transfer_view);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_scan);

        mTvUserSelected.setText(android.text.TextUtils.join(",", selectedUsers));

        scanBtn = (Button) findViewById(R.id.btn_scan);
        transferBtn = (Button) findViewById(R.id.btn_transfer);

        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {
            // Activity Tracker or HeartVue
            findViewById(R.id.iv_device_setting).setVisibility(View.VISIBLE);
        } else {
            // Blood Pressure
            findViewById(R.id.iv_device_setting).setVisibility(View.INVISIBLE);
        }

        // Hide Add device
        findViewById(R.id.iv_add_device).setVisibility(View.GONE);
    }

    private void enableDisableButton(boolean enable) {

        findViewById(R.id.iv_vital_data).setEnabled(enable);
        scanBtn.setEnabled(enable);
        transferBtn.setEnabled(enable);
    }

    private void initLists() {

        mPeripheralList = new ArrayList<OmronPeripheral>();
        mScannedDevicesAdapter = new ScannedDevicesAdapter(mContext, mPeripheralList);
        mLvScannedList.setAdapter(mScannedDevicesAdapter);
        mLvScannedList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                int noOfUsers = Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Users));
                if (noOfUsers == 1) {
                    connectPeripheral(mScannedDevicesAdapter.getItem(position));
                }else {
                    connectPeripheralWithWait(mScannedDevicesAdapter.getItem(position));
                }
            }
        });
    }

    private void showDeviceListView() {
        mRlTransferView.setVisibility(View.GONE);
        mRlDeviceListView.setVisibility(View.VISIBLE);
    }

    private void showTransferView() {
        mRlDeviceListView.setVisibility(View.GONE);
        mRlTransferView.setVisibility(View.VISIBLE);
        setDeviceInformation();
    }

    private void setStatus(String statusMessage) {
        mTvStatusLabel.setText(statusMessage);
    }

    private void resetErrorMessage() {

        mTvErrorCode.setText("-");
        mTvErrorDesc.setText("-");
        mTvStatusLabel.setText("-");

    }

    private void setDeviceInformation() {
        if (null != mSelectedPeripheral) {
            if (null != mSelectedPeripheral.getModelName()) {
                mTvDeviceInfo.setText(mSelectedPeripheral.getModelName() + " - " + getString(R.string.device_information));
            } else {
                mTvDeviceInfo.setText(getString(R.string.device_information));

            }
        }
    }

    private void showVitalDataResult(final ArrayList<HashMap<String, Object>> bloodPressureItemList) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (bloodPressureItemList.size() == 0) {

                    mTvErrorDesc.setText("No New readings transferred");
                    mTvTImeStamp.setText("-");
                    mTvSystolic.setText("-");
                    mTvDiastolic.setText("-");
                    mTvPulseRate.setText("-");

                } else {

                    HashMap<String, Object> bloodPressureItem = bloodPressureItemList.get(bloodPressureItemList.size() - 1);

                    mTvErrorDesc.setText("-");
                    mTvTImeStamp.setText(String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.DateKey)));
                    mTvSystolic.setText(String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.SystolicKey)) + "\t mmHg");
                    mTvDiastolic.setText(String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.DiastolicKey)) + "\t mmHg");
                    mTvPulseRate.setText(String.valueOf(bloodPressureItem.get(OmronConstants.OMRONVitalData.PulseKey)) + "\t bpm");

                }
            }
        });

    }

    private void resetVitalDataResult() {

        mTvTImeStamp.setText("-");
        mTvSystolic.setText("-");
        mTvDiastolic.setText("-");
        mTvPulseRate.setText("-");
    }

    private void resetDeviceList() {

        if (mScannedDevicesAdapter != null) {
            mProgressBar.setVisibility(View.GONE);
            mPeripheralList = new ArrayList<OmronPeripheral>();
            mScannedDevicesAdapter.setPeripheralList(mPeripheralList);
            mScannedDevicesAdapter.notifyDataSetChanged();
        }
    }

    public void showMessage(String title, String message) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                });


        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}