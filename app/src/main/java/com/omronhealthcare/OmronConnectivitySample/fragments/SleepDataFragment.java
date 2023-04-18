package com.omronhealthcare.OmronConnectivitySample.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivitySample.Database.OmronDBConstans;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.adapter.SleepDataListAdapter;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;

/**
 * Created by Omron HealthCare Inc
 */

public class SleepDataFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    SleepDataListAdapter sleepDataListAdapter;
    private Context mContext;

    private static final int SLEEP_LOADER_ID = 1;

    private RecyclerView lvDevicelist;

    private String localName;

    public SleepDataFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SleepDataFragment newInstance(String localName) {
        SleepDataFragment fragment = new SleepDataFragment();
        Bundle args = new Bundle();
        args.putString(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, localName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sleep_data, container, false);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            localName = getArguments().getString(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME).toLowerCase();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvDevicelist = (RecyclerView) view.findViewById(R.id.lv_devicelist);
        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        lvDevicelist.setLayoutManager(linearLayoutManager);
        sleepDataListAdapter = new SleepDataListAdapter(mContext);
        lvDevicelist.setAdapter(sleepDataListAdapter);
        getLoaderManager().initLoader(SLEEP_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new AsyncTaskLoader<Cursor>(getActivity()) {

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
                    return getActivity().getContentResolver().query(OmronDBConstans.SLEEP_DATA_CONTENT_URI,
                            null,
                            OmronDBConstans.DEVICE_LOCAL_NAME + "=? ",
                            new String[]{localName},
                            OmronDBConstans.SLEEP_DATA_INDEX + " DESC");

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

        sleepDataListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        sleepDataListAdapter.swapCursor(null);
    }
}