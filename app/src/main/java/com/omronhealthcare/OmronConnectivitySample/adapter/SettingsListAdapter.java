package com.omronhealthcare.OmronConnectivitySample.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.models.ReminderItem;

import java.util.ArrayList;

/**
 * Created by Omron HealthCare Inc
 */

public class SettingsListAdapter extends RecyclerView.Adapter<SettingsListAdapter.VersionViewHolder> {

    private final Context mContext;
    private final ArrayList<ReminderItem> mReminderItems;


    public SettingsListAdapter(Context context, ArrayList<ReminderItem> items) {
        this.mContext = context;
        this.mReminderItems = items;
    }

    @Override
    public VersionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_settings, parent, false);
        VersionViewHolder viewHolder = new VersionViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VersionViewHolder versionViewHolder, final int position) {
        ReminderItem item = mReminderItems.get(position);

        versionViewHolder.tvReminderTime.setText(item.getHour() + ":" + item.getMinute());
        versionViewHolder.tvRepeat.setText(item.getDays());

    }

    @Override
    public int getItemCount() {
        return mReminderItems.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public ReminderItem getItem(int position) {
        return mReminderItems.get(position);

    }


    class VersionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvReminderTime;
        private TextView tvRepeat;

        public VersionViewHolder(View view) {
            super(view);
            tvReminderTime = (TextView) view.findViewById(R.id.tv_reminder_time);
            tvRepeat = (TextView) view.findViewById(R.id.tv_repeat);

        }
    }
}
