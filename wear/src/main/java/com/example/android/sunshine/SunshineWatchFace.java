/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final String TIME = "time";
    private static final String DATE = "date";
    private static final String WEATHER_MAX = "wmax";
    private static final String WEATHER_MIN = "wmin";

    Resources mResources;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    public class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient
            .ConnectionCallbacks {
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mDatePaint;
        Paint mTimePaint;
        Paint mWeatherMaxPaint;
        Paint mWeatherMinPaint;

        boolean mAmbient;

        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffsetTime;
        float mXOffsetDate;
        float mXOffsetWeather;

        float mYOffsetTime;
        float mYOffsetDate;
        float getmYOffsetDivider;
        float mYOffsetWeather;

        String[] weekDayNames = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        String[] monthDayNames = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT",
                "NOV", "DEC"};

        int weatherID;
        String maxTemp;
        String minTemp;

        private GoogleApiClient mGoogleApiClient;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        boolean weatherAvailable;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setShowSystemUiTime(false)
                    .build());

            mResources = SunshineWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mResources.getColor(R.color.sunshine_blue));

            mTimePaint = createPaint(TIME);
            mDatePaint = createPaint(DATE);
            mWeatherMaxPaint = createPaint(WEATHER_MAX);
            mWeatherMinPaint = createPaint(WEATHER_MIN);

            mCalendar = Calendar.getInstance();

            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleApiClient.connect();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        private Paint createPaint(String type) {

            Paint paint = new Paint();

            switch (type) {
                case TIME:
                    paint.setColor(mResources.getColor(R.color.white));
                    paint.setTypeface(NORMAL_TYPEFACE);
                    paint.setAntiAlias(true);
                    break;

                case DATE:
                    paint.setColor(mResources.getColor(R.color.sunshine_light_blue));
                    paint.setTypeface(NORMAL_TYPEFACE);
                    paint.setAntiAlias(true);
                    break;

                case WEATHER_MAX:
                    paint.setColor(mResources.getColor(R.color.white));
                    paint.setTypeface(NORMAL_TYPEFACE);
                    paint.setAntiAlias(true);
                    break;

                case WEATHER_MIN:
                    paint.setColor(mResources.getColor(R.color.sunshine_light_blue));
                    paint.setTypeface(NORMAL_TYPEFACE);
                    paint.setAntiAlias(true);
                    break;
            }

            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                EventBus.getDefault().register(this);
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
                EventBus.getDefault().unregister(this);
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
//            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load mResources that have alternate values for round watches.
            boolean isRound = insets.isRound();
            mXOffsetTime = mResources.getDimension(isRound
                    ? R.dimen.digital_x_offset_time_round : R.dimen.digital_x_offset_time);
            mXOffsetDate = mResources.getDimension(isRound
                    ? R.dimen.digital_x_offset_date_round : R.dimen.digital_x_offset_date);
            mXOffsetWeather = mResources.getDimension(isRound
                    ? R.dimen.digital_x_offset_weather_round : R.dimen.digital_x_offset_weather);

            mYOffsetTime = mResources.getDimension(isRound
                    ? R.dimen.digital_y_offset_time_round : R.dimen.digital_y_offset_time);

            mYOffsetDate = mResources.getDimension(isRound
                    ? R.dimen.digital_y_offset_date_round : R.dimen.digital_y_offset_date);

            getmYOffsetDivider = mResources.getDimension(isRound
                    ? R.dimen.divider_y_offset_round : R.dimen.divider_y_offset);

            mYOffsetWeather = mResources.getDimension(isRound
                    ? R.dimen.digital_y_offset_weather_round : R.dimen.digital_y_offset_weather);

            float textSizeTime = mResources.getDimension(isRound
                    ? R.dimen.digital_text_size_time_round : R.dimen.digital_text_size_time);
            mTimePaint.setTextSize(textSizeTime);

            float textSizeDate = mResources.getDimension(isRound
                    ? R.dimen.digital_text_size_date_round : R.dimen.digital_text_size_date);
            mDatePaint.setTextSize(textSizeDate);

            float textSizeWeather = mResources.getDimension(isRound
                    ? R.dimen.digital_text_size_weather_round : R.dimen.digital_text_size_weather);
            mWeatherMaxPaint.setTextSize(textSizeWeather);
            mWeatherMinPaint.setTextSize(textSizeWeather);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;

            if (inAmbientMode) {
                if (mLowBitAmbient) {
                    mTimePaint.setAntiAlias(false);
                    mDatePaint.setAntiAlias(false);
                }
                mDatePaint.setColor(mResources.getColor(R.color.white));
            } else {
                if (mLowBitAmbient) {
                    mTimePaint.setAntiAlias(true);
                    mDatePaint.setAntiAlias(true);
                }
                mDatePaint.setColor(mResources.getColor(R.color.sunshine_light_blue));
            }

            invalidate();

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
//            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            // Draw H:MM in ambient mode and in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            String textTime = String.format("%d:%02d", mCalendar.get(Calendar.HOUR_OF_DAY),
                    mCalendar.get(Calendar.MINUTE));

            String textDate = String.format("%s, %s %d %d",
                    weekDayNames[mCalendar.get(Calendar.DAY_OF_WEEK)],
                    monthDayNames[mCalendar.get(Calendar.MONTH)],
                    mCalendar.get(Calendar.DAY_OF_MONTH),
                    mCalendar.get(Calendar.YEAR));

            if (!mAmbient) {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

                canvas.drawText(textTime, mXOffsetTime, mYOffsetTime, mTimePaint);
                canvas.drawText(textDate, mXOffsetDate, mYOffsetDate, mDatePaint);

                canvas.drawLine((3 * bounds.width()) / 8, getmYOffsetDivider, (5 * bounds.width()) / 8,
                        getmYOffsetDivider,
                        mDatePaint);


                if (weatherAvailable) {

                    float x = mXOffsetWeather;

                    Bitmap bitmap = BitmapFactory.decodeResource
                            (mResources, getArtResourceForWeatherCondition(weatherID));

                    Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap, 60, 60, true);

                    canvas.drawBitmap(bitmap1, x, mYOffsetWeather - 45, mWeatherMaxPaint);

                    x += 70;

                    canvas.drawText(maxTemp + "", x, mYOffsetWeather, mWeatherMaxPaint);

                    x += mWeatherMaxPaint.measureText(maxTemp);

                    canvas.drawText(minTemp + "", x, mYOffsetWeather, mWeatherMinPaint);
                }
            } else {
                canvas.drawColor(Color.BLACK);
                canvas.drawText(textTime, mXOffsetTime, mYOffsetTime, mTimePaint);
                canvas.drawText(textDate, mXOffsetDate, mYOffsetDate, mDatePaint);
            }

        }

        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void showWeather(WeatherListenerService.WeatherData weatherData) {
            weatherAvailable = true;
            weatherID = weatherData.weatherId;
            maxTemp = weatherData.tempHigh;
            minTemp = weatherData.tempLow;
            invalidate();
        }

        private int getArtResourceForWeatherCondition(int weatherId) {
            // Based on weather code data found at:
            // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
            if (weatherId >= 200 && weatherId <= 232) {
                return R.drawable.art_storm;
            } else if (weatherId >= 300 && weatherId <= 321) {
                return R.drawable.art_light_rain;
            } else if (weatherId >= 500 && weatherId <= 504) {
                return R.drawable.art_rain;
            } else if (weatherId == 511) {
                return R.drawable.art_snow;
            } else if (weatherId >= 520 && weatherId <= 531) {
                return R.drawable.art_rain;
            } else if (weatherId >= 600 && weatherId <= 622) {
                return R.drawable.art_snow;
            } else if (weatherId >= 701 && weatherId <= 761) {
                return R.drawable.art_fog;
            } else if (weatherId == 761 || weatherId == 781) {
                return R.drawable.art_storm;
            } else if (weatherId == 800) {
                return R.drawable.art_clear;
            } else if (weatherId == 801) {
                return R.drawable.art_light_clouds;
            } else if (weatherId >= 802 && weatherId <= 804) {
                return R.drawable.art_clouds;
            }
            return -1;
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(
                            mGoogleApiClient).await();

                    for (Node node : nodes.getNodes()) {
                        Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, node.getId(), "/fetch-weather-data",
                                new byte[0]
                        ).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                if(sendMessageResult.getStatus().isSuccess()) {
                                    Log.d("xxx", "request for weatherdata");
                                }
                            }
                        });
                    }
                }
            }).start();
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    }


}
