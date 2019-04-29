package com.example.android.walkmyandroid;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static com.example.android.walkmyandroid.App.CHANNEL_ID;

public class ForegroundService extends Service{

    public  static final String START_ACTION = "com.example.android.walkmyandroid.START";
    public  static final String STOP_ACTION = "com.example.android.walkmyandroid.STOP";
    public static final String Foreground_Notif = "ForegroundService";

    private Thread bgThread;

    private PowerManager.WakeLock wakeLock = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        //https://stuff.mit.edu/afs/sipb/project/android/docs/reference/android/os/PowerManager.html
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ForegroundService.class.getName());
        wakeLock.acquire();

        bgThread = new Thread(new Runnable() {


            @Override
            public void run() {

                try {
                    for (int i = 0; i < 10000; i++) {
                        Thread.sleep(1000);
                        Log.i(Foreground_Notif, "i = " + Integer.toString(i));
                    }
                }catch (Exception e) {
                    Log.i(Foreground_Notif, "bgThread: " + e.getMessage());
                }

            }
        });
    }

    @Override
    public void onDestroy() {
        wakeLock.release();
        wakeLock = null;
    }

    //https://codinginflow.com/tutorials/android/foreground-service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            //intent for stop service
            Intent stopIntent = new Intent(this, ForegroundService.class);
            stopIntent.setAction(STOP_ACTION);
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_dialog_alert);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.title))
                    .setTicker(getString(R.string.Secondary))
                    .setContentText(getString(R.string.Primary))
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.close), stopPendingIntent)
                    .build();

            startForeground(1, notification);
            bgThread.start();
        return START_STICKY;
    }
}
