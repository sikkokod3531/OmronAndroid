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
import com.omronhealthcare.OmronConnectivitySample.adapter.PulseOxymeterDataListAdapter;
import com.omronhealthcare.OmronConnectivitySample.adapter.VitalDataListAdapter;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;

/**
 * Created by Omron HealthCare Inc
 */
public class PulseOxymeterDataListingActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mListView;
    PulseOxymeterDataListAdapter mOxymeterDataListAdapter;
    private Context mContext;

    private static final int OXYMETER_LOADER_ID = 8;

    private TextView OxymeterDataCount;
    private String localName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_oxymeter_data_listing);

        try {
            localName = getIntent().getStringExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME).toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mContext = this;

        initViews();

        mOxymeterDataListAdapter = new PulseOxymeterDataListAdapter(mContext);
        mListView.setAdapter(mOxymeterDataListAdapter);
        getSupportLoaderManager().initLoader(OXYMETER_LOADER_ID, null, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSupportLoaderManager().destroyLoader(OXYMETER_LOADER_ID);
    }

    private void initViews() {

        OxymeterDataCount = (TextView) findViewById(R.id.oxymeter_data_count);

        mListView = (RecyclerView) findViewById(R.id.lv_devicelist);
        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(PulseOxymeterDataListingActivity.this, LinearLayoutManager.VERTICAL, false);
        mListView.setLayoutManager(linearLayoutManager);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new AsyncTaskLoader<Cursor>(PulseOxymeterDataListingActivity.this) {

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
                    return getContentResolver().query(OmronDBConstans.OXYMETER_DATA_CONTENT_URI,
                            null,
                            OmronDBConstans.DEVICE_LOCAL_NAME + "=? ",
                            new String[]{localName},
                            OmronDBConstans.OXYMETER_DATA_INDEX + " DESC");

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

        OxymeterDataCount.setText("Pulse Oxymeter Data Count : " + data.getCount());
        mOxymeterDataListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mOxymeterDataListAdapter.swapCursor(null);
    }
}
