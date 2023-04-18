package com.omronhealthcare.OmronConnectivitySample.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivitySample.Database.OmronDBConstans;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.adapter.SettingsListAdapter;
import com.omronhealthcare.OmronConnectivitySample.models.ReminderItem;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;

import java.util.ArrayList;

/**
 * Created by Omron HealthCare Inc
 */
public class SettingsFragment extends Fragment {

    SettingsListAdapter mSettingsListAdapter;
    private Context mContext;
    private RecyclerView lvDevicelist;
    private TextView mTvTimeFormat;
    private String localName;

    public SettingsFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SettingsFragment newInstance(String localName) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, localName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_reminder_listing, container, false);
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
        mContext = getActivity();

        mTvTimeFormat = (TextView) view.findViewById(R.id.tv_time_format);

        lvDevicelist = (RecyclerView) view.findViewById(R.id.lv_devicelist);
        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        lvDevicelist.setLayoutManager(linearLayoutManager);
        fetchReminderDetails();
    }

    private void fetchReminderDetails() {
        String timeFormatValue = null;
        Cursor curs = getActivity().getContentResolver().query(OmronDBConstans.REMINDER_DATA_CONTENT_URI,
                null,OmronDBConstans.DEVICE_LOCAL_NAME+" COLLATE NOCASE = '"+localName+"'",
                null, null);

        ArrayList<ReminderItem> reminderItems = new ArrayList<>();

        if (curs != null) {
            if (curs.getCount() > 0) {
                curs.moveToFirst();
                do {
                    timeFormatValue = curs.getString(curs.getColumnIndex(OmronDBConstans.REMINDER_DATA_TimeFormat));
                    String hour = curs.getString(curs.getColumnIndex(OmronDBConstans.REMINDER_DATA_Hour));
                    String minute = curs.getString(curs.getColumnIndex(OmronDBConstans.REMINDER_DATA_Minute));
                    String days = curs.getString(curs.getColumnIndex(OmronDBConstans.REMINDER_DATA_Days));
                    ReminderItem reminderItem = new ReminderItem(hour, minute, days);
                    reminderItems.add(reminderItem);

                } while (curs.moveToNext());
            }
        }

        if (null != timeFormatValue) {
            mTvTimeFormat.setText(getString(R.string.device_time_format) + timeFormatValue);
        }

        mSettingsListAdapter = new SettingsListAdapter(mContext, reminderItems);
        lvDevicelist.setAdapter(mSettingsListAdapter);


    }


}