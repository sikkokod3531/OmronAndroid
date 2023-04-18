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

public class PulseOxymeterDataListAdapter extends RecyclerView.Adapter<PulseOxymeterDataListAdapter.VersionViewHolder> {

    private final Context context;
    Cursor mCursor;


    public PulseOxymeterDataListAdapter(Context context) {
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
        tvText = tvText + "SpO2 " + " (%) : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.OXYMETER_DATA_SpO2Key)) + "\n";
        tvText = tvText + "Pulse " + " (bpm)  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.OXYMETER_DATA_PulseKey)) + "\n";
        tvText = tvText + "Date  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.OXYMETER_DATA_StartTimeKey)) + "\n";
        tvText = tvText + "Device  : " + mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.DEVICE_IDENTITY_NAME));
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
        return mCursor.getInt(mCursor.getColumnIndex(OmronDBConstans.OXYMETER_DATA_INDEX));
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
