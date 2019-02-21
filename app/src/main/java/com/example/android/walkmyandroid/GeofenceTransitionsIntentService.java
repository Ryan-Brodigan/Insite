package com.example.android.walkmyandroid;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

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
        }
        else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Log the exit transition details
            Log.i(LOGTAG, getString(R.string.geofence_user_exit));
        }
        else {
            // Log the error.
            Log.e(LOGTAG, getString(R.string.geofence_transition_invalid_type));
        }
    }
}
