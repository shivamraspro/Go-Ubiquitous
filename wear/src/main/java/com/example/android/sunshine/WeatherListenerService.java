package com.example.android.sunshine;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import org.greenrobot.eventbus.EventBus;

public class WeatherListenerService extends WearableListenerService {

    public static class WeatherData {
        public static int weatherId;
        public static String tempHigh;
        public static String tempLow;

        public WeatherData(int weatherId, String tempHigh, String tempLow) {
            this.weatherId = weatherId;
            this.tempHigh = tempHigh;
            this.tempLow = tempLow;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent dataEvent : dataEventBuffer) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if (path.equals("/weather-today")) {
                    Log.d("xxx", "weather data received successfully");
                    EventBus.getDefault().post(new WeatherData(
                            dataMap.getInt("weatherId"),
                            dataMap.getString("tempHigh"),
                            dataMap.getString("tempLow")));
                }
            }
        }
    }
}
