package com.example.android.walkmyandroid;

public class User {

    private String userID;
    private String username;
    private long lastGeofenceEntrance;
    private long lastGeofenceExit;
    private long lastClockIn;
    private long lastClockOut;
    private boolean checkedIn;

    public User() {
    }

    public User(String userID, String username) {
        this.userID = userID;
        this.username = username;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getLastGeofenceEntrance() {
        return lastGeofenceEntrance;
    }

    public void setLastGeofenceEntrance(long lastGeofenceEntrance) {
        this.lastGeofenceEntrance = lastGeofenceEntrance;
    }

    public long getLastGeofenceExit() {
        return lastGeofenceExit;
    }

    public void setLastGeofenceExit(long lastGeofenceExit) {
        this.lastGeofenceExit = lastGeofenceExit;
    }

    public long getLastClockIn() {
        return lastClockIn;
    }

    public void setLastClockIn(long lastClockIn) {
        this.lastClockIn = lastClockIn;
    }

    public long getLastClockOut() {
        return lastClockOut;
    }

    public void setLastClockOut(long lastClockOut) {
        this.lastClockOut = lastClockOut;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }
}
