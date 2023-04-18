package com.omronhealthcare.OmronConnectivitySample.Activities;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivitySample.Database.OmronDBConstans;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.adapter.VitalDataListAdapter;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;

/**
 * Created by Omron HealthCare Inc
 */
public class VitalDataListingActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mListView;
    VitalDataListAdapter mVitalDataListAdapter;
    private Context mContext;

    private static final int VITAL_LOADER_ID = 1;

    private TextView vitalCount;
    private String localName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vital_data_listing);

        try {
            localName = getIntent().getStringExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME).toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mContext = this;

        initViews();

        mVitalDataListAdapter = new VitalDataListAdapter(mContext);
        mListView.setAdapter(mVitalDataListAdapter);
        getSupportLoaderManager().initLoader(VITAL_LOADER_ID, null, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSupportLoaderManager().destroyLoader(VITAL_LOADER_ID);
    }

    private void initViews() {

        vitalCount = (TextView) findViewById(R.id.vital_count);

        mListView = (RecyclerView) findViewById(R.id.lv_devicelist);
        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(VitalDataListingActivity.this, LinearLayoutManager.VERTICAL, false);
        mListView.setLayoutManager(linearLayoutManager);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new AsyncTaskLoader<Cursor>(VitalDataListingActivity.this) {

            Cursor mTaskData = null;

            @Override
            protected void onStartLoading() {
                if (mTaskData != null) {
                    deliverResult(mTaskData);
                } else {
                    forceLoad();
                }
            }

            @Override
            public Cursor loadInBackground() {
                try {
                    return getContentResolver().query(OmronDBConstans.VITAL_DATA_CONTENT_URI,
                            null,
                            OmronDBConstans.DEVICE_LOCAL_NAME + "=? ",
                            new String[]{localName},
                            OmronDBConstans.VITAL_DATA_INDEX + " DESC");

                } catch (Exception e) {
                    return null;
                }
            }

            public void deliverResult(Cursor data) {
                mTaskData = data;
                super.deliverResult(data);
            }
        };

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        vitalCount.setText("Vital Data Count : " + data.getCount());
        mVitalDataListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVitalDataListAdapter.swapCursor(null);
    }
}
