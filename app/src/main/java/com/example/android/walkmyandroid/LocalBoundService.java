package com.example.android.walkmyandroid;

import java.util.Calendar;
import java.util.Date;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class LocalBoundService extends Service  {
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        LocalBoundService getService() {
            return LocalBoundService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public Date getCurrentDate() {
        return Calendar.getInstance().getTime();
    }

}
