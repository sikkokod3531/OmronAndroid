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

/**
 * Pulse Oxymeter Main DashBoard activity, where perform the pairing and data transfer.
 */

public class PulseOxymeterMainActivity extends BaseActivity {

    private Context mContext;
    private String TAG = "OmronSampleApp";

    private ListView mLvScannedList;
    private ArrayList<OmronPeripheral> mPeripheralList;
    private ScannedDevicesAdapter mScannedDevicesAdapter;
    private OmronPeripheral mSelectedPeripheral;

    private ArrayList<Integer> selectedUsers = new ArrayList<>();

    private RelativeLayout mRlDeviceListView, mRlTransferView;
    private TextView mTvTImeStamp, mTvSpO2, mTvPulseRate;
    private TextView mTvDeviceInfo, mTvDeviceLocalName, mTvDeviceUuid, mTvStatusLabel, mTvErrorCode, mTvErrorDesc;
    private ProgressBar mProgressBar;

    private Button scanBtn;
    private Button transferBtn;

    private PreferencesManager preferencesManager = null;

    HashMap<String, String> device = null;

    private Boolean isScan;

    private final int TIME_INTERVAL = 1000;

    Handler mHandler;
    Runnable mRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oxymeter_main);
        mContext = this;

        isScan = false;

        if (preferencesManager == null)
            preferencesManager = new PreferencesManager(PulseOxymeterMainActivity.this);

        // Selected users
        selectedUsers = (ArrayList<Integer>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_USER);
        if(selectedUsers == null) {
            selectedUsers = new ArrayList<>();
            selectedUsers.add(1);
        }
        // Selected device
        device = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_DEVICE);

        initViews();
        showDeviceListView();
        initClickListeners();
        initLists();

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

        // Set Scan timeout interval (optional)
        peripheralConfig.timeoutInterval = Constants.CONNECTION_TIMEOUT;
        // Set User Hash Id (mandatory)
        peripheralConfig.userHashId = "<email_address_of_user>"; // Set logged in user email

        // Disclaimer: Read definition before usage
        peripheralConfig.enableAllDataRead = isHistoricDataRead;

        // Pass the last sequence number of reading  tracked by app - "SequenceKey" for each vital data
        HashMap<Integer, Integer> sequenceNumbersForTransfer = new HashMap<>();
        sequenceNumbersForTransfer.put(1, 0);
        sequenceNumbersForTransfer.put(2, 0);
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

                                Toast.makeText(PulseOxymeterMainActivity.this, "Error Code : " + resultInfo.getResultCode() + "\nError Detail Code : " + resultInfo.getDetailInfo(), Toast.LENGTH_LONG).show();
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

                            PulseOxymeterMainActivity.this.runOnUiThread(new Runnable() {
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
                        Toast.makeText(PulseOxymeterMainActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show();

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
        startOmronPeripheralManager(true, false);
        performDataTransfer();
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

        // Pulse oxximeter Data
        ArrayList<HashMap<String, Object>> pulseOximeterData = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataPulseOximeterKey);
        if (pulseOximeterData != null) {

            for (HashMap<String, Object> pulseOximeterItem : pulseOximeterData) {
                Log.d("Pulse Oximeter - ", pulseOximeterItem.toString());
            }

            insertVitalDataToDB(pulseOximeterData, deviceInfo);
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
                                    final ArrayList<HashMap<String, Object>> pulseOxymeterItemList = (ArrayList<HashMap<String, Object>>) vitalData.get(OmronConstants.OMRONVitalDataPulseOximeterKey);
                                    if (pulseOxymeterItemList != null) {

                                        showVitalDataResult(pulseOxymeterItemList);
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

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();

        for (HashMap<String, Object> pulseOximeterItem : dataList) {

            ContentValues cv = new ContentValues();

            calendar.setTimeInMillis((Long) pulseOximeterItem.get(OmronConstants.OMRONPulseOximeterData.StartDateKey));

            cv.put(OmronDBConstans.OXYMETER_DATA_StartTimeKey, format.format(calendar.getTime()));
            cv.put(OmronDBConstans.OXYMETER_DATA_SpO2Key, String.valueOf(pulseOximeterItem.get(OmronConstants.OMRONPulseOximeterData.SPO2LevelKey)));
            cv.put(OmronDBConstans.OXYMETER_DATA_PulseKey, String.valueOf(pulseOximeterItem.get(OmronConstants.OMRONPulseOximeterData.PulseRateKey)));
            cv.put(OmronDBConstans.DEVICE_LOCAL_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.LocalNameKey).toLowerCase());
            cv.put(OmronDBConstans.DEVICE_DISPLAY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.DisplayNameKey));
            cv.put(OmronDBConstans.DEVICE_IDENTITY_NAME, deviceInfo.get(OmronConstants.OMRONDeviceInformation.IdentityNameKey));
            cv.put(OmronDBConstans.DEVICE_CATEGORY, device.get(OmronConstants.OMRONBLEConfigDevice.Category));

            Uri uri = getContentResolver().insert(OmronDBConstans.OXYMETER_DATA_CONTENT_URI, cv);
            if (uri != null) {
                //TODO successful insert
            }
        }
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

        //Data transfer
        findViewById(R.id.btn_transfer).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                transferData();
            }
        });

        //Open Pulse Oxymeter data activity
        findViewById(R.id.iv_vital_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedPeripheral != null) {
                    Intent toVitalData = new Intent(PulseOxymeterMainActivity.this, PulseOxymeterDataListingActivity.class);
                    toVitalData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, mSelectedPeripheral.getLocalName());
                    startActivity(toVitalData);
                }
            }
        });
    }

    private void initViews() {

        mLvScannedList = (ListView) findViewById(R.id.lv_scannedlist);
        mTvTImeStamp = (TextView) findViewById(R.id.tv_timestamp_value);
        mTvSpO2 = (TextView) findViewById(R.id.tv_spo2_value);
        mTvPulseRate = (TextView) findViewById(R.id.tv_pulserate_value);

        mTvDeviceInfo = (TextView) findViewById(R.id.device_info);
        mTvDeviceLocalName = (TextView) findViewById(R.id.tv_device_name);
        mTvDeviceUuid = (TextView) findViewById(R.id.tv_device_uuid);
        mTvStatusLabel = (TextView) findViewById(R.id.tv_status_value);
        mTvErrorCode = (TextView) findViewById(R.id.tv_error_value);
        mTvErrorDesc = (TextView) findViewById(R.id.tv_error_desc);

        mRlDeviceListView = (RelativeLayout) findViewById(R.id.rl_device_list);
        mRlTransferView = (RelativeLayout) findViewById(R.id.rl_transfer_view);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_scan);

        scanBtn = (Button) findViewById(R.id.btn_scan);
        transferBtn = (Button) findViewById(R.id.btn_transfer);
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

    private void showVitalDataResult(final ArrayList<HashMap<String, Object>> pulseOxymeterItemList) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (pulseOxymeterItemList.size() == 0) {

                    mTvErrorDesc.setText("No New readings transferred");
                    mTvTImeStamp.setText("-");
                    mTvSpO2.setText("-");
                    mTvPulseRate.setText("-");

                } else {
                    HashMap<String, Object> pulseOxymeterItem = pulseOxymeterItemList.get(pulseOxymeterItemList.size() - 1);

                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis((Long) pulseOxymeterItem.get(OmronConstants.OMRONPulseOximeterData.StartDateKey));
                    
                    mTvErrorDesc.setText("-");
                    mTvTImeStamp.setText(format.format(calendar.getTime()));
                    mTvSpO2.setText(String.valueOf(pulseOxymeterItem.get(OmronConstants.OMRONPulseOximeterData.SPO2LevelKey)) + "\t %");
                    mTvPulseRate.setText(String.valueOf(pulseOxymeterItem.get(OmronConstants.OMRONPulseOximeterData.PulseRateKey)) + "\t bpm");

                }
            }
        });

    }

    private void resetVitalDataResult() {

        mTvTImeStamp.setText("-");
        mTvSpO2.setText("-");
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