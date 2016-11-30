package com.example.android.sunshine;

import android.util.Log;

import com.example.android.sunshine.sync.SunshineSyncAdapter;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class FetchWeatherRequestListenerService extends WearableListenerService {

    public void register(Utility.CallBack callback) {
        callback.initializeGoogleApiClient();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().equals("/fetch-weather-data")) {
            Log.d("xxx", "request for weather accepted");

            FetchWeatherRequestListenerService caller = new FetchWeatherRequestListenerService();
            Utility.CallBack callBack = new SunshineSyncAdapter(getApplicationContext(), false);
            caller.register(callBack);
        }
    }
}
