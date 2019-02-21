/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.walkmyandroid;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted, OnMapReadyCallback {

    private final int REQUEST_LOCATION_PERMISSION = 1;
//    private TextView mLocationTextView;
    private FusedLocationProviderClient mFusedLocationClient;
    private AnimatorSet mRotateAnim;
//    private ImageView mImageView;
    private boolean mTrackingLocation;
    private Button mButton;
    private LocationCallback mLocationCallback;
    private GoogleMap mMap;
    private GeofencingClient mGeofencingClient;
    private Geofence mGeofence;
    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mButton = findViewById(R.id.button_location);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mTrackingLocation){
                    startTrackingLocation();
                }else{
                    stopTrackingLocation();
                }
            }
        });

        // N.B.
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // If tracking is turned on, reverse geocode into an address
                if (mTrackingLocation) {
                    new FetchAddressTask(MainActivity.this, MainActivity.this)
                            .execute(locationResult.getLastLocation());

                    mMap.clear();

                    drawGeofenceCircleOnMap();

                    Location currentLocation = locationResult.getLastLocation();

                    LatLng currentLatLong = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currentLatLong).title("currentLocation"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLong));
                    mMap.animateCamera( CameraUpdateFactory.zoomTo( 16.5f ) );
                }
            }
        };

        //Google Maps Setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.g_map);
        mapFragment.getMapAsync(this);

        //Geofencing Client and drawing Geofence on map
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mGeofence = getGeofence();
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added
                        Log.i("Geofences Success", "GEOFENCE SUCCESSFULLY ADDED");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add geofences
                        Log.i("Geofence Failure", "GEOFENCE FAILED TO ADD");
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        drawGeofenceCircleOnMap();
    }

    private void drawGeofenceCircleOnMap(){
        mMap.addCircle(new CircleOptions()
                .center(new LatLng(53.98174589, -6.39237732))
                .radius(100)
                .strokeColor(Color.RED)
                .fillColor(0x220000FF)
                .strokeWidth(5));
    }

    private Geofence getGeofence(){
        return new Geofence.Builder()

                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("MahGeofence")

                .setCircularRegion(
                        53.98174589,
                        -6.39237732,
                        100
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(mGeofence);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void startTrackingLocation(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
        else if(!mTrackingLocation){

            mFusedLocationClient.requestLocationUpdates
                    (getLocationRequest(), mLocationCallback,
                            null /* Looper */);

//            mRotateAnim.start();
            mTrackingLocation = true;
            mButton.setText(R.string.stop_tracking_location);
        }
    }

    private void stopTrackingLocation(){
        if (mTrackingLocation) {

            mFusedLocationClient.removeLocationUpdates(mLocationCallback);

            mTrackingLocation = false;
            mButton.setText(R.string.start_tracking_location);
//            mLocationTextView.setText(R.string.textview_hint);
//            mRotateAnim.end();
        }
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                // If the permission is granted, get the location,
                // otherwise, show a Toast
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startTrackingLocation();
                } else {
                    Toast.makeText(this,
                            R.string.location_permission_denied,
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onTaskCompleted(String result) {
        if(mTrackingLocation){
//            mLocationTextView.setText(getString(R.string.address_text,
//                    result, System.currentTimeMillis()));
        }
    }
}
