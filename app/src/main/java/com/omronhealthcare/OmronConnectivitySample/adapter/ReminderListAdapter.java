package com.omronhealthcare.OmronConnectivitySample.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.utility.PreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Omron HealthCare Inc
 */

public class ReminderListAdapter extends RecyclerView.Adapter<ReminderListAdapter.VersionViewHolder> {

    private final Context context;
    JSONArray peripheralDevices;
    ReminderSelect mSavedItemSelect;
    Boolean isTimeFormat24 = true;

    public ReminderListAdapter(Context context, JSONArray peripheralDevices, ReminderSelect savedItemSelect) {
        this.context = context;
        this.peripheralDevices = peripheralDevices;
        this.mSavedItemSelect = savedItemSelect;
    }

    @Override
    public VersionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_reminder, parent, false);
        VersionViewHolder viewHolder = new VersionViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VersionViewHolder versionViewHolder, final int position) {
        try {
            int hour = peripheralDevices.getJSONObject(position).getInt(PreferencesManager.JSON_KEY_REMINDER_TIME_HOUR);
            int minute = peripheralDevices.getJSONObject(position).getInt(PreferencesManager.JSON_KEY_REMINDER_TIME_MINUTE);
            versionViewHolder.tvReminderTime.setText(getDisplayTime(hour, minute));
            versionViewHolder.tvRepeat.setText(getDisplayReminder(peripheralDevices.getJSONObject(position).getJSONObject(PreferencesManager.JSON_KEY_REMINDER_REPEAT)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        if (peripheralDevices == null) {
            return 0;
        }
        return (peripheralDevices.length());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public JSONObject getItem(int position) {
        try {
            return peripheralDevices.getJSONObject(position);
        } catch (JSONException e) {
            return null;
        }
    }

    public void updateItemList(JSONArray peripheralDevices) {
        this.peripheralDevices = peripheralDevices;
        notifyDataSetChanged();
    }

    public void setIsTimeFormat24(boolean isTimeFormat24) {
        this.isTimeFormat24 = isTimeFormat24;
        notifyDataSetChanged();
    }


    class VersionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private LinearLayout llBg;
        private TextView tvReminderTime;
        private TextView tvRepeat;
        private ImageView ivRemove;

        public VersionViewHolder(View view) {
            super(view);
            llBg = (LinearLayout) view.findViewById(R.id.ll_bg);
            tvReminderTime = (TextView) view.findViewById(R.id.tv_reminder_time);
            tvRepeat = (TextView) view.findViewById(R.id.tv_repeat);
            ivRemove = (ImageView) view.findViewById(R.id.iv_remove);

            tvReminderTime.setOnClickListener(this);
            tvRepeat.setOnClickListener(this);
            ivRemove.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_reminder_time:
                    try {
                        mSavedItemSelect.onTimeSelect(peripheralDevices.getJSONObject(getAdapterPosition()), getAdapterPosition());
                    } catch (JSONException e) {
                    }
                    break;
                case R.id.tv_repeat:
                    try {
                        mSavedItemSelect.onRepeatSelect(peripheralDevices.getJSONObject(getAdapterPosition()), getAdapterPosition());
                    } catch (JSONException e) {
                    }
                    break;
                case R.id.iv_remove:
                    try {
                        mSavedItemSelect.onDeleteSelect(peripheralDevices.getJSONObject(getAdapterPosition()), getAdapterPosition());
                    } catch (JSONException e) {
                    }
                    break;
            }
        }
    }

    public static interface ReminderSelect {
        public void onTimeSelect(JSONObject peripheralDevice, int position);

        public void onRepeatSelect(JSONObject peripheralDevice, int position);

        public void onDeleteSelect(JSONObject peripheralDevice, int position);
    }

    private String getDisplayTime(int hourOfDay, int minute) {

        if(isTimeFormat24){
            return String.format("%02d", hourOfDay)
                    + ":" + String.format("%02d",minute) + " ";
        }else{
            String aMpM = "AM";
            if (hourOfDay > 11) {
                aMpM = "PM";
            }

            int currentHour;
            if (hourOfDay > 11) {
                currentHour = hourOfDay - 12;
            } else {
                currentHour = hourOfDay;
            }

            if(currentHour==0)
                currentHour = 12;

            return String.format("%02d",currentHour)
                    + ":" + String.format("%02d",minute) + " " + aMpM;
        }

    }

    private String getDisplayReminder(JSONObject object) {
        String displayString = "";
        try {
            if (object.getInt(OmronConstants.OMRONDeviceAlarmSettings.SundayKey) == 1) {
                if (displayString.trim().length() > 0)
                    displayString=displayString+", ";
                displayString=displayString+"Sun";
            }
            if (object.getInt(OmronConstants.OMRONDeviceAlarmSettings.MondayKey) == 1) {
                if (displayString.trim().length() > 0)
                    displayString=displayString+", ";
                displayString=displayString+"Mon";
            }
            if (object.getInt(OmronConstants.OMRONDeviceAlarmSettings.TuesdayKey) == 1) {
                if (displayString.trim().length() > 0)
                    displayString=displayString+", ";
                displayString=displayString+"Tue";
            }
            if (object.getInt(OmronConstants.OMRONDeviceAlarmSettings.WednesdayKey) == 1) {
                if (displayString.trim().length() > 0)
                    displayString=displayString+", ";
                displayString=displayString+"Wed";
            }
            if (object.getInt(OmronConstants.OMRONDeviceAlarmSettings.ThursdayKey) == 1) {
                if (displayString.trim().length() > 0)
                    displayString=displayString+", ";
                displayString=displayString+"Thu";
            }
            if (object.getInt(OmronConstants.OMRONDeviceAlarmSettings.FridayKey) == 1) {
                if (displayString.trim().length() > 0)
                    displayString=displayString+", ";
                displayString=displayString+"Fri";
            }
            if (object.getInt(OmronConstants.OMRONDeviceAlarmSettings.SaturdayKey) == 1) {
                if (displayString.trim().length() > 0)
                    displayString=displayString+", ";
                displayString=displayString+"Sat";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return displayString;
    }

}
