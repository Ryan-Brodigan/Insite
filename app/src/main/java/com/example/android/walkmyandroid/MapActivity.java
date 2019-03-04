package com.example.android.walkmyandroid;

import android.Manifest;
import android.animation.AnimatorSet;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;



import java.util.Random;

public class MapActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted, OnMapReadyCallback {

    private final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient mFusedLocationClient;
    private AnimatorSet mRotateAnim;
    private boolean mTrackingLocation;
    private Location mCurrentLocation;
    private Button mButton;
    private LocationCallback mLocationCallback;
    private GoogleMap mMap;
    private GeofencingClient mGeofencingClient;
    private Geofence mGeofence;
    private PendingIntent mGeofencePendingIntent;
    private DatabaseReference mDatabase;
    private User currentUser;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);


        //Get Firebase RD reference
        mDatabase = FirebaseDatabase.getInstance().getReference();

        auth = FirebaseAuth.getInstance();

        //GET USER CREDS AND UPDATE CURRENT USER
        currentUser = new User(auth.getCurrentUser().getUid(),auth.getCurrentUser().getEmail());
        currentUser.setClockedIn(false);
        Log.i("This is login", currentUser.getUserID() + " " + currentUser.getUsername());

        //Check-In/Out Button
        mButton = findViewById(R.id.check_in_out_button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!currentUser.isClockedIn()){
                    clockIn();
                }else{
                    clockOut();
                }
            }
        });

        //FusedLocationProviderClient for getting the User's current location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // N.B. When we receive the latest location update
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // If tracking is turned on, reverse geocode into an address
                if (mTrackingLocation) {
                    new FetchAddressTask(MapActivity.this, MapActivity.this)
                            .execute(locationResult.getLastLocation());

                    mMap.clear();

                    drawGeofenceCircleOnMap();

                    mCurrentLocation = locationResult.getLastLocation();

                    LatLng currentLatLong = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currentLatLong).title("currentLocation"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLong));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(16.5f));
                }
            }
        };

        //Google Maps Setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.g_map);
        mapFragment.getMapAsync(this);

        //Geofencing Client and adding Geofence on map
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mGeofence = getGeofence();
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
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

        //Once we're done setup, start tracking the User's location
        startTrackingLocation();

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
            mTrackingLocation = true;
        }
    }

    private void clockIn(){
        if(!currentUser.isClockedIn()){
            currentUser.setClockedIn(true);
            mButton.setText(R.string.clock_out);

            registerClockInOut("Clocked-In");
        }
    }

    private void clockOut(){
        if(currentUser.isClockedIn()){
            currentUser.setClockedIn(false);
            mButton.setText(R.string.clock_in);

            registerClockInOut("Clocked-Out");
        }
    }

    private void registerClockInOut(String eventType){
        Event newClockInOutEvent = new Event(ServerValue.TIMESTAMP,
                currentUser.getUsername(),
                currentUser.getUserID(),
                mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude(),
                eventType);

        Random random = new Random();

        mDatabase.child("Events").child(Integer.toString(newClockInOutEvent.hashCode() + random.nextInt())).setValue(newClockInOutEvent);
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