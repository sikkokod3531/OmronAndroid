package com.omronhealthcare.OmronConnectivitySample.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivitySample.R;
import com.omronhealthcare.OmronConnectivitySample.models.PeripheralDevice;

import java.util.List;

/**
 * Created by Omron HealthCare Inc
 */

public class SavedDevicesListAdapter extends RecyclerView.Adapter<SavedDevicesListAdapter.VersionViewHolder> {

    private final Context context;
    List<PeripheralDevice> peripheralDevices;
    SavedItemSelect mSavedItemSelect;

    public SavedDevicesListAdapter(Context context, List<PeripheralDevice> peripheralDevices,SavedItemSelect savedItemSelect) {
        this.context = context;
        this.peripheralDevices = peripheralDevices;
        this.mSavedItemSelect = savedItemSelect;
    }

    public void setDevices(List<PeripheralDevice> peripheralDevices) {

        this.peripheralDevices = peripheralDevices;
    }

    @Override
    public VersionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_saved_devices, parent, false);
        VersionViewHolder viewHolder = new VersionViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VersionViewHolder versionViewHolder, final int position) {
        versionViewHolder.tvModelName.setText(peripheralDevices.get(position).getModelName());
        versionViewHolder.tvDeviceUuid.setText(peripheralDevices.get(position).getUuid());
        versionViewHolder.tvLocalName.setText(peripheralDevices.get(position).getLocalName());
    }

    @Override
    public int getItemCount() {
        if (peripheralDevices == null) {
            return 0;
        }
        return (peripheralDevices.size());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public PeripheralDevice getItem(int position) {
        return peripheralDevices.get(position);
    }

    public void updateItemList(List<PeripheralDevice> peripheralDevices) {
        this.peripheralDevices.clear();
        if (peripheralDevices != null)
            this.peripheralDevices.addAll(peripheralDevices);
        notifyDataSetChanged();
    }

    class VersionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private LinearLayout llBg;
        private LinearLayout llDetails;
        private TextView tvModelName;
        private TextView tvLocalName;
        private TextView tvDeviceUuid;

        public VersionViewHolder(View view) {
            super(view);
            llBg = (LinearLayout) view.findViewById(R.id.ll_bg);
            llDetails = (LinearLayout) view.findViewById(R.id.ll_details);
            tvModelName = (TextView) view.findViewById(R.id.tv_model_name);
            tvLocalName = (TextView) view.findViewById(R.id.tv_local_name);
            tvDeviceUuid = (TextView) view.findViewById(R.id.tv_device_uuid);

            llDetails.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.ll_details:
                    mSavedItemSelect.onItemSelect(peripheralDevices.get(getAdapterPosition()));
                    break;
            }
        }
    }

    public static interface SavedItemSelect {
        public void onItemSelect(PeripheralDevice peripheralDevice);
    }

}
