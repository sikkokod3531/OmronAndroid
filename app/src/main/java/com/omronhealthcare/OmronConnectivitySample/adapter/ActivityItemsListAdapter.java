package com.omronhealthcare.OmronConnectivitySample.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivitySample.Activities.ActivityDataActivity;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.models.ActivityDataItem;
import com.omronhealthcare.OmronConnectivitySample.utility.Constants;

import java.util.ArrayList;

/**
 * Created by Omron HealthCare Inc
 */
public class ActivityItemsListAdapter extends RecyclerView.Adapter<ActivityItemsListAdapter.VersionViewHolder> {

    private final Context context;
    Cursor mCursor;
    ArrayList<ActivityDataItem> mActivityDataItemArrayList;
    String localName;

    public ActivityItemsListAdapter(Context context, ArrayList<ActivityDataItem> activityDataItemArrayList, String localName) {
        this.context = context;
        mActivityDataItemArrayList = activityDataItemArrayList;
        this.localName = localName;
    }

    @Override
    public VersionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_activity_data, parent, false);
        VersionViewHolder viewHolder = new VersionViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VersionViewHolder versionViewHolder, final int position) {

        versionViewHolder.tvData.setText(mActivityDataItemArrayList.get(position).getName());
        versionViewHolder.linearLayout1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent toActivityData = new Intent(context, ActivityDataActivity.class);
                toActivityData.putExtra(Constants.bundleKeys.KEY_ACTIVITY_DATA_KEY, mActivityDataItemArrayList.get(position).getKey());
                toActivityData.putExtra(Constants.bundleKeys.KEY_ACTIVITY_DATA_TYPE, mActivityDataItemArrayList.get(position).getName());
                toActivityData.putExtra(Constants.extraKeys.KEY_DEVICE_LOCAL_NAME, localName);
                context.startActivity(toActivityData);


            }
        });

    }

    @Override
    public int getItemCount() {
        if (mActivityDataItemArrayList == null) {
            return 0;
        }
        return (mActivityDataItemArrayList.size());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    class VersionViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout linearLayout1;
        private LinearLayout llBg;
        private TextView tvData;

        public VersionViewHolder(View view) {
            super(view);
            linearLayout1 = (RelativeLayout) view.findViewById(R.id.linearLayout1);
            llBg = (LinearLayout) view.findViewById(R.id.ll_bg);
            tvData = (TextView) view.findViewById(R.id.tv_data);
        }
    }


}
