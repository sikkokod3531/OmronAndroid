package com.omronhealthcare.OmronConnectivitySample.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants;
import com.omronhealthcare.OmronConnectivitySample.Activities.OmronConnectedDeviceList;
import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.utility.PreferencesManager;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Omron HealthCare Inc
 */

public class ConnectedDeviceAdapter extends BaseAdapter {

    private final Context context;

    List<HashMap<String, String>> mDeviceList;
    OmronConnectedDeviceList mOmronConnectedDeviceList;

    public ConnectedDeviceAdapter(Context context, List<HashMap<String, String>> deviceList) {
        this.context = context;
        mDeviceList = deviceList;
        mOmronConnectedDeviceList = (OmronConnectedDeviceList) context;
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public HashMap<String, String> getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflator = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        HashMap<String, String> item = getItem(position);
        View v;
        v = createContentView(convertView, inflator, item, parent);
        return v;
    }

    private View createContentView(View convertView, LayoutInflater inflator, final HashMap<String, String> item, ViewGroup parent) {

        View v = convertView;
        ViewHolder holder;

        if (convertView == null) {

            v = inflator.inflate(R.layout.list_item_device_listing, null);

            holder = new ViewHolder();
            holder.tvMdelName = (TextView) v.findViewById(R.id.tv_model_name);
            holder.tvDeviceSeries = (TextView) v.findViewById(R.id.tv_device_series);
            holder.ivInfo = (ImageView) v.findViewById(R.id.iv_info);
            holder.llBg = (LinearLayout) v.findViewById(R.id.ll_bg);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }


        holder.tvMdelName.setText(item.get(OmronConstants.OMRONBLEConfigDevice.ModelDisplayName));

        if(item.get(OmronConstants.OMRONBLEConfigDevice.ModelDisplayName).equalsIgnoreCase("Connected Devices")) {
            PreferencesManager preferencesManager = new PreferencesManager(context);
            String count = "Count : " + ((preferencesManager.getSavedDeviceList() != null) ? preferencesManager.getSavedDeviceList().size() : 0);
            holder.tvDeviceSeries.setText(count);
        }else {
            holder.tvDeviceSeries.setText(item.get(OmronConstants.OMRONBLEConfigDevice.Identifier));
        }

        if(item.get(OmronConstants.OMRONBLEConfigDevice.Thumbnail) != null) {
            Resources res = context.getResources();
            int resourceId = res.getIdentifier(item.get(OmronConstants.OMRONBLEConfigDevice.Thumbnail), "drawable", context.getPackageName());
            holder.ivInfo.setImageResource(resourceId);
        }else {
            holder.ivInfo.setImageResource(R.drawable.ic_info_outline);
        }

        holder.ivInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOmronConnectedDeviceList.showDeviceInfo(item);
            }
        });

        holder.llBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOmronConnectedDeviceList.selectDevice(item);
            }
        });

        return v;
    }

    public static class ViewHolder {

        public TextView tvMdelName;
        public TextView tvDeviceSeries;
        LinearLayout llBg;
        public ImageView ivInfo;
    }

}
