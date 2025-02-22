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

import static com.example.pseudoranges.StatusChangeHelper.gnssStatusToString;

import android.location.GnssAutomaticGainControl;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.util.Log;

import com.example.pseudoranges.parsers.ClockParser;
import com.example.pseudoranges.parsers.MeasurementParser;

import java.util.Collection;

/**
 * A class representing a UI logger for the application. Its responsibility is show information in
 * the UI.
 */
public class MyListener implements MeasurementListener {

    private static final String TAG = "LISTENER";

    private final MeasurementParser measurementParser;
    private final ClockParser clockParser;

    public MyListener(MainActivity mainActivity) {
        measurementParser = new MeasurementParser(mainActivity);
        clockParser = new ClockParser(mainActivity);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e("onProviderEnabled: ", provider);
    }

    @Override
    public void onTTFFReceived(long l) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e("onProviderDisabled: ", provider);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("onLocationChanged: ", location + "\n");
    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        // FIRST! Parse Clock
        clockParser.parseClock(event.getClock(), event.getMeasurements().size());
        // Unused
        Collection<GnssAutomaticGainControl> controls = event.getGnssAutomaticGainControls();
        event.getMeasurements().forEach(measurementParser::parseMeasurement);
    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
        Log.e("onGnssNavigationMessageReceived: ", event.toString());
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
        Log.e("onStatusChanged: ", getGnssNavigationMessageStatus(status));
    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {
        Log.e("onGnssStatusChanged: ", gnssStatusToString(gnssStatus));
    }

    @Override
    public void onNmeaReceived(long timestamp, String s) {
        Log.e("onNmeaReceived: ", "timestamp=%d, %s".formatted(
                timestamp,
                s
            )
        );
    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {
        Log.i(TAG, "Listener Registered!");
    }

    private String getGnssNavigationMessageStatus(int status) {
        return switch (status) {
            case GnssNavigationMessage.STATUS_UNKNOWN -> "Status Unknown";
            case GnssNavigationMessage.STATUS_PARITY_PASSED -> "READY";
            case GnssNavigationMessage.STATUS_PARITY_REBUILT -> "Status Parity Rebuilt";
            default -> "<Unknown>";
        };
    }

}
