package com.omronhealthcare.OmronConnectivitySample.models;

/**
 * Created by Omron HealthCare Inc
 */

public class ReminderItem {
    String hour;
    String minute;

    public ReminderItem(String hour, String minute, String days) {
        this.hour = hour;
        this.minute = minute;
        this.days = days;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    String days;
}
