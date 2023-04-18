package com.omronhealthcare.OmronConnectivitySample.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.adapter.ActivityItemsListAdapter;
import com.omronhealthcare.OmronConnectivitySample.models.ActivityDataItem;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;

import java.util.ArrayList;


/**
 * Created by Omron HealthCare Inc
 */
public class ActivityItemFragment extends Fragment {

    ActivityItemsListAdapter activityDataListAdapter;
    private Context mContext;

    private static final int ACTIVITY_LOADER_ID = 2;

    private RelativeLayout dataCount;
    private TextView tvDataCount;
    private RecyclerView lvDevicelist;
    private ArrayList<ActivityDataItem> activityDataItemArrayList;

    private String localName;

    public ActivityItemFragment() {
        activityDataItemArrayList = new ArrayList<>();
        initList();
    }

    private void initList() {

        ActivityDataItem activityDataItem;
        activityDataItem = new ActivityDataItem(OmronConstants.OMRONActivityData.StepsPerDay, "Steps");
        activityDataItemArrayList.add(activityDataItem);
        activityDataItem = new ActivityDataItem(OmronConstants.OMRONActivityData.AerobicStepsPerDay, "Aerobics Steps");
        activityDataItemArrayList.add(activityDataItem);
        activityDataItem = new ActivityDataItem(OmronConstants.OMRONActivityData.WalkingCaloriesPerDay, "Walking Calories");
        activityDataItemArrayList.add(activityDataItem);
        activityDataItem = new ActivityDataItem(OmronConstants.OMRONActivityData.DistancePerDay, "Distance");
        activityDataItemArrayList.add(activityDataItem);

    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ActivityItemFragment newInstance(String localName) {
        ActivityItemFragment fragment = new ActivityItemFragment();
        Bundle args = new Bundle();
        args.putString(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, localName);
        fragment.setArguments(args);
        return fragment;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContext = getActivity();
        View rootView = inflater.inflate(R.layout.fragment_activity_item, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvDevicelist = (RecyclerView) view.findViewById(R.id.lv_devicelist);
        LinearLayoutManager linearLayoutManager
                = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        lvDevicelist.setLayoutManager(linearLayoutManager);
        activityDataListAdapter = new ActivityItemsListAdapter(mContext, activityDataItemArrayList, localName);
        lvDevicelist.setAdapter(activityDataListAdapter);
    }


}