package com.example.android.walkmyandroid;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class CalculateDistanceToWorksiteCenterRemoteService extends Service {

    private static final double CARROLLS_LATITUDE = 53.98174589;
    private static final double CARROLLS_LONGITUDE = -6.39237732;

    public CalculateDistanceToWorksiteCenterRemoteService() {
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            Bundle data = msg.getData();
            double userLongitude = data.getDouble("Longitude");
            double userLatitude = data.getDouble("Latitude");

            Location Carrolls = new Location("");
            Carrolls.setLatitude(CARROLLS_LATITUDE);
            Carrolls.setLongitude(CARROLLS_LONGITUDE);

            Location userLocation = new Location("");
            userLocation.setLatitude(userLatitude);
            userLocation.setLongitude(userLongitude);

            float distanceInMeters = userLocation.distanceTo(Carrolls);

            //Send the response back to the MapActiivty Handler
            Message reply = Message.obtain();

            Bundle bundle = new Bundle();
            bundle.putDouble("Distance", distanceInMeters);

            reply.setData(bundle);

            try {
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Log.e("SERVICE YOKE", "DISTANCE IN METERS: " + distanceInMeters);
        }
    }

    final Messenger myMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return myMessenger.getBinder();
    }
}
