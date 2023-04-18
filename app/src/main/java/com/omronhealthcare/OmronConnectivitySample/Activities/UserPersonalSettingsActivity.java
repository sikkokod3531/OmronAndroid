package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;
import com.omronhealthcare.OmronConnectivitySample.utility.PreferencesManager;
import com.omronhealthcare.OmronConnectivitySample.utility.Utilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class UserPersonalSettingsActivity extends AppCompatActivity {
    private Spinner spWeightUnit;
    private Spinner spGender;
    private TextView tvDateOfBirth;
    private EditText etHeight;
    private EditText etDCI;
    private String dateString;
    private Button btSet;
    final Calendar calendar = Calendar.getInstance();
    private boolean isUpdate;
    private PreferencesManager preferencesManager;
    private LinearLayout viewDOB, viewHeight;
    private RelativeLayout viewGender;
    HashMap<String, String> device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_personal_settings);
        spWeightUnit = (Spinner) findViewById(R.id.sp_unit);
        tvDateOfBirth = findViewById(R.id.tv_date);
        btSet = findViewById(R.id.bt_set);
        spGender = findViewById(R.id.sp_gender);
        etHeight = findViewById(R.id.et_height);
        etDCI = findViewById(R.id.et_dci);
        viewDOB = findViewById(R.id.view_dob);
        viewHeight = findViewById(R.id.view_height);
        viewGender = findViewById(R.id.rel_gender);

        device = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_DEVICE);

        if(device != null) {
            int noOfUsers = Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Users));
            if (noOfUsers == 1) {
                // Hide options
                viewDOB.setVisibility(View.GONE);
                viewHeight.setVisibility(View.GONE);
                viewGender.setVisibility(View.GONE);
            }
        }



        preferencesManager = new PreferencesManager(UserPersonalSettingsActivity.this);
        etDCI.setText(String.valueOf(-1));
        isUpdate = getIntent().getBooleanExtra(Constants.bundleKeys.KEY_BUNDLE_IS_WEIGHT_UPDATE, false);

        if (isUpdate) {
            btSet.setText("Update");
        } else {
            btSet.setText("Set");
        }
        ArrayList<String> spinnerArray = new ArrayList<String>();
        spinnerArray.add("Kg");
        spinnerArray.add("Lbs");
        spinnerArray.add("St");
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        spWeightUnit.setAdapter(spinnerArrayAdapter);

        ArrayList<String> genderArray = new ArrayList<String>();
        genderArray.add("Male");
        genderArray.add("Female");
        ArrayAdapter<String> genderArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, genderArray);
        spGender.setAdapter(genderArrayAdapter);

        tvDateOfBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(UserPersonalSettingsActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);
                        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                        dateString = format.format(calendar.getTime());
                        tvDateOfBirth.setText(dateString);
                    }
                }, calendar
                        .get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        btSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int noOfUsers = 0;
                if(device != null) {
                    noOfUsers = Integer.parseInt(device.get(OmronConstants.OMRONBLEConfigDevice.Users));
                }
                if (noOfUsers > 1 && etHeight.getText().toString().isEmpty()) {
                    Toast.makeText(UserPersonalSettingsActivity.this, "Please enter height", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!etDCI.getText().toString().trim().isEmpty()) {
                    preferencesManager.saveDCIValue(Long.parseLong(etDCI.getText().toString()));
                } else {
                    preferencesManager.saveDCIValue(-1);
                }
                HashMap<String, String> device = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_DEVICE);
                ArrayList<Integer> selectedUsers = (ArrayList<Integer>) getIntent().getSerializableExtra(Constants.extraKeys.KEY_SELECTED_USER);

                Intent intent;
                if (isUpdate) {
                    intent = getIntent();
                } else {
                    intent = new Intent(UserPersonalSettingsActivity.this, WeightScaleMainActivity.class);
                }

                Bundle bundle = new Bundle();
                bundle.putString(Constants.bundleKeys.KEY_BUNDLE_WEIGHT_UNIT, spWeightUnit.getSelectedItem().toString());
                if (noOfUsers > 1) {
                    float height_cm = Float.valueOf(etHeight.getText().toString());
                    String height = String.valueOf((int) (Utilities.round(height_cm, 2) * 100));
                    bundle.putString(Constants.bundleKeys.KEY_BUNDLE_DOB, dateString);
                    bundle.putString(Constants.bundleKeys.KEY_BUNDLE_GENDER, spGender.getSelectedItem().toString());
                    bundle.putString(Constants.bundleKeys.KEY_BUNDLE_HEIGHT_CM, height);
                }
                intent.putExtra(Constants.extraKeys.KEY_WEIGHT_SETTINGS, bundle);

                if (isUpdate) {
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    intent.putExtra(Constants.extraKeys.KEY_SELECTED_DEVICE, device);
                    intent.putExtra(Constants.extraKeys.KEY_SELECTED_USER, selectedUsers);
                    startActivity(intent);
                }
            }
        });
    }

}
