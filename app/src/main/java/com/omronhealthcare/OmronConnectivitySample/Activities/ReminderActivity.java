package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.DeviceConfiguration.OmronPeripheralManagerConfig;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Interface.OmronPeripheralManagerUpdateListener;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.LibraryManager.OmronPeripheralManager;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronErrorInfo;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronPeripheral;
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.App;
import com.omronhealthcare.OmronConnectivitySample.Database.OmronDBConstans;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.adapter.ReminderListAdapter;
import com.omronhealthcare.OmronConnectivitySample.models.PeripheralDevice;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;
import com.omronhealthcare.OmronConnectivitySample.utility.PreferencesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Omron HealthCare Inc
 */

/******************************************************************************************************/
/************************ Reminder Functionality for Activity Device / HeartVue ***********************/
/******************************************************************************************************/

public class ReminderActivity extends BaseActivity implements View.OnClickListener {

    PreferencesManager preferencesManager;

    private RelativeLayout activityMain;
    private RelativeLayout topBar;
    private ImageView ivAddDevice;
    private TextView textView;
    private ImageView ivRemoveSavedDevices;
    private RelativeLayout rlDeviceList;
    private RelativeLayout rlTimeFormat;
    private TextView tvTimeFormat;
    private Switch swTimeFormat;
    private RecyclerView rvSavedDevices;
    private LinearLayout llTop;

    private ReminderListAdapter reminderListAdapter;

    private PeripheralDevice peripheralDevice;
    HashMap<String, String> device = null;

    public static final String ARG_DEVICE = "device";

    private int currentSelection = -1;
    private int mSelectedHour = 0;
    private int mSelectedMinute = 0;

    boolean[] currentDaySelection = null;
    private JSONObject currentDaySelectionJSON = null;
    private HashMap<String, String> profileSettings = null;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        mContext = this;
        peripheralDevice = getIntent().getParcelableExtra(ARG_DEVICE);
        try {
            device = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_DEVICE);
        } catch (Exception e) {
        }

        try {
            profileSettings = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_PERSONAL_SETTINGS);
        } catch (Exception e) {
        }

        preferencesManager = new PreferencesManager(ReminderActivity.this);
        reminderListAdapter = new ReminderListAdapter(ReminderActivity.this, preferencesManager.getReminderList(peripheralDevice.getUuid()), new ReminderListAdapter.ReminderSelect() {
            @Override
            public void onTimeSelect(JSONObject reminder, int position) {
                currentSelection = position;
                showTimePicker();
            }

            @Override
            public void onRepeatSelect(JSONObject reminder, int position) {
                currentSelection = position;
                showRepeatSelector(false);
                reminderListAdapter.updateItemList(preferencesManager.getReminderList(peripheralDevice.getUuid()));
            }

            @Override
            public void onDeleteSelect(JSONObject reminder, int position) {
                preferencesManager.removeReminder(peripheralDevice.getUuid(), reminder);
                reminderListAdapter.updateItemList(preferencesManager.getReminderList(peripheralDevice.getUuid()));
            }
        });

        activityMain = (RelativeLayout) findViewById(R.id.activity_main);
        topBar = (RelativeLayout) findViewById(R.id.top_bar);
        ivAddDevice = (ImageView) findViewById(R.id.iv_add_device);
        textView = (TextView) findViewById(R.id.textView);
        ivRemoveSavedDevices = (ImageView) findViewById(R.id.iv_remove_saved_devices);
        rlDeviceList = (RelativeLayout) findViewById(R.id.rl_device_list);
        rlTimeFormat = (RelativeLayout) findViewById(R.id.rl_time_format);
        tvTimeFormat = (TextView) findViewById(R.id.tv_time_format);
        swTimeFormat = (Switch) findViewById(R.id.sw_time_format);
        rvSavedDevices = (RecyclerView) findViewById(R.id.rv_saved_devices);
        llTop = (LinearLayout) findViewById(R.id.ll_top);
        findViewById(R.id.btn_update).setOnClickListener(this);
        ivAddDevice.setOnClickListener(this);

        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(ReminderActivity.this, LinearLayoutManager.VERTICAL, false);
        rvSavedDevices.setLayoutManager(linearLayoutManager);
        rvSavedDevices.addItemDecoration(new DividerItemDecoration(ReminderActivity.this, DividerItemDecoration.VERTICAL));
        rvSavedDevices.setAdapter(reminderListAdapter);

        preferencesManager.getReminderFormat(peripheralDevice.getUuid());

        if (preferencesManager.getReminderFormat(peripheralDevice.getUuid()) == null) {
            preferencesManager.addReminderFormat(peripheralDevice.getUuid(), "24");
        }
        tvTimeFormat.setText(getResources().getString(R.string.time_format, preferencesManager.getReminderFormat(peripheralDevice.getUuid())));

        if (preferencesManager.getReminderFormat(peripheralDevice.getUuid()).equalsIgnoreCase("24")) {
            swTimeFormat.setChecked(false);
            reminderListAdapter.setIsTimeFormat24(true);
        } else {
            swTimeFormat.setChecked(true);
            reminderListAdapter.setIsTimeFormat24(false);
        }

        swTimeFormat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if (isChecked) {
                    preferencesManager.addReminderFormat(peripheralDevice.getUuid(), "12");
                    reminderListAdapter.setIsTimeFormat24(false);
                } else {
                    preferencesManager.addReminderFormat(peripheralDevice.getUuid(), "24");
                    reminderListAdapter.setIsTimeFormat24(true);
                }
                tvTimeFormat.setText(getResources().getString(R.string.time_format, preferencesManager.getReminderFormat(peripheralDevice.getUuid())));
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_update:
                showProgressDialog(getString(R.string.update_reminder));

                OmronPeripheralManagerConfig existingConfig = OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).getConfiguration();
                Log.d("DeviceSettings", "Existing Settings - " + existingConfig.deviceSettings.toString());
                ArrayList<HashMap> existingDeviceSettings =  existingConfig.deviceSettings;


                ArrayList<HashMap> reminders = preferencesManager.getReminderHashMap(peripheralDevice.getUuid());
                if(reminders.size() > 0) {

                    HashMap<String, ArrayList> reminderSettings = new HashMap<>();
                    reminderSettings.put(OmronConstants.OMRONDeviceAlarmSettingsKey, reminders);

                    // Add new configuration to existing configuration
                    existingDeviceSettings.add(reminderSettings);
                }

//
//                HashMap<String, String> timeSettings = new HashMap<String, String>();
//                timeSettings.put(OmronConstants.OMRONDeviceTimeSettings.FormatKey, preferencesManager.getReminderFormat(peripheralDevice.getUuid()));
//                HashMap<String, HashMap> deviceTimeSettings = new HashMap<>();
//                deviceTimeSettings.put(OmronConstants.OMRONDeviceTimeSettingsKey, timeSettings);

                HashMap<String, Object> dateFormatSettings = new HashMap<String, Object>();
                dateFormatSettings.put(OmronConstants.OMRONDeviceDateSettings.FormatKey, OmronConstants.OMRONDeviceDateFormat.DayMonth);
                HashMap<String, HashMap> dateSettings = new HashMap<>();
                dateSettings.put(OmronConstants.OMRONDeviceDateSettingsKey, dateFormatSettings);


                // Time Format
                HashMap<String, Object> timeFormatSettings = new HashMap<String, Object>();
                timeFormatSettings.put(OmronConstants.OMRONDeviceTimeSettings.FormatKey, OmronConstants.OMRONDeviceTimeFormat.Time24Hour);
                HashMap<String, HashMap> timeSettings = new HashMap<>();
                timeSettings.put(OmronConstants.OMRONDeviceTimeSettingsKey, timeFormatSettings);


                ArrayList<HashMap> deviceSettings = new ArrayList<>();
                existingDeviceSettings.add(timeSettings);


                Log.d("DeviceSettings", "New Settings - " + existingDeviceSettings.toString());

                OmronPeripheralManagerConfig newConfig = new OmronPeripheralManagerConfig();
                newConfig.deviceSettings = existingDeviceSettings;

                // Update Configuration
                OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).setConfiguration(newConfig);

                if (null != peripheralDevice) {

                    OmronPeripheral peripheral = new OmronPeripheral(peripheralDevice.getLocalName(), peripheralDevice.getUuid());

                    //Call to update the settings
                    OmronPeripheralManager.sharedManager(App.getInstance().getApplicationContext()).updatePeripheral(peripheral, new OmronPeripheralManagerUpdateListener() {
                        @Override
                        public void onUpdateCompleted(final OmronPeripheral peripheral, final OmronErrorInfo resultInfo) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    hideProgressDialog();
                                    if (resultInfo.getResultCode() == 0) {
                                        showMessage(getString(R.string.update_success), getString(R.string.update_settings_sucess));
                                    } else {
                                        int erroCode = resultInfo.getResultCode();
                                        String detailInfo = resultInfo.getDetailInfo();
                                        showMessage(getString(R.string.update_failed), "Error Code : " + erroCode + "\n" + detailInfo);
                                    }
                                }
                            });

                        }
                    });


                }

                break;
            case R.id.iv_add_device:
                if (reminderListAdapter.getItemCount() >= 5) {
                    Toast.makeText(ReminderActivity.this, "You can add only 5 reminders", Toast.LENGTH_SHORT).show();
                } else {
                    currentSelection = -1;
                    showTimePicker();
                }
                break;
        }
    }

    private void showTimePicker() {
        int mHour = 0;
        int mMinute = 0;
        Log.d("showTimePicker", "showTimePicker: ");

        if (currentSelection == -1) {
            Calendar calendar = Calendar.getInstance();
            mHour = calendar.get(Calendar.HOUR_OF_DAY);
            mMinute = calendar.get(Calendar.MINUTE);
        } else {
            JSONObject jsonObject = reminderListAdapter.getItem(currentSelection);
            try {
                mHour = jsonObject.getInt(PreferencesManager.JSON_KEY_REMINDER_TIME_HOUR);
                mMinute = jsonObject.getInt(PreferencesManager.JSON_KEY_REMINDER_TIME_MINUTE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(ReminderActivity.this, new TimePickerDialog.OnTimeSetListener() {

            int callCount = 0;

            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                if (callCount == 0) {
                    Log.d("Time", selectedHour + ":" + selectedMinute);
                    mSelectedHour = selectedHour;
                    mSelectedMinute = selectedMinute;
                    showRepeatSelector(true);
                }
                callCount++;
            }
        }, mHour, mMinute, !swTimeFormat.isChecked());
        mTimePicker.show();
    }

    private void showRepeatSelector(boolean isFromTimePicker) {

        JSONObject jsonObject = null;

        if (isFromTimePicker) {

        } else {

            if (currentSelection == -1)
                return;

            try {
                jsonObject = reminderListAdapter.getItem(currentSelection);
                mSelectedHour = jsonObject.getInt(PreferencesManager.JSON_KEY_REMINDER_TIME_HOUR);
                mSelectedMinute = jsonObject.getInt(PreferencesManager.JSON_KEY_REMINDER_TIME_MINUTE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (currentSelection == -1) {
            currentDaySelection = new boolean[]{true, true, true, true, true, true, true};
            currentDaySelectionJSON = new JSONObject();
            try {
                currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.SundayKey, 1);
                currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.MondayKey, 1);
                currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey, 1);
                currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey, 1);
                currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey, 1);
                currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.FridayKey, 1);
                currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey, 1);
            } catch (JSONException e) {
            }
        } else {
            currentDaySelection = new boolean[7];
            try {
                if (jsonObject == null)
                    jsonObject = reminderListAdapter.getItem(currentSelection);

                currentDaySelectionJSON = jsonObject.getJSONObject(PreferencesManager.JSON_KEY_REMINDER_REPEAT);
                if (currentDaySelectionJSON.getInt(OmronConstants.OMRONDeviceAlarmSettings.SundayKey) == 1) {
                    currentDaySelection[0] = true;
                } else {
                    currentDaySelection[0] = false;
                }
                if (currentDaySelectionJSON.getInt(OmronConstants.OMRONDeviceAlarmSettings.MondayKey) == 1) {
                    currentDaySelection[1] = true;
                } else {
                    currentDaySelection[1] = false;
                }
                if (currentDaySelectionJSON.getInt(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey) == 1) {
                    currentDaySelection[2] = true;
                } else {
                    currentDaySelection[2] = false;
                }
                if (currentDaySelectionJSON.getInt(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey) == 1) {
                    currentDaySelection[3] = true;
                } else {
                    currentDaySelection[3] = false;
                }
                if (currentDaySelectionJSON.getInt(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey) == 1) {
                    currentDaySelection[4] = true;
                } else {
                    currentDaySelection[4] = false;
                }
                if (currentDaySelectionJSON.getInt(OmronConstants.OMRONDeviceAlarmSettings.FridayKey) == 1) {
                    currentDaySelection[5] = true;
                } else {
                    currentDaySelection[5] = false;
                }
                if (currentDaySelectionJSON.getInt(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey) == 1) {
                    currentDaySelection[6] = true;
                } else {
                    currentDaySelection[6] = false;
                }
            } catch (JSONException e) {
                currentDaySelection = new boolean[]{true, true, true, true, true, true, true};
                currentDaySelectionJSON = new JSONObject();
                try {
                    currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.SundayKey, 1);
                    currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.MondayKey, 1);
                    currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey, 1);
                    currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey, 1);
                    currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey, 1);
                    currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.FridayKey, 1);
                    currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey, 1);
                } catch (JSONException e1) {
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Reminder")
                .setMultiChoiceItems(R.array.days, currentDaySelection, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                        try {
                            switch (which) {
                                case 0:
                                    if (isChecked) {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.SundayKey, 1);
                                        currentDaySelection[0] = true;
                                    } else {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.SundayKey, 0);
                                        currentDaySelection[0] = false;
                                    }
                                    break;
                                case 1:
                                    if (isChecked) {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.MondayKey, 1);
                                        currentDaySelection[0] = true;
                                    } else {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.MondayKey, 0);
                                        currentDaySelection[0] = false;
                                    }
                                    break;
                                case 2:
                                    if (isChecked) {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey, 1);
                                        currentDaySelection[0] = true;
                                    } else {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey, 0);
                                        currentDaySelection[0] = false;
                                    }
                                    break;
                                case 3:
                                    if (isChecked) {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey, 1);
                                        currentDaySelection[0] = true;
                                    } else {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey, 0);
                                        currentDaySelection[0] = false;
                                    }
                                    break;
                                case 4:
                                    if (isChecked) {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey, 1);
                                        currentDaySelection[0] = true;
                                    } else {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey, 0);
                                        currentDaySelection[0] = false;
                                    }
                                    break;
                                case 5:
                                    if (isChecked) {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.FridayKey, 1);
                                        currentDaySelection[0] = true;
                                    } else {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.FridayKey, 0);
                                        currentDaySelection[0] = false;
                                    }
                                    break;
                                case 6:
                                    if (isChecked) {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey, 1);
                                        currentDaySelection[0] = true;
                                    } else {
                                        currentDaySelectionJSON.put(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey, 0);
                                        currentDaySelection[0] = false;
                                    }
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean flagAtleastOneDaySelected = false;
                        for (boolean value : currentDaySelection) {
                            if (value) {
                                flagAtleastOneDaySelected = true;
                                break;
                            }
                        }

                        if (flagAtleastOneDaySelected)
                            saveReminder();
                        else
                            Toast.makeText(ReminderActivity.this, "Select atleast one day", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).create().show();
    }

    private void saveReminder() {
        JSONObject jsonObject = null;
        if (currentSelection == -1) {
            jsonObject = new JSONObject();
        } else {
            jsonObject = reminderListAdapter.getItem(currentSelection);
        }
        try {
            jsonObject.put(PreferencesManager.JSON_KEY_REMINDER_TIME_HOUR, mSelectedHour);
            jsonObject.put(PreferencesManager.JSON_KEY_REMINDER_TIME_MINUTE, mSelectedMinute);
            jsonObject.put(PreferencesManager.JSON_KEY_REMINDER_REPEAT, currentDaySelectionJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        preferencesManager.addReminder(peripheralDevice.getUuid(), jsonObject);
        reminderListAdapter.updateItemList(preferencesManager.getReminderList(peripheralDevice.getUuid()));
    }

    public void showMessage(String title, String message) {

        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                    }
                });


        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    //Insert new reminders to local DB
    private void insertReminderDetails(String localName, ArrayList<HashMap> deviceSettings) {

        int i = getContentResolver().delete(OmronDBConstans.REMINDER_DATA_CONTENT_URI, OmronDBConstans.REMINDER_DATA_DEVICE_LOCAL_NAME + "=? ", new String[]{localName});
        HashMap<String, String> timeSettings = null;
        ArrayList<HashMap> reminders = null;
        for (HashMap item : deviceSettings) {
            for (Object key : item.keySet()) {
                if (key.toString().equalsIgnoreCase(OmronConstants.OMRONDeviceAlarmSettingsKey)) {
                    reminders = (ArrayList<HashMap>) item.get(key);
                }

//                else if (key.toString().equalsIgnoreCase(OmronConstants.OMRONDeviceDisplaySettingsKey)) {
//                    timeSettings = (HashMap<String, String>) item.get(key);
//                }
            }
        }

        String timeformat = ""; //timeSettings.get(OmronConstants.OMRONDeviceDisplaySettings.TimeFormatKey);
        for (HashMap<String, HashMap> item : reminders) {

            HashMap<String, String> reminderTime = item.get(OmronConstants.OMRONDeviceAlarmSettings.TimeKey);
            HashMap<String, String> reminderDays = item.get(OmronConstants.OMRONDeviceAlarmSettings.DaysKey);
            String hour = reminderTime.get(OmronConstants.OMRONDeviceAlarmSettings.HourKey);
            String minute = reminderTime.get(OmronConstants.OMRONDeviceAlarmSettings.MinuteKey);
            String days = "";
            for (String key : reminderDays.keySet()) {
                if (days.length() != 0) {
                    days += ", ";
                }
                if (reminderDays.get(key).equals("1")) {
                    days += key;
                }
            }

            ContentValues cv = new ContentValues();
            cv.put(OmronDBConstans.REMINDER_DATA_DEVICE_LOCAL_NAME, localName);
            cv.put(OmronDBConstans.REMINDER_DATA_TimeFormat, timeformat);
            cv.put(OmronDBConstans.REMINDER_DATA_Hour, hour);
            cv.put(OmronDBConstans.REMINDER_DATA_Minute, minute);
            cv.put(OmronDBConstans.REMINDER_DATA_Days, days);

            Uri uri = getContentResolver().insert(OmronDBConstans.REMINDER_DATA_CONTENT_URI, cv);
        }
    }

}
