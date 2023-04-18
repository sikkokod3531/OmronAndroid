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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Omron HealthCare Inc
 */

public class RecordsDataListAdapter extends RecyclerView.Adapter<RecordsDataListAdapter.VersionViewHolder> {

    private final Context context;
    Cursor mCursor;


    public RecordsDataListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public VersionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_records_data, parent, false);
        VersionViewHolder viewHolder = new VersionViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VersionViewHolder versionViewHolder, int position) {
        if (!mCursor.moveToPosition(position))
            return;

        String dateString = getDateAndTIme(mCursor.getString(mCursor.getColumnIndex(OmronDBConstans.RECORD_DATA_StartDateUTCKey)));

        String tvText = "";
        tvText = dateString;
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
        return mCursor.getInt(mCursor.getColumnIndex(OmronDBConstans.RECORD_DATA_INDEX));
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

    private String getDateAndTIme(String timeStamp) {

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
        long startDate = Long.parseLong(timeStamp);

        Date date= new Date(startDate);
        String dateString = formatter.format(date);

        return dateString;
    }
}
