package com.example.android.walkmyandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

public class ConnectivityChangeBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
            if(!isConnectedViaWifiOrMobileNetwork(context)){
                Toast toast = Toast.makeText(context, "LOST CONNECTION TO WIFI", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    private boolean isConnectedViaWifiOrMobileNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMobile = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return (mWifi.isConnected() || mMobile.isConnected());
    }
}
