package com.example.android.walkmyandroid;

import android.Manifest;
import android.animation.AnimatorSet;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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

import java.util.Date;

public class MapActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted, OnMapReadyCallback {

    //Handler for sending as the replyTo param of the Message we send to the service for calculating the distance between the User's current location
    //And the centre of the Carroll's Worksite


    class DistanceCalculationHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            double distance = data.getDouble("Distance");
            mDistanceToWorksiteTextView = findViewById(R.id.distance_to_center);
            mDistanceToWorksiteTextView.setText(getString(R.string.distance_to_site, distance));
        }
    }

    private final int REQUEST_LOCATION_PERMISSION = 1;
    private final double CARROLLS_LATITUDE = 53.98174589;
    private final double CARROLLS_LONGITUDE = -6.39237732;
    private final float GEOFENCE_RADIUS = 100;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mCurrentLocation;
    private Button mButton;
    private LocationCallback mLocationCallback;
    private GoogleMap mMap;
    private TextView mAddressTextView;
    private TextView mCurrentUserTextView;
    private TextView mDistanceToWorksiteTextView;
    private GeofencingClient mGeofencingClient;
    private Geofence mGeofence;
    private PendingIntent mGeofencePendingIntent;
    private DatabaseReference mDatabase;
    private User currentUser;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;
    private Button signOut, rstPass;

    //Service Setup

    Messenger mDistanceService = null;
    boolean isBoundToDistanceService;
    //ServiceConnection for Distance Service
    private ServiceConnection myDistanceServiceConnection =
            new ServiceConnection() {
                public void onServiceConnected(
                        ComponentName className,
                        IBinder service) {
                    mDistanceService = new Messenger(service);
                    isBoundToDistanceService = true;
                }

                public void onServiceDisconnected(
                        ComponentName className) {
                    mDistanceService = null;
                    isBoundToDistanceService = false;
                }
            };

    LocalBoundService localService;
    private boolean isBound = false;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalBoundService.LocalBinder binder = (LocalBoundService.LocalBinder) service;
            localService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);


        //Setup our Broadcast receiver to listen for changes in the device's connectivity
        ConnectivityChangeBroadcastReceiver receiver = new ConnectivityChangeBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);

        //Bind to our Remote Bound Service
        Intent intent = new Intent(getApplicationContext(),
                CalculateDistanceToWorksiteCenterRemoteService.class);

        bindService(intent, myDistanceServiceConnection, Context.BIND_AUTO_CREATE);

        //Get Firebase RD reference
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //get the firebase authentication instance
        auth = FirebaseAuth.getInstance();

        //get the user session of the current user logged in
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //user.getEmail();
                if (user == null) {
                    //if user session is lost, log them out.
                    startActivity(new Intent(MapActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };
        //GET USER CREDS AND UPDATE CURRENT USER
        currentUser = new User(auth.getCurrentUser().getUid(),auth.getCurrentUser().getEmail());
        currentUser.setClockedIn(false);

        //Set Text with User's email
        mCurrentUserTextView = findViewById(R.id.current_user);
        mCurrentUserTextView.setText(getString(R.string.currentUser, currentUser.getUsername()));

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

        mAddressTextView = findViewById(R.id.textview_location);

        //FusedLocationProviderClient for getting the User's current location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mGeofencingClient = null;

        // N.B. When we receive the latest location update
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                new FetchAddressTask(MapActivity.this, MapActivity.this)
                        .execute(locationResult.getLastLocation());

                mMap.clear();

                drawGeofenceCircleOnMap();

                mCurrentLocation = locationResult.getLastLocation();

                if(mGeofencingClient == null){
                    //Geofencing Client and adding Geofence on map
                    mGeofencingClient = LocationServices.getGeofencingClient(MapActivity.this);
                    mGeofence = getGeofence();
                    if (ActivityCompat.checkSelfPermission(MapActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MapActivity.this, new String[]
                                        {Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_LOCATION_PERMISSION);
                    }
                    mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                            .addOnSuccessListener(MapActivity.this, new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Geofences added
                                    Log.i("Geofences Success", "GEOFENCE SUCCESSFULLY ADDED");
                                }
                            })
                            .addOnFailureListener(MapActivity.this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Failed to add geofences
                                    Log.i("Geofence Failure", "GEOFENCE FAILED TO ADD");
                                }
                            });
                }

                LatLng currentLatLong = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(currentLatLong).title("currentLocation"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLong));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16.5f));

                sendDistanceCalculationMessage();
            }
        };

        //Google Maps Setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.g_map);
        mapFragment.getMapAsync(this);

        //Once we're done setup, start tracking the User's location
        startTrackingLocation();

        signOut = (Button) findViewById(R.id.signout_btn);
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        rstPass = (Button) findViewById(R.id.reset_password);
        rstPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPass();
            }
        });

    }

    public void sendDistanceCalculationMessage(){

        if(!isBoundToDistanceService) return;

        Message msg = Message.obtain();

        Bundle bundle = new Bundle();
        bundle.putDouble("Longitude", mCurrentLocation.getLongitude());
        bundle.putDouble("Latitude", mCurrentLocation.getLatitude());

        //For the reply
        msg.replyTo = new Messenger(new DistanceCalculationHandler());

        msg.setData(bundle);

        try{
           mDistanceService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        drawGeofenceCircleOnMap();
    }

    private void drawGeofenceCircleOnMap(){
        mMap.addCircle(new CircleOptions()
                .center(new LatLng(CARROLLS_LATITUDE, CARROLLS_LONGITUDE))
                .radius(GEOFENCE_RADIUS)
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
                        CARROLLS_LATITUDE,
                        CARROLLS_LONGITUDE,
                        GEOFENCE_RADIUS
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
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);

        intent.putExtra("CURRENT_USER_ID", currentUser.getUserID());
        intent.putExtra("CURRENT_USER_USERNAME", currentUser.getUsername());
        intent.putExtra("CURRENT_LATITUDE", mCurrentLocation.getLatitude());
        intent.putExtra("CURRENT_LONGITUDE", mCurrentLocation.getLongitude());

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
        else{
            mFusedLocationClient.requestLocationUpdates
                    (getLocationRequest(), mLocationCallback,
                            null /* Looper */);
        }
    }

    private void clockIn(){
        if(!currentUser.isClockedIn() && userIsWithinGeofence()){
            currentUser.setClockedIn(true);
            mButton.setText(R.string.clock_out);
            Toast.makeText(this, R.string.clock_in_toast, Toast.LENGTH_LONG).show();

            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent startIntent = new Intent(MapActivity.this,ForegroundService.class);
                    startIntent.setAction(ForegroundService.START_ACTION);
                    startService(startIntent);
                }
            });

            registerClockInOut("Clocked-In");

        }
        else{
            Toast.makeText(this, R.string.cannot_clock_in_whilst_offsite_toast, Toast.LENGTH_LONG).show();
        }
    }

    private void clockOut(){
        if(currentUser.isClockedIn()){
            currentUser.setClockedIn(false);
            mButton.setText(R.string.clock_in);
            Toast.makeText(this, R.string.clock_out_toast, Toast.LENGTH_LONG).show();

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

    private boolean userIsWithinGeofence(){

        Location geofenceCentrePoint = new Location("");
        geofenceCentrePoint.setLatitude(CARROLLS_LATITUDE);
        geofenceCentrePoint.setLongitude(CARROLLS_LONGITUDE);

        return mCurrentLocation.distanceTo(geofenceCentrePoint) <= GEOFENCE_RADIUS;
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

    public void signOut() {
        auth.signOut();
    }

    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
        Intent intent = new Intent(this, LocalBoundService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }

        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    public void dispalyDate(View view) {
        if (isBound) {
            Date date = localService.getCurrentDate();
            Toast.makeText(this, String.valueOf(date), Toast.LENGTH_SHORT).show();
        }
    }

    public void resetPass(){
        Intent intent = new Intent(MapActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onTaskCompleted(String result) {
        mAddressTextView.setText(getString(R.string.address_text, result));
    }


}