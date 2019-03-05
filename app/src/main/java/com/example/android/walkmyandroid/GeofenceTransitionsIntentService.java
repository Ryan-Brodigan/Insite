package com.example.android.walkmyandroid;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.Random;

public class GeofenceTransitionsIntentService extends IntentService {

    private static String LOGTAG = "GEOFENCEINTENTS";

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(LOGTAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Log the entry transition details.
            Log.i(LOGTAG, getString(R.string.geofence_user_entry));

            registerGeofenceEvent(intent,"Geofence Entry");
        }
        else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Log the exit transition details
            Log.i(LOGTAG, getString(R.string.geofence_user_exit));

            registerGeofenceEvent(intent, "Geofence Exit");
        }
        else {
            // Log the error.
            Log.e(LOGTAG, getString(R.string.geofence_transition_invalid_type));
        }
    }

    private void registerGeofenceEvent(Intent intent, String eventType){

        //Database Reference
        DatabaseReference DBRef = FirebaseDatabase.getInstance().getReference();

        Event newClockInOutEvent = new Event(ServerValue.TIMESTAMP,
                intent.getExtras().getString("CURRENT_USER_USERNAME"),
                intent.getExtras().getString("CURRENT_USER_ID"),
                intent.getExtras().getDouble("CURRENT_LATITUDE"),
                intent.getExtras().getDouble("CURRENT_LONGITUDE"),
                eventType);

        Random random = new Random();

        DBRef.child("Events").child(Integer.toString(newClockInOutEvent.hashCode() + random.nextInt())).setValue(newClockInOutEvent);
    }
}