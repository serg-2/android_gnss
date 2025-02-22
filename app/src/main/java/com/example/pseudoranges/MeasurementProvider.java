/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.pseudoranges;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A container for measurement-related API calls. It binds the measurement providers with the
 * various {@link MeasurementListener} implementations.
 */
@SuppressLint("MissingPermission")
public class MeasurementProvider {

    public static final String TAG = "MeasurementProvider";

    private static final long LOCATION_RATE_GPS_MS = TimeUnit.SECONDS.toMillis(1L);
    private static final long LOCATION_RATE_NETWORK_MS = TimeUnit.SECONDS.toMillis(60L);

    private boolean mLogLocations = true;
    private boolean mLogNavigationMessages = true;
    private boolean mLogMeasurements = true;
    private boolean mLogStatuses = true;
    private boolean mLogNmeas = true;
    private long registrationTimeNanos = 0L;
    private long firstLocationTimeNanos = 0L;
    private long ttff = 0L;
    private boolean firstTime = true;

    private final List<MeasurementListener> mListeners;

    private final LocationManager mLocationManager;
    private final android.location.LocationListener mLocationListener =
            new android.location.LocationListener() {
                @Override
                public void onProviderEnabled(@NonNull String provider) {
                    if (mLogLocations) {
                        for (MeasurementListener listener : mListeners) {
                            listener.onProviderEnabled(provider);
                        }
                    }
                }

                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    if (mLogLocations) {
                        for (MeasurementListener logger : mListeners) {
                            logger.onProviderDisabled(provider);
                        }
                    }
                }

                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (firstTime && Objects.equals(location.getProvider(), LocationManager.GPS_PROVIDER)) {
                        if (mLogLocations) {
                            for (MeasurementListener logger : mListeners) {
                                firstLocationTimeNanos = SystemClock.elapsedRealtimeNanos();
                                ttff = firstLocationTimeNanos - registrationTimeNanos;
                                logger.onTTFFReceived(ttff);
                            }
                        }
                        firstTime = false;
                    }
                    if (mLogLocations) {
                        for (MeasurementListener logger : mListeners) {
                            logger.onLocationChanged(location);
                        }
                    }
                }
            };

    private final Consumer<Location> locationConsumer =
        new Consumer<>() {
            @Override
            public void accept(Location location) {
                if (firstTime && Objects.equals(location.getProvider(), LocationManager.GPS_PROVIDER)) {
                    if (mLogLocations) {
                        for (MeasurementListener logger : mListeners) {
                            firstLocationTimeNanos = SystemClock.elapsedRealtimeNanos();
                            ttff = firstLocationTimeNanos - registrationTimeNanos;
                            logger.onTTFFReceived(ttff);
                        }
                    }
                    firstTime = false;
                }
                if (mLogLocations) {
                    for (MeasurementListener logger : mListeners) {
                        logger.onLocationChanged(location);
                    }
                }
            }
        };

    private final com.google.android.gms.location.LocationListener mFusedLocationListener =
            new com.google.android.gms.location.LocationListener() {

                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (firstTime && Objects.equals(location.getProvider(), LocationManager.GPS_PROVIDER)) {
                        if (mLogLocations) {
                            for (MeasurementListener logger : mListeners) {
                                firstLocationTimeNanos = SystemClock.elapsedRealtimeNanos();
                                ttff = firstLocationTimeNanos - registrationTimeNanos;
                                logger.onTTFFReceived(ttff);
                            }
                        }
                        firstTime = false;
                    }
                    if (mLogLocations) {
                        for (MeasurementListener logger : mListeners) {
                            logger.onLocationChanged(location);
                        }
                    }
                }
            };

    private final GnssMeasurementsEvent.Callback gnssMeasurementsEventListener =
            new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                    if (mLogMeasurements) {
                        for (MeasurementListener logger : mListeners) {
                            logger.onGnssMeasurementsReceived(event);
                        }
                    }
                }
            };

    private final GnssNavigationMessage.Callback gnssNavigationMessageListener =
            new GnssNavigationMessage.Callback() {
                @Override
                public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                    // Xiaomi not support
                    Log.e(TAG, "Received navigation message!");
                    if (mLogNavigationMessages) {
                        for (MeasurementListener logger : mListeners) {
                            logger.onGnssNavigationMessageReceived(event);
                        }
                    }
                }
            };

    private final GnssStatus.Callback gnssStatusListener =
            new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                }

                @Override
                public void onStopped() {
                }

                @Override
                public void onFirstFix(int ttff) {
                }

                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    for (MeasurementListener logger : mListeners) {
                        logger.onGnssStatusChanged(status);
                    }
                }
            };

    private final OnNmeaMessageListener nmeaListener =
            new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String s, long l) {
                    if (mLogNmeas) {
                        for (MeasurementListener logger : mListeners) {
                            logger.onNmeaReceived(l, s);
                        }
                    }
                }
            };

    public MeasurementProvider(Context context, MeasurementListener... loggers) {
        this.mListeners = Arrays.asList(loggers);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public LocationManager getLocationManager() {
        return mLocationManager;
    }

    public void setLogLocations(boolean value) {
        mLogLocations = value;
    }

    public boolean canLogLocations() {
        return mLogLocations;
    }

    public void setLogNavigationMessages(boolean value) {
        mLogNavigationMessages = value;
    }

    public boolean canLogNavigationMessages() {
        return mLogNavigationMessages;
    }

    public void setLogMeasurements(boolean value) {
        mLogMeasurements = value;
    }

    public boolean canLogMeasurements() {
        return mLogMeasurements;
    }

    public void setLogStatuses(boolean value) {
        mLogStatuses = value;
    }

    public boolean canLogStatuses() {
        return mLogStatuses;
    }

    public void setLogNmeas(boolean value) {
        mLogNmeas = value;
    }

    public boolean canLogNmeas() {
        return mLogNmeas;
    }

    // Temporary Unused
    public void registerLocation() {
        boolean isGpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGpsProviderEnabled) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_RATE_NETWORK_MS,
                    0.0f /* minDistance */,
                    mLocationListener);
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_RATE_GPS_MS,
                    0.0f /* minDistance */,
                    mLocationListener);
        }
        logRegistration("LocationUpdates", isGpsProviderEnabled);
    }

    public void unregisterLocation() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    /* DEPRECATED
    public void registerFusedLocation() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000
        )
                .setMinUpdateIntervalMillis(100)
                .build();

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, mFusedLocationListener);
    }

    public void unRegisterFusedLocation() {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mFusedLocationListener);
        }
    }
     */

    public void registerSingleNetworkLocation() {
        boolean isNetworkProviderEnabled =
                mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isNetworkProviderEnabled) {
            mLocationManager.getCurrentLocation(
                LocationManager.NETWORK_PROVIDER,
                null,
                Executors.newSingleThreadExecutor(),
                locationConsumer
            );

            // DEPRECATED
//            mLocationManager.requestSingleUpdate(
//                    LocationManager.NETWORK_PROVIDER, mLocationListener, null);

        }
        logRegistration("LocationUpdates", isNetworkProviderEnabled);
    }

    public void registerSingleGpsLocation() {
        boolean isGpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGpsProviderEnabled) {
            this.firstTime = true;
            registrationTimeNanos = SystemClock.elapsedRealtimeNanos();

            // DEPRECATED
            // mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);
            mLocationManager.getCurrentLocation(
                LocationManager.NETWORK_PROVIDER,
                null,
                Executors.newSingleThreadExecutor(),
                locationConsumer
            );
        }
        logRegistration("LocationUpdates", isGpsProviderEnabled);
    }

    public void registerMeasurements() {
        logRegistration(
                "GnssMeasurements",
                mLocationManager.registerGnssMeasurementsCallback(
                    Executors.newSingleThreadExecutor(),
                    gnssMeasurementsEventListener
                ));
    }

    public void unregisterMeasurements() {
        mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsEventListener);
    }

    public void registerNavigationMessages() {
        logRegistration(
                "GpsNavigationMessage",
                mLocationManager.registerGnssNavigationMessageCallback(
                    Executors.newSingleThreadExecutor(),
                    gnssNavigationMessageListener
                ));
    }

    public void unregisterNavigationMessages() {
        mLocationManager.unregisterGnssNavigationMessageCallback(gnssNavigationMessageListener);
    }

    public void registerGnssStatus() {
        logRegistration("GnssStatus", mLocationManager.registerGnssStatusCallback(
            Executors.newSingleThreadExecutor(),
            gnssStatusListener
        ));
    }

    public void unregisterGnssStatus() {
        mLocationManager.unregisterGnssStatusCallback(gnssStatusListener);
    }

    public void registerNmea() {
        logRegistration("Nmea", mLocationManager.addNmeaListener(
            Executors.newSingleThreadExecutor(),
            nmeaListener
        ));
    }

    public void unregisterNmea() {
        mLocationManager.removeNmeaListener(nmeaListener);
    }

    public void registerAll() {
        registerLocation();
        registerMeasurements();
        registerNavigationMessages();
        registerGnssStatus();
        registerNmea();
    }

    public void unregisterAll() {
        unregisterLocation();
        unregisterMeasurements();
        unregisterNavigationMessages();
        unregisterGnssStatus();
        unregisterNmea();
    }

    private void logRegistration(String listener, boolean result) {
        for (MeasurementListener logger : mListeners) {
            logger.onListenerRegistration(listener, result);
        }
    }

}
