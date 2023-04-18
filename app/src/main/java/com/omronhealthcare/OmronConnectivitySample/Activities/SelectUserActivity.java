package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;
import com.omronhealthcare.OmronConnectivitySample.utility.Utilities;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Omron HealthCare Inc
 */
public class SelectUserActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "SelectUserActivity";
    SwitchCompat switchCompat;
    SwitchCompat switchCompat2;
    SwitchCompat switchCompat3;
    SwitchCompat switchCompat4;
    HashMap<String, String> device;

    private LinearLayout llUserSelection;
    private LinearLayout llUserDetails;

    private Spinner spHeightGreater;
    private Spinner spHeightLower;
    private Spinner spWeight;
    private Spinner spStrideGreater;
    private Spinner spStrideLower;
    private RelativeLayout rlUser3;
    private RelativeLayout rlUser4;

    private ArrayList<Integer> selectedUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user);

        device = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_DEVICE);

        // Activity Tracker
        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.ACTIVITY) {
            //Show user details selection screen for activity device.
            initViewUserDetails();
        } else {
            // Blood Pressure device - show user selection only.
            initViewUserSelection();
        }
    }

    private void setTopBar(@StringRes int topBar) {
        TextView tvTitle = (TextView) findViewById(R.id.tv_title);
        tvTitle.setText(topBar);
    }

    private void initViewUserSelection() {
        setTopBar(R.string.select_user);
        llUserSelection = (LinearLayout) findViewById(R.id.ll_user_selection);
        llUserSelection.setVisibility(View.VISIBLE);
        switchCompat = (SwitchCompat) findViewById(R.id
                .switch_user1);

        switchCompat.setOnCheckedChangeListener(this);

        switchCompat2 = (SwitchCompat) findViewById(R.id
                .switch_user2);
        switchCompat2.setSwitchPadding(40);
        switchCompat2.setOnCheckedChangeListener(this);
        rlUser3 = findViewById(R.id.rel_user_3);
        rlUser4 = findViewById(R.id.rel_user_4);

        if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Users)) == 4) {
            rlUser3.setVisibility(View.VISIBLE);
            rlUser4.setVisibility(View.VISIBLE);
            switchCompat3 = (SwitchCompat) findViewById(R.id
                    .switch_user3);
            switchCompat3.setSwitchPadding(40);
            switchCompat3.setOnCheckedChangeListener(this);

            switchCompat4 = (SwitchCompat) findViewById(R.id
                    .switch_user4);
            switchCompat4.setSwitchPadding(40);
            switchCompat4.setOnCheckedChangeListener(this);
        } else {
            rlUser3.setVisibility(View.GONE);
            rlUser4.setVisibility(View.GONE);

        }


        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedUsers.size() == 0) {
                    showToastMessage("Please select a user");
                } else {
                    // BCM device
                    if (Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Category)) == OmronConstants.OMRONBLEDeviceCategory.BODYCOMPOSITION) {
                        Intent toMain = new Intent(SelectUserActivity.this, UserPersonalSettingsActivity.class);
                        toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, device);
                        toMain.putExtra(Constants.extraKeys.KEY_SELECTED_USER, selectedUsers);
                        startActivity(toMain);
                    } else {
                        // BP device
                        Intent toMain = new Intent(SelectUserActivity.this, MainActivity.class);
                        toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, device);
                        toMain.putExtra(Constants.extraKeys.KEY_SELECTED_USER, selectedUsers);
                        startActivity(toMain);
                    }
                }
            }
        });
    }

    private void initViewUserDetails() {

        llUserDetails = (LinearLayout) findViewById(R.id.ll_user_details);
        llUserDetails.setVisibility(View.VISIBLE);
        setTopBar(R.string.enter_user_details);

        spHeightGreater = (Spinner) findViewById(R.id.sp_height_greater);
        spHeightLower = (Spinner) findViewById(R.id.sp_height_lower);
        spWeight = (Spinner) findViewById(R.id.sp_weight);
        spStrideGreater = (Spinner) findViewById(R.id.sp_stride_greater);
        spStrideLower = (Spinner) findViewById(R.id.sp_stride_lower);

        spWeight.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spHeightLower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spHeightGreater.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayList<Integer> spinnerArray = new ArrayList<Integer>();
        for (int i = 75; i <= 400; i++)
            spinnerArray.add(i);
        ArrayAdapter<Integer> spinnerArrayAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        spWeight.setAdapter(spinnerArrayAdapter);
        spWeight.setSelection(81);
        spStrideGreater.setSelection(2);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int height_ft = Integer.parseInt((String) spHeightGreater.getSelectedItem());
                int height_inch = Integer.parseInt((String) spHeightLower.getSelectedItem());

                int stride_ft = Integer.parseInt((String) spStrideGreater.getSelectedItem());
                int stride_inch = Integer.parseInt((String) spStrideLower.getSelectedItem());

                int weight_lbs = (int) spWeight.getSelectedItem();

                float weight_kg = Utilities.convertlbToKg(weight_lbs);
                float stride_cm = Utilities.convertFeetInchToCm(stride_ft, stride_inch);
                float height_cm = Utilities.convertFeetInchToCm(height_ft, height_inch);

                if (weight_kg <= 0) {

                    showToastMessage("Please select weight");

                } else if (stride_cm <= 0) {

                    showToastMessage("Please select stride");

                } else if (height_cm <= 0) {

                    showToastMessage("Please select height");

                } else {

                    String height = String.valueOf((int) (Utilities.round(height_cm, 2) * 100));
                    String weight = String.valueOf((int) (Utilities.round(weight_kg, 2) * 100));
                    String stride = String.valueOf((int) (Utilities.round(stride_cm, 2) * 100));

                    // Set Height, Weight and Stride to OmronPeripheralManagerConfig
                    HashMap<String, String> settingsModel = new HashMap<String, String>();
                    settingsModel.put("personalHeight", height);
                    settingsModel.put("personalWeight", weight);
                    settingsModel.put("personalStride", stride);

                    Log.d(TAG, settingsModel.toString());

                    Intent toMain = new Intent(SelectUserActivity.this, MainActivity.class);
                    toMain.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, device);
                    toMain.putExtra(Constants.extraKeys.KEY_PERSONAL_SETTINGS, settingsModel);
                    startActivity(toMain);
                }
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int index = -1;
        switch (buttonView.getId()) {
            case R.id.switch_user1:
                index = 1;
                break;
            case R.id.switch_user2:
                index = 2;
                break;
            case R.id.switch_user3:
                index = 3;
                break;
            case R.id.switch_user4:
                index = 4;
                break;
        }
        if (isChecked) {
            selectedUsers.add(index);
        } else {
            selectedUsers.remove((Integer) index);
        }
    }

    private void showToastMessage(String s) {
        Toast.makeText(SelectUserActivity.this, s, Toast.LENGTH_SHORT).show();
    }
}