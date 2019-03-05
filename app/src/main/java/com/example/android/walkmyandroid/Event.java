package com.example.android.walkmyandroid;

import android.annotation.SuppressLint;

import java.util.Map;
import java.util.Objects;

public class Event {
    private Map<String, String> timestamp;
    private String userName;
    private String userID;
    private double latitude;
    private double longitude;
    private String eventType;

    public Event() {

    }

    public Event(Map<String, String> timestamp, String userName, String userID, double latitude, double longitude, String eventType) {
        this.timestamp = timestamp;
        this.userName = userName;
        this.userID = userID;
        this.latitude = latitude;
        this.longitude = longitude;
        this.eventType = eventType;
    }

    public Map<String, String> getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Map<String, String> timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Double.compare(event.latitude, latitude) == 0 &&
                Double.compare(event.longitude, longitude) == 0 &&
                Objects.equals(timestamp, event.timestamp) &&
                Objects.equals(userName, event.userName) &&
                Objects.equals(userID, event.userID) &&
                Objects.equals(eventType, event.eventType);
    }

    @SuppressLint("NewApi")
    @Override
    public int hashCode() {

        return Objects.hash(timestamp, userName, userID, latitude, longitude, eventType);
    }
}
