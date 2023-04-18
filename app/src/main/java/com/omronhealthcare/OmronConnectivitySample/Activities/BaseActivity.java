package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.app.ProgressDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class BaseActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeProgressDialog();

    }

    private void initializeProgressDialog() {
        try {
            if (android.os.Build.VERSION.SDK_INT > 10) {
                mProgressDialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
            } else {
                mProgressDialog = new ProgressDialog(this);
            }
            mProgressDialog.setCancelable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showProgressDialog(String message) {
        try {
            mProgressDialog.setMessage(message);
            if (mProgressDialog != null) {
                mProgressDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideProgressDialog() {
        try {
            if (mProgressDialog.isShowing() && mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
