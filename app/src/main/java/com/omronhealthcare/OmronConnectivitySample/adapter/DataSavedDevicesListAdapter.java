package com.omronhealthcare.OmronConnectivitySample.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.utility.PreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Omron HealthCare Inc
 */

public class DataSavedDevicesListAdapter extends RecyclerView.Adapter<DataSavedDevicesListAdapter.VersionViewHolder> {

    private final Context context;
    JSONArray deviceList;
    DeviceItemSelect mSavedItemSelect;

    public DataSavedDevicesListAdapter(Context context, JSONArray deviceList, DeviceItemSelect savedItemSelect) {
        this.context = context;
        this.deviceList = deviceList;
        this.mSavedItemSelect = savedItemSelect;
    }

    @Override
    public VersionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_data_saved_devices, parent, false);
        VersionViewHolder viewHolder = new VersionViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VersionViewHolder versionViewHolder, final int position) {
        try {
            versionViewHolder.tvLocalName.setText(deviceList.getJSONObject(position).getString(PreferencesManager.JSON_KEY_DEVICE_LOCAL_NAME));
            versionViewHolder.tvModelName.setText(deviceList.getJSONObject(position).getString(PreferencesManager.JSON_KEY_DEVICE_MODEL_NAME));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        if (deviceList == null) {
            return 0;
        }
        return (deviceList.length());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public JSONObject getItem(int position) {
        try {
            return deviceList.getJSONObject(position);
        } catch (JSONException e) {
            return null;
        }
    }

    public void updateItemList(JSONArray deviceList) {
        this.deviceList=deviceList;
        notifyDataSetChanged();
    }

    class VersionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private LinearLayout llBg;
        private LinearLayout llDetails;
        private TextView tvModelName;
        private TextView tvLocalName;

        public VersionViewHolder(View view) {
            super(view);
            llBg = (LinearLayout) view.findViewById(R.id.ll_bg);
            llDetails = (LinearLayout) view.findViewById(R.id.ll_details);
            tvModelName = (TextView) view.findViewById(R.id.tv_model_name);
            tvLocalName = (TextView) view.findViewById(R.id.tv_local_name);

            llDetails.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ll_details:
                    try {
                        mSavedItemSelect.onItemSelect(deviceList.getJSONObject(getAdapterPosition()).getString(PreferencesManager.JSON_KEY_DEVICE_LOCAL_NAME), deviceList.getJSONObject(getAdapterPosition()).getString(PreferencesManager.JSON_KEY_DEVICE_MODEL_NAME), deviceList.getJSONObject(getAdapterPosition()).getInt(PreferencesManager.JSON_KEY_DEVICE_CATEGORY), getAdapterPosition());
                    } catch (JSONException e) {
                    }
                    break;
            }
        }
    }

    public static interface DeviceItemSelect {
        public void onItemSelect(String localName,String model, Integer category, int position);
    }

}
