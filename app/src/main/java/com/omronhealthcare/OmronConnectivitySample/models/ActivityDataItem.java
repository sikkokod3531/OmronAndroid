package com.omronhealthcare.OmronConnectivitySample.models;

/**
 * Created by Omron HealthCare Inc
 */

public class ActivityDataItem {

    String mKey;
    String mName;

    public ActivityDataItem(String mKey, String mName) {
        this.mKey = mKey;
        this.mName = mName;
    }

    public String getKey() {
        return mKey;
    }

    public String getName() {
        return mName;
    }


}
