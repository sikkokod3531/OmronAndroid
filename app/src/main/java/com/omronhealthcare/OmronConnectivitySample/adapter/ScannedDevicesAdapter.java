package com.omronhealthcare.OmronConnectivitySample.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronPeripheral;
import com.omronhealthcare.OmronConnectivitySample.R;

import java.util.ArrayList;

/**
 * Created by Omron HealthCare Inc
 */

public class ScannedDevicesAdapter extends BaseAdapter {

    private final Context context;

    private ArrayList<OmronPeripheral> mPeripheralList;
    public ScannedDevicesAdapter(Context context, ArrayList<OmronPeripheral> peripheralList) {
        this.context = context;
        mPeripheralList = peripheralList;
    }

    public void setPeripheralList(ArrayList<OmronPeripheral> peripheralList) {

        mPeripheralList = peripheralList;
    }

    @Override
    public int getCount() {
        return mPeripheralList.size();
    }

    @Override
    public OmronPeripheral getItem(int position) {
        return mPeripheralList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflator = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        OmronPeripheral item = getItem(position);
        View v;
        v = createContentView(inflator, item, parent);
        return v;
    }

    private View createContentView(LayoutInflater inflator, final OmronPeripheral item, ViewGroup parent) {
        View v = inflator.inflate(R.layout.list_item_scanned_device, parent, false);

        TextView tvMdelName = (TextView) v.findViewById(R.id.tv_model_name);
        TextView tvDeviceSeries = (TextView) v.findViewById(R.id.tv_device_series);


        tvMdelName.setText(item.getLocalName());
        tvDeviceSeries.setText(item.getUuid() + "\nRSSI : " + item.getRssi());

        return v;
    }

}
