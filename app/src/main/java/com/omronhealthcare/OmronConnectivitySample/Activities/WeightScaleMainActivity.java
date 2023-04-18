package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class WeightScaleMainActivity extends BaseActivity {

    private Context mContext;
    private String TAG = "OmronSampleApp";

    private ListView mLvScannedList;
    private ArrayList<OmronPeripheral> mPeripheralList;
    private ScannedDevicesAdapter mScannedDevicesAdapter;
    private OmronPeripheral mSelectedPeripheral;

    private int mSelectedUser = 1;
    private ArrayList<Integer> selectedUsers = new ArrayList<>();

    private RelativeLayout mRlDeviceListView, mRlTransferView;
    private TextView mTvTImeStamp, mWeightData1, mWeightData2, mWeightData3, mTvUserSelected, mWeightData4, mDCIValue;
    private TextView mTvDeviceInfo, mTvDeviceLocalName, mTvDeviceUuid, mTvStatusLabel, mTvErrorCode, mTvErrorDesc;
    private ProgressBar mProgressBar;
    private Button scanBtn;
    private Button transferBtn;
    private Bundle weightBundle;
    private ImageView editButton;

    private PreferencesManager preferencesManager = null;

    HashMap<String, String> device = null;

    private Boolean isScan;

    private final int TIME_INTERVAL = 1000;

    Handler mHandler;
    Runnable mRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_main);
        mContext = this;

        isScan = false;

        if (preferencesManager == null)
            preferencesManager = new PreferencesManager(WeightScaleMainActivity.this);

        // Selected users
        selectedUsers = (ArrayList<Integer>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_USER);
        if (selectedUsers == null) {
            selectedUsers = new ArrayList<>();
            selectedUsers.add(1);
        }

        // Selected device
        device = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_DEVICE);

        weightBundle = getIntent().getBundleExtra(Constants.extraKeys.KEY_WEIGHT_SETTINGS);

        initViews();
        showDeviceListView();
        initClickListeners();
        initLists();

        // Start OmronPeripheralManager
        startOmronPeripheralManager(false);

    }

    @Override
    protected void onPause() {

        super.onPause();

        enableDisableButton(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            weightBundle = data.getBundleExtra(Constants.extraKeys.KEY_WEIGHT_SETTINGS);
            updatePeripheralForSelectedUser();
        }
    }

    /**
     * Configure library functionalities
     */
    private void startOmronPeripheralManager(boolean isHistoricDataRead) {

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
//        HashMap<Integer, Integer> sequenceNumbersForTransfer = new HashMap<>();
//        sequenceNumbersForTransfer.put(1, 20);
//        sequenceNumbersForTransfer.put(2, 0);
//        peripheralConfig.sequenceNumbersForTransfer = sequenceNumbersForTransfer;

        // Set configuration for OmronPeripheralManager
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).setConfiguration(peripheralConfig);


        //Initialize the connection process.
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).startManager();

        // Notification Listener for BLE State Change
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(OmronConstants.OMRONBLECentralManagerDidUpdateStateNotification));
    }

    private void startScanning() {

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

                                Toast.makeText(WeightScaleMainActivity.this, "Error Code : " + resultInfo.getResultCode() + "\nError Detail Code : " + resultInfo.getDetailInfo(), Toast.LENGTH_LONG).show();
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

                            WeightScaleMainActivity.this.runOnUiThread(new Runnable() {
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


//                        try {
//                            Object personalSettingsForSelectedUser = mSelectedPeripheral.getDeviceSettingsWithUser(mSelectedUser);
//                            if(personalSettingsForSelectedUser != null){
//                                HashMap<String, Object> settings = (HashMap<String, Object>) personalSettingsForSelectedUser;
//                                if(settings.containsKey(OmronConstants.OMRONDevicePersonalSettings.WeightKey)){
//                                    HashMap<String, Object> weightSettings = (HashMap<String, Object>) settings.get(OmronConstants.OMRONDevicePersonalSettings.WeightKey);
//                                    if(weightSettings != null){
//                                        if(weightSettings.containsKey(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey)){
//                                            int dciValue = (int) weightSettings.get(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey);
//                                            preferencesManager.saveDCIValue(dciValue);
//                                            mDCIValue.setText(String.valueOf(dciValue));
//                                        }
//                                    }
//                                }
//                            }
//                        }catch (Exception e){
//
//                        }


                        if (wait) {
                            mHandler = new Handler();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    resumeConnection(peripheral);
                                }
                            }, 5000);
                        } else {
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
                        Toast.makeText(WeightScaleMainActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show();

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
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {
            startOmronPeripheralManager(false);
            performDataTransfer();
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
            alertDialogBuilder.setTitle("Transfer");
            alertDialogBuilder.setMessage("Do you want to transfer all historic readings from device?");
            alertDialogBuilder.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            startOmronPeripheralManager(true);
                            performDataTransfer();
                        }
                    });
            alertDialogBuilder.setNegativeButton("No",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            startOmronPeripheralManager(false);
                            performDataTransfer();
                        }
                    });
            alertDialogBuilder.setCancelable(false);
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


    private void uploadData(HashMap<String, Object> vitalData, OmronPeripheral peripheral, boolean isWait) {

        HashMap<String, String> deviceInfo = peripheral.getDeviceInformation();

        // Weight Data
        ArrayList<HashMap<String, Object>> weightData = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataWeightKey);
        if (weightData != null) {

            for (HashMap<String, Object> weightItem : weightData) {

                Log.d("Weight - ", weightItem.toString());

            }
            //  insertVitalDataToDB(weightData, deviceInfo);
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

                            // Get vital data for previously selected user using OmronPeripheral
                            Object output = peripheral.getVitalDataWithUser(mSelectedUser);

                            if (output instanceof OmronErrorInfo) {

                                final OmronErrorInfo errorInfo = (OmronErrorInfo) output;

                                mTvErrorCode.setText(errorInfo.getResultCode() + " / " + errorInfo.getDetailInfo());
                                mTvErrorDesc.setText(errorInfo.getMessageInfo());

                            } else {

                                HashMap<String, Object> vitalData = (HashMap<String, Object>) output;

                                if (vitalData != null) {

                                    //Weightdata
                                    final ArrayList<HashMap<String, Object>> weightItemList = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataWeightKey);
                                    if (weightItemList != null) {

                                        showVitalDataResult(weightItemList);
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

        for (HashMap<String, Object> weightItem : dataList) {

            ContentValues cv = new ContentValues();
            cv.put(OmronDBConstans.WEIGHT_DATA_StartTimeKey, String.valueOf(weightItem.get(OmronConstants.OMRONWeightData.StartDateKey)));
            cv.put(OmronDBConstans.WEIGHT_DATA_WeightKey, String.valueOf(weightItem.get(OmronConstants.OMRONWeightData.WeightKey)));
            cv.put(OmronDBConstans.WEIGHT_DATA_BMIKey, String.valueOf(weightItem.get(OmronConstants.OMRONWeightData.BMIKey)));
            cv.put(OmronDBConstans.WEIGHT_DATA_RestingMetabolismKey, String.valueOf(weightItem.get(OmronConstants.OMRONWeightData.RestingMetabolismKey)));
            cv.put(OmronDBConstans.DEVICE_SELECTED_USER, String.valueOf(weightItem.get(OmronConstants.OMRONVitalData.UserIdKey)));
            cv.put(OmronDBConstans.DEVICE_LOCAL_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.LocalNameKey).toLowerCase());
            cv.put(OmronDBConstans.DEVICE_DISPLAY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.DisplayNameKey));
            cv.put(OmronDBConstans.DEVICE_IDENTITY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.IdentityNameKey));
            cv.put(OmronDBConstans.DEVICE_CATEGORY, device.get(OmronConstants.OMRONBLEConfigDevice.Category));

            Uri uri = getContentResolver().insert(OmronDBConstans.WEIGHT_DATA_CONTENT_URI, cv);
            if (uri != null) {
                //TODO successful insert
            }
        }
    }

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

        // Category body composition

        HashMap<String, Object> settingsModel = new HashMap<>();
        HashMap<String, HashMap> userSettings = new HashMap<>();
        HashMap<String, Object> personalWeightSettings = new HashMap<>();


        String gender = weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_GENDER);
        String unit = weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_WEIGHT_UNIT);
        int unitValue;
        if (unit.equals("Kg")) {
            unitValue = OmronConstants.OMRONDeviceWeightUnit.Kg;
        } else if (unit.equals("Lbs")) {
            unitValue = OmronConstants.OMRONDeviceWeightUnit.Lbs;
        } else {
            unitValue = OmronConstants.OMRONDeviceWeightUnit.St;

        }
        int genderValue = OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Male;
        if (gender.equals("Female")) {
            genderValue = OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Female;
        }

        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserHeightKey, weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_HEIGHT_CM));
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserGenderKey, genderValue);
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserDateOfBirthKey, weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_DOB, "19000101"));

        //Weight settings

        personalWeightSettings.put(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey, OmronConstants.OMRONDevicePersonalSettings.WeightDCINotAvailable);
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.WeightKey, personalWeightSettings);
        userSettings.put(OmronConstants.OMRONDevicePersonalSettingsKey, settingsModel);
        // Weight Settings
        // Add other weight common settings if any
        HashMap<String, Object> weightCommonSettings = new HashMap<>();
        weightCommonSettings.put(OmronConstants.OMRONDeviceWeightSettings.UnitKey, unitValue);
        HashMap<String, Object> weightSettings = new HashMap<>();
        weightSettings.put(OmronConstants.OMRONDeviceWeightSettingsKey, weightCommonSettings);
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.WeightKey, weightSettings);
        userSettings.put(OmronConstants.OMRONDevicePersonalSettingsKey, settingsModel);

        ArrayList<HashMap> deviceSettings = new ArrayList<>();
        deviceSettings.add(userSettings);

        peripheralConfig.deviceSettings = deviceSettings;


        // Set Scan timeout interval (optional)
        peripheralConfig.timeoutInterval = Constants.CONNECTION_TIMEOUT;

        // Set User Hash Id (mandatory)
        peripheralConfig.userHashId = "<email_address_of_user>"; // Set logged in user email

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

                                ArrayList<HashMap> deviceSettings = mSelectedPeripheral.getDeviceSettings();
                                if (deviceSettings != null) {
                                    Log.d(TAG, "Device Settings:" + deviceSettings.toString());
                                }
                                Object personalSettingsForUser1 = mSelectedPeripheral.getDeviceSettingsWithUser(1);
                                if (personalSettingsForUser1 != null) {
                                    Log.d(TAG, "Personal Settings for User 1:" + personalSettingsForUser1.toString());
                                }
                                Object personalSettingsForUser2 = mSelectedPeripheral.getDeviceSettingsWithUser(2);
                                if (personalSettingsForUser2 != null) {
                                    Log.d(TAG, "Personal Settings for User 2:" + personalSettingsForUser2.toString());
                                }

                                try {
                                    Object personalSettingsForSelectedUser = mSelectedPeripheral.getDeviceSettingsWithUser(mSelectedUser);
                                    if (personalSettingsForSelectedUser != null) {
                                        HashMap<String, Object> settings = (HashMap<String, Object>) personalSettingsForSelectedUser;
                                        if (settings.containsKey(OmronConstants.OMRONDevicePersonalSettings.WeightKey)) {
                                            HashMap<String, Object> weightSettings = (HashMap<String, Object>) settings.get(OmronConstants.OMRONDevicePersonalSettings.WeightKey);
                                            if (weightSettings != null) {
                                                if (weightSettings.containsKey(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey)) {
                                                    long dciValue = (long) weightSettings.get(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey);
                                                    preferencesManager.saveDCIValue(dciValue);
                                                    mDCIValue.setText(String.valueOf(dciValue));
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {

                                }

                                OmronPeripheralManagerConfig peripheralConfig = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getConfiguration();
                                Log.d(TAG, "Device Config :  " + peripheralConfig.getDeviceConfigGroupIdAndGroupIncludedId(peripheral.getDeviceGroupIDKey(), peripheral.getDeviceGroupIncludedGroupIDKey()));
                            } else {

                                setStatus("-");
                                mTvErrorCode.setText(resultInfo.getDetailInfo());
                                mTvErrorDesc.setText(resultInfo.getMessageInfo());
                            }
                        }
                        enableDisableButton(true);
                    }
                });

            }
        });
    }

    private void updatePeripheralForSelectedUser() {


        if (mSelectedPeripheral == null) {
            mTvErrorDesc.setText("Device Not Paired");
            return;
        }

        resetErrorMessage();
        enableDisableButton(false);
        resetVitalDataResult();

        // Set State Change Listener
        setStateChanges();

        setStatus("Connecting...");

        OmronPeripheralManagerConfig peripheralConfig = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getConfiguration();

        // Filter device to scan and connect (optional)
        if (device != null && device.get(OmronConstants.OMRONBLEConfigDevice.GroupID) != null && device.get(OmronConstants.OMRONBLEConfigDevice.GroupIncludedGroupID) != null) {

            // Add item
            List<HashMap<String, String>> filterDevices = new ArrayList<>();
            filterDevices.add(device);
            peripheralConfig.deviceFilters = filterDevices;
        }

        // Category body composition

        HashMap<String, Object> settingsModel = new HashMap<>();

        String gender = weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_GENDER);
        String unit = weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_WEIGHT_UNIT);
        int unitValue;
        if (unit.equals("Kg")) {
            unitValue = OmronConstants.OMRONDeviceWeightUnit.Kg;
        } else if (unit.equals("Lbs")) {
            unitValue = OmronConstants.OMRONDeviceWeightUnit.Lbs;
        } else {
            unitValue = OmronConstants.OMRONDeviceWeightUnit.St;

        }
        int genderValue = OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Male;
        if (gender.equals("Female")) {
            genderValue = OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Female;
        }

        double height = Double.parseDouble(weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_HEIGHT_CM));
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserHeightKey, weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_HEIGHT_CM));
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserGenderKey, genderValue);
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.UserDateOfBirthKey, weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_DOB, "19000101"));


        //Weight settings
        HashMap<String, Object> personalWeightSettings = new HashMap<>();
        personalWeightSettings.put(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey, OmronConstants.OMRONDevicePersonalSettings.WeightDCINotAvailable);
        settingsModel.put(OmronConstants.OMRONDevicePersonalSettings.WeightKey, personalWeightSettings);

        // Weight Settings
        // Add other weight common settings if any
        HashMap<String, Object> weightCommonSettings = new HashMap<>();
        weightCommonSettings.put(OmronConstants.OMRONDeviceWeightSettings.UnitKey, unitValue);
        HashMap<String, Object> weightSettings = new HashMap<>();
        weightSettings.put(OmronConstants.OMRONDeviceWeightSettingsKey, weightCommonSettings);


        HashMap<String, HashMap> userSettings = new HashMap<>();
        userSettings.put(OmronConstants.OMRONDevicePersonalSettingsKey, settingsModel);

        ArrayList<HashMap> deviceSettings = new ArrayList<>();
        deviceSettings.add(userSettings);
        deviceSettings.add(weightSettings);

        peripheralConfig.deviceSettings = deviceSettings;


        // Set Scan timeout interval (optional)
        peripheralConfig.timeoutInterval = Constants.CONNECTION_TIMEOUT;

        // Set User Hash Id (mandatory)
        peripheralConfig.userHashId = "<email_address_of_user>"; // Set logged in user email

        // Set configuration for OmronPeripheralManager
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).setConfiguration(peripheralConfig);

        //Initialize the connection process.
//        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).startManager();

        //Create peripheral object with localname and UUID
        OmronPeripheral peripheral = new OmronPeripheral(mSelectedPeripheral.getLocalName(), mSelectedPeripheral.getUuid());

        //Call to update the settings
        OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).updatePeripheral(peripheral, mSelectedUser, new OmronPeripheralManagerUpdateListener() {
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

                                ArrayList<HashMap> deviceSettings = mSelectedPeripheral.getDeviceSettings();
                                if (deviceSettings != null) {
                                    Log.d(TAG, "Device Settings:" + deviceSettings.toString());
                                }
                                Object personalSettingsForUser1 = mSelectedPeripheral.getDeviceSettingsWithUser(1);
                                if (personalSettingsForUser1 != null) {
                                    Log.d(TAG, "Personal Settings for User 1:" + personalSettingsForUser1.toString());
                                }
                                Object personalSettingsForUser2 = mSelectedPeripheral.getDeviceSettingsWithUser(2);
                                if (personalSettingsForUser2 != null) {
                                    Log.d(TAG, "Personal Settings for User 2:" + personalSettingsForUser2.toString());
                                }

                                try {
                                    Object personalSettingsForSelectedUser = mSelectedPeripheral.getDeviceSettingsWithUser(mSelectedUser);
                                    if (personalSettingsForSelectedUser != null) {
                                        HashMap<String, Object> settings = (HashMap<String, Object>) personalSettingsForSelectedUser;
                                        if (settings.containsKey(OmronConstants.OMRONDevicePersonalSettings.WeightKey)) {
                                            HashMap<String, Object> weightSettings = (HashMap<String, Object>) settings.get(OmronConstants.OMRONDevicePersonalSettings.WeightKey);
                                            if (weightSettings != null) {
                                                if (weightSettings.containsKey(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey)) {
                                                    long dciValue = (long) weightSettings.get(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey);
                                                    preferencesManager.saveDCIValue(dciValue);
                                                    mDCIValue.setText(String.valueOf(dciValue));
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {

                                }
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


    private ArrayList<HashMap> getBCMSettings(ArrayList<HashMap> deviceSettings) {

        // body composition
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.BODYCOMPOSITION) {

            //Weight settings
            HashMap<String, Object> weightPersonalSettings = new HashMap<>();
            weightPersonalSettings.put(OmronConstants.OMRONDevicePersonalSettings.WeightDCIKey, OmronConstants.OMRONDevicePersonalSettings.WeightDCINotAvailable);

            HashMap<String, Object> settings = new HashMap<>();
            settings.put(OmronConstants.OMRONDevicePersonalSettings.WeightKey, weightPersonalSettings);
            if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Users)) > 1) {
                // BCM configuration

                String gender = weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_GENDER);
                int genderValue = OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Male;
                if (gender.equals("Female")) {
                    genderValue = OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Female;
                }

                settings.put(OmronConstants.OMRONDevicePersonalSettings.UserHeightKey, weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_HEIGHT_CM));
                settings.put(OmronConstants.OMRONDevicePersonalSettings.UserGenderKey, genderValue);
                settings.put(OmronConstants.OMRONDevicePersonalSettings.UserDateOfBirthKey, weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_DOB, "19000101"));
            }

            HashMap<String, HashMap> personalSettings = new HashMap<>();
            personalSettings.put(OmronConstants.OMRONDevicePersonalSettingsKey, settings);

            // Weight Settings
            // Add other weight common settings if any
            String unit = weightBundle.getString(Constants.bundleKeys.KEY_BUNDLE_WEIGHT_UNIT);
            int unitValue;
            if (unit.equals("Kg")) {
                unitValue = OmronConstants.OMRONDeviceWeightUnit.Kg;
            } else if (unit.equals("Lbs")) {
                unitValue = OmronConstants.OMRONDeviceWeightUnit.Lbs;
            } else {
                unitValue = OmronConstants.OMRONDeviceWeightUnit.St;

            }
            HashMap<String, Object> weightCommonSettings = new HashMap<>();
            weightCommonSettings.put(OmronConstants.OMRONDeviceWeightSettings.UnitKey, unitValue);
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

                int bluetoothState = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getBluetoothState();

                if (bluetoothState == OmronConstants.OMRONBLEBluetoothState.OMRONBLEBluetoothStateUnknown) {

                    Log.d(TAG, "Bluetooth is in unknown state");

                } else if (bluetoothState == OmronConstants.OMRONBLEBluetoothState.OMRONBLEBluetoothStateOff) {

                    Log.d(TAG, "Bluetooth is currently powered off");

                } else if (bluetoothState == OmronConstants.OMRONBLEBluetoothState.OMRONBLEBluetoothStateOn) {

                    Log.d(TAG, "Bluetooth is currently powered on");
                }

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
                        Intent toVitalData = new Intent(WeightScaleMainActivity.this, DataListingActivity.class);
                        toVitalData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, mSelectedPeripheral.getLocalName());
                        startActivity(toVitalData);
                    } else {
                        Intent toVitalData = new Intent(WeightScaleMainActivity.this, VitalDataListingActivity.class);
                        toVitalData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, mSelectedPeripheral.getLocalName());
                        startActivity(toVitalData);
                    }
            }
        });

        findViewById(R.id.iv_device_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WeightScaleMainActivity.this, UserPersonalSettingsActivity.class);
                intent.putExtra(Constants.bundleKeys.KEY_BUNDLE_IS_WEIGHT_UPDATE, true);
                startActivityForResult(intent, 1);
            }
        });
    }

    private void initViews() {

        mLvScannedList = (ListView) findViewById(R.id.lv_scannedlist);
        mTvTImeStamp = (TextView) findViewById(R.id.tv_timestamp_value);
        mWeightData1 = (TextView) findViewById(R.id.tv_sys_value);
        mWeightData2 = (TextView) findViewById(R.id.tv_dia_value);
        mWeightData3 = (TextView) findViewById(R.id.tv_pulse_value);
        mWeightData4 = (TextView) findViewById(R.id.tv_skeletal_muscle_value);
        mDCIValue = (TextView) findViewById(R.id.tv_dci_value);
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

        mTvUserSelected.setText(Integer.toString(mSelectedUser));

        scanBtn = (Button) findViewById(R.id.btn_scan);
        transferBtn = (Button) findViewById(R.id.btn_transfer);

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

    private void showVitalDataResult(final ArrayList<HashMap<String, Object>> weightData) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (weightData.size() == 0) {

                    mTvErrorDesc.setText("No New readings transferred");
                    mTvTImeStamp.setText("-");
                    mWeightData1.setText("-");
                    mWeightData2.setText("-");
                    mWeightData3.setText("-");
                    mWeightData4.setText("-");
                    mDCIValue.setText("-");


                } else {

                    HashMap<String, Object> weightDataItem = weightData.get(weightData.size() - 1);

                    mTvErrorDesc.setText("-");
                    SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis((Long) weightDataItem.get(OmronConstants.OMRONWeightData.StartDateKey));
                    mTvTImeStamp.setText(format.format(calendar.getTime()));

                    String weightText = "";
                    if(weightDataItem.get(OmronConstants.OMRONWeightData.WeightKey) != null) {
                        weightText = String.valueOf(weightDataItem.get(OmronConstants.OMRONWeightData.WeightKey)) + " Kg";
                    }

                    if(weightDataItem.get(OmronConstants.OMRONWeightData.WeightLbsKey) != null) {
                        weightText += " / "  + String.valueOf(weightDataItem.get(OmronConstants.OMRONWeightData.WeightLbsKey)) + " Lbs";
                    }

                    mWeightData1.setText(weightText);

                    if(weightDataItem.get(OmronConstants.OMRONWeightData.BMIKey) != null) {
                        mWeightData2.setText(String.valueOf(weightDataItem.get(OmronConstants.OMRONWeightData.BMIKey)));
                    }else {
                        mWeightData2.setText("-");
                    }
                    if(weightDataItem.get(OmronConstants.OMRONWeightData.RestingMetabolismKey) != null) {
                        mWeightData3.setText(String.valueOf(weightDataItem.get(OmronConstants.OMRONWeightData.RestingMetabolismKey)));
                    }else {
                        mWeightData3.setText("-");
                    }
                    if(weightDataItem.get(OmronConstants.OMRONWeightData.SkeletalMusclePercentageKey) != null) {
                        mWeightData4.setText(String.valueOf(weightDataItem.get(OmronConstants.OMRONWeightData.SkeletalMusclePercentageKey)));
                    }else {
                        mWeightData4.setText("-");
                    }

                }
            }
        });

    }

    private void resetVitalDataResult() {

        mTvTImeStamp.setText("-");
        mWeightData1.setText("-");
        mWeightData2.setText("-");
        mWeightData3.setText("-");
        mWeightData4.setText("-");
        mDCIValue.setText("-");
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
