package com.omronhealthcare.OmronConnectivitySample.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.omronhealthcare.OmronConnectivitySample.Database.OmronDBConstans;
import com.omronhealthcare.OmronConnectivitySample.R;

/**
 * Created by Omron HealthCare Inc
 */

public class VitalDataListAdapter extends RecyclerView.Adapter<VitalDataListAdapter.VersionViewHolder> {

    private final Context context;
    Cursor mCursor;


    public VitalDataListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public VersionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_vital_data, parent, false);
        VersionViewHolder viewHolder = new VersionViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VersionViewHolder versionViewHolder, int position) {
        if (!mCursor.moveToPosition(position))
            return;

        String tvText = "";
        tvText = tvText + "SYS " + " (mmHg): " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.VITAL_DATA_OMRONVitalDataSystolicKey)) + "\n";
        tvText = tvText + "DIA " + " (mmHg)  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.VITAL_DATA_OMRONVitalDataDiastolicKey)) + "\n";
        tvText = tvText + "PULSE " + " (bpm)  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.VITAL_DATA_OMRONVitalDataPulseKey)) + "\n";
        tvText = tvText + "AFIB  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.VITAL_DATA_OMRONVitalDataAtrialFibrillationDetectionFlagKey)) + "\n";
        tvText = tvText + "NightMode Error  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.VITAL_DATA_OMRONVitalDataDisplayedErrorCodeNightModeKey)) + "\n";
        tvText = tvText + "Measurement Mode  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.VITAL_DATA_OMRONVitalDataMeasurementModeKey)) + "\n";
        tvText = tvText + "DEVICE   : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.DEVICE_IDENTITY_NAME)) + "\n";
        tvText = tvText + "USER  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.DEVICE_SELECTED_USER)) + "\n";
        tvText = tvText + "DATE  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.VITAL_DATA_OMRONVitalDataMeasurementDateKey)) + "\n";
        versionViewHolder.tvData.setText(tvText);
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        return (mCursor.getCount());
    }

    @Override
    public long getItemId(int position) {
        if (!mCursor.moveToPosition(position))
            throw new IndexOutOfBoundsException("Unable to move cursor to position " + position);
        return mCursor.getInt(mCursor.getColumnIndex(OmronDBConstans.VITAL_DATA_INDEX));
    }

    /**
     * Swaps the Cursor currently held in the adapter with a new one
     * and triggers a UI refresh
     *
     * @param newCursor the new cursor that will replace the existing one
     */
    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        if (newCursor != null) {
            // Force the RecyclerView to refresh
            this.notifyDataSetChanged();
        }
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
