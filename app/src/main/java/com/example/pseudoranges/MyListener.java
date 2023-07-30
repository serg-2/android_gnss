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

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * A class representing a UI logger for the application. Its responsibility is show information in
 * the UI.
 */
public class MyListener implements MeasurementListener {

    public static final double speed_light = 2.99792458e8;
    public static final String unknown_phase = "unkno";
    private static final String TAG = "LISTENER";
    private int leapseconds = 18;

    private MainActivity activity;

    public MyListener(MainActivity mainActivity) {
        activity = mainActivity;
    }

    @Override
    public void onProviderEnabled(String provider) {
        logLocationEvent("onProviderEnabled: " + provider);
    }

    @Override
    public void onTTFFReceived(long l) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        logLocationEvent("onProviderDisabled: " + provider);
    }

    @Override
    public void onLocationChanged(Location location) {
        logLocationEvent("onLocationChanged: " + location + "\n");
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
        String message =
                String.format(
                        "onStatusChanged: provider=%s, status=%s, extras=%s",
                        provider, locationStatusToString(status), extras);
        logLocationEvent(message);
    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        // FIRST! Parse Clock
        parseClock(event.getClock());

        for (GnssMeasurement measurement : event.getMeasurements()) {
            parseMeasurement(measurement);
        }

        //activity.tv1Text.postValue(toStringClockClass());
        //activity.tv2Text.postValue(toStringMeasurementMap());
    }

    private void parseClock(GnssClock gnssClock) {
        activity.mainViewModel.gc().AgeData = System.currentTimeMillis();

        activity.mainViewModel.gc().TimeNanos = gnssClock.getTimeNanos();
        activity.mainViewModel.gc().HardwareClockDiscontinuityCount = gnssClock.getHardwareClockDiscontinuityCount();

        // True
        if (gnssClock.hasLeapSecond()) {
            activity.mainViewModel.gc().LeapSecond = gnssClock.getLeapSecond();
        }

        // True
        if (gnssClock.hasTimeUncertaintyNanos()) {
            activity.mainViewModel.gc().TimeUncertaintyNanos = gnssClock.getTimeUncertaintyNanos();
        }

        // True
        if (gnssClock.hasFullBiasNanos()) {
            activity.mainViewModel.gc().FullBiasNanos = gnssClock.getFullBiasNanos();
        }

        // True
        if (gnssClock.hasBiasNanos()) {
            activity.mainViewModel.gc().BiasNanos = gnssClock.getBiasNanos();
        }

        // True, but no values
        if (gnssClock.hasBiasUncertaintyNanos()) {
            activity.mainViewModel.gc().BiasUncertaintyNanos = gnssClock.getBiasUncertaintyNanos();
        }

        // True
        if (gnssClock.hasDriftNanosPerSecond()) {
            activity.mainViewModel.gc().DriftNanosPerSecond = gnssClock.getDriftNanosPerSecond();
        }

        // True
        if (gnssClock.hasDriftUncertaintyNanosPerSecond()) {
            activity.mainViewModel.gc().DriftUncertaintyNanosPerSecond = gnssClock.getDriftUncertaintyNanosPerSecond();
        }

        // Additional
        double weekNumber = Math.floor(-(gnssClock.getFullBiasNanos() * 1e-9 / 604800));
        double weekNumberNanos = weekNumber * 604800 * 1e9;

        double tRxNanos = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - weekNumberNanos;

        if (gnssClock.hasBiasNanos()) {
            tRxNanos = tRxNanos - gnssClock.getBiasNanos();
        }

        activity.mainViewModel.gc().TRxNanos = tRxNanos;

        // 251
        //gnssClock.getHardwareClockDiscontinuityCount();
    }

    private void parseMeasurement(GnssMeasurement measurement) {
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) {
            //Log.e("MAIN", "Meas: " + measurement);
        }

        String constellation = getConstellationName(measurement.getConstellationType());
        Integer svid = measurement.getSvid();

        String bandName = getBandName(measurement.getConstellationType(), measurement.getCarrierFrequencyHz());

        // Write measurement state to viewModel
        activity.mainViewModel.setState(measurement.getAccumulatedDeltaRangeState());

        // Check Constellation exists in map
        if (!activity.mainViewModel.gm().containsKey(constellation)) {
            activity.mainViewModel.gm().put(constellation, new LinkedHashMap<>());
        }

        // Check SVID exists in Constellation
        if (!activity.mainViewModel.gm().get(constellation).containsKey(svid)) {
            activity.mainViewModel.gm().get(constellation).put(svid, new LinkedHashMap<>());
        }

        // Check Freq exists in SVID
        if (!activity.mainViewModel.gm().get(constellation).get(svid).containsKey(bandName)) {
            activity.mainViewModel.gm().get(constellation).get(svid).put(bandName, new Satellite());
        }


        Satellite nSatellite = activity.mainViewModel.gm().get(constellation).get(svid).get(bandName);

        nSatellite.AgeData = System.currentTimeMillis();

        nSatellite.TimeOffsetNanos = measurement.getTimeOffsetNanos();
        nSatellite.State = measurement.getState();
        nSatellite.ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
        nSatellite.ReceivedSvTimeUncertaintyNanos = measurement.getReceivedSvTimeUncertaintyNanos();
        nSatellite.Cn0DbHz = measurement.getCn0DbHz();
        nSatellite.PseudorangeRateMetersPerSecond = measurement.getPseudorangeRateMetersPerSecond();
        nSatellite.PseudorangeRateUncertaintyMetersPerSeconds = measurement.getPseudorangeRateUncertaintyMetersPerSecond();

        nSatellite.MultipathIndicator = measurement.getMultipathIndicator();

        // 16 (GnssMeasurement.ADR_STATE_HALF_CYCLE_REPORTED)
        if (measurement.getAccumulatedDeltaRangeState() != GnssMeasurement.ADR_STATE_UNKNOWN) {
            nSatellite.AccumulatedDeltaRangeState = measurement.getAccumulatedDeltaRangeState();
            nSatellite.AccumulatedDeltaRangeMeters = measurement.getAccumulatedDeltaRangeMeters();
            nSatellite.AccumulatedDeltaRangeUncertaintyMeters = measurement.getAccumulatedDeltaRangeUncertaintyMeters();
        } else {
            nSatellite.AccumulatedDeltaRangeState = 0;
            nSatellite.AccumulatedDeltaRangeMeters = 0;
            nSatellite.AccumulatedDeltaRangeUncertaintyMeters = 0;
        }

        // True
        if (measurement.hasCarrierFrequencyHz()) {
            nSatellite.CarrierFrequencyHz = measurement.getCarrierFrequencyHz();
        }

        // false
        if (measurement.hasCarrierCycles()) {
            nSatellite.CarrierCycles = measurement.getCarrierCycles();
        }

        // false
        if (measurement.hasCarrierPhase()) {
            nSatellite.CarrierPhase = measurement.getCarrierPhase();
        }

        // False
        if (measurement.hasCarrierPhaseUncertainty()) {
            nSatellite.CarrierPhaseUncertainty = measurement.getCarrierPhaseUncertainty();
        }

        // False
        if (measurement.hasSnrInDb()) {
            nSatellite.SnrInDb = measurement.getSnrInDb();
        }

        // True
        if (measurement.hasAutomaticGainControlLevelDb()) {
            nSatellite.AgcDb = measurement.getAutomaticGainControlLevelDb();
        }

        // True
        if (measurement.hasCarrierFrequencyHz()) {
            nSatellite.CarrierFreqHz = measurement.getCarrierFrequencyHz();
        }

        // Additional
        double tRxNanos;
        if (measurement.getTimeOffsetNanos() != 0) {
            tRxNanos = activity.mainViewModel.gc().TRxNanos + measurement.getTimeOffsetNanos();
        } else {
            tRxNanos = activity.mainViewModel.gc().TRxNanos;
        }

        double tRxSeconds = tRxNanos * 1e-9;
        double tTxSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;

        // GLONASS
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GLONASS) {
            double tRxSeconds_GLO = tRxSeconds % 86400;
            double tTxSeconds_GLO = tTxSeconds - 10800 + leapseconds;
            if (tTxSeconds_GLO < 0) {
                tTxSeconds_GLO = tTxSeconds_GLO + 86400;
            }
            tRxSeconds = tRxSeconds_GLO;
            tTxSeconds = tTxSeconds_GLO;
        }

        // BEIDOU
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_BEIDOU) {
            double tRxSeconds_BDS = tRxSeconds;
            double tTxSeconds_BDS = tTxSeconds + leapseconds - 4;
            if (tTxSeconds_BDS > 604800) {
                tTxSeconds_BDS = tTxSeconds_BDS - 604800;
            }
            tRxSeconds = tRxSeconds_BDS;
            tTxSeconds = tTxSeconds_BDS;
        }

        double prSeconds = tRxSeconds - tTxSeconds;
        nSatellite.PRTIME_OLD = prSeconds;

        boolean iRollover = prSeconds > 604800 / 2;
        if (iRollover) {
            double delS = Math.round(prSeconds / 604800) * 604800;
            double prS = prSeconds - delS;
            double maxBiasSeconds = 10;
            if (prS > maxBiasSeconds) {
                Log.e("RollOver", "Rollover Error");
                iRollover = true;
            } else {
                tRxSeconds = tRxSeconds - delS;
                prSeconds = tRxSeconds - tTxSeconds;
                iRollover = false;
            }
        }
        nSatellite.PRTIME_NEW = prSeconds;

        // Valid or not?
        if (prSeconds > 0.040 && prSeconds < 0.2) {
            nSatellite.Valid = true;
        } else {
            nSatellite.Valid = false;
        }

        double prm = prSeconds * speed_light;

        // For QZSS Create Array
        /*
        if(iRollover){
            array[arrayRow][1] = "ROLLOVER_ERROR";
            prm = 0.0;
        }else if(prSeconds < 0 || prSeconds > 0.5){
            array[arrayRow][1] = "INVALID_VALUE";
            prm = 0.0;
        }else if(getStateName(measurement.getState()) == "1") {
                array[arrayRow][1] = String.format("%14.3f", prm);
                CheckClockSync = true;
            }else {
                array[arrayRow][1] = getStateName(measurement.getState());
            }
         */
        int index = measurement.getSvid();
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GLONASS) {
            index = index + 64;
        }
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_BEIDOU) {
            index = index + 200;
        }
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GALILEO) {
            index = index + 235;
        }

        nSatellite.PRM = prm;
        nSatellite.INDEX = index;
        nSatellite.BandName = bandName;

        // PhaseShift calculation -----------------------------------------------
        // if (((byte) measurement.getAccumulatedDeltaRangeState() & 0b00000001) == 1) {
        // if (((byte) measurement.getAccumulatedDeltaRangeState() & GnssMeasurement.ADR_STATE_VALID) == GnssMeasurement.ADR_STATE_VALID) {
        // After some time ADR_STATE_HALF_CYCLE_REPORTED Only bit enabled. (After lock coordinate?)
        double wavelength = speed_light / measurement.getCarrierFrequencyHz();
        double fullDelta = measurement.getPseudorangeRateMetersPerSecond();
        double deltaRemainderPart = (fullDelta / wavelength) % 1;
        nSatellite.PhaseShift = deltaRemainderPart;
        // Full phase 100%
        // double deltaRemainderPartInPercent = deltaRemainderPart * 100;
        // Full phase 360 degree
        double deltaRemainderPartInDegrees = deltaRemainderPart * 360;
        //nSatellite.PHASE = String.format(Locale.ENGLISH, "%+2.0f%%", deltaRemainderPartInPercent);

        if (nSatellite.AccumulatedDeltaRangeMeters != 0) {
            nSatellite.PHASE = String.format(Locale.ENGLISH, "%+04.0f\u00B0", deltaRemainderPartInDegrees);
        } else {
            nSatellite.PHASE = unknown_phase;
        }
        // End PhaseShift calculation ---------------------------------------------

        // Log features
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) {
            //Log.e("Satellite", "tRxSeconds: "+ tRxNanos+ " getReceivedSvTimeNanos(): " + measurement.getReceivedSvTimeNanos());
            //Log.e("Satellite" , String.format("Difference: %5.3f сек", prSeconds));
            //Log.e("Satellite" , String.format("PseudoRange: %7.0f км", prm / 1000));
        }

    }

    private String getBandName(int constellationType, float carrierFrequencyHz) {
        // Frequency in kHz
        int frequency = (int) Math.round(carrierFrequencyHz * 1e-3);
        switch (constellationType) {
            case GnssStatus.CONSTELLATION_GPS:
                switch (frequency) {
                    case 1575420:
                        return "L1";
                    case 1227600:
                        return "L2";
                    case 1176450:
                        return "L5";
                    default:
                        Log.e(TAG, "UNKNOWN GPS Frequency!" + frequency);
                        return "U_GPS";
                }
            case GnssStatus.CONSTELLATION_SBAS:
                switch (frequency) {
                    case 1575420:
                        return "L1";
                    case 1176450:
                        return "L5";
                    default:
                        Log.e(TAG, "UNKNOWN SBAS Frequency!" + frequency);
                        return "U_SBAS";
                }
            case GnssStatus.CONSTELLATION_GLONASS:
                // L1 1598062.5 kHz - 1605375 kHz
                if (frequency >= 1598062 && frequency <= 1605375) {
                    return "L1";
                    // L2 1242937.5 kHz - 1248625 kHz
                } else if (frequency >= 1242937 && frequency <= 1248625) {
                    return "L2";
                } else {
                    Log.e(TAG, "UNKNOWN GLONASS Frequency!" + frequency);
                    return "U_GLO";
                }
            case GnssStatus.CONSTELLATION_QZSS:
                switch (frequency) {
                    case 1575420:
                        return "L1";
                    case 1227600:
                        return "L2";
                    case 1176450:
                        return "L5";
                    case 1278750:
                        return "L6";
                    default:
                        Log.e(TAG, "UNKNOWN QZSS Frequency!" + frequency);
                        return "U_QZSS";
                }
            case GnssStatus.CONSTELLATION_BEIDOU:
                switch (frequency) {
                    case 1561098:
                    case 1575420:
                        return "L1";
                    case 1207140:
                    case 1176450:
                        return "L2";
                    case 1268520:
                        return "L3";
                    default:
                        Log.e(TAG, "UNKNOWN BeiDou Frequency!" + frequency);
                        return "U_BeiDou";
                }
            case GnssStatus.CONSTELLATION_GALILEO:
                switch (frequency) {
                    case 1575420:
                        return "L1";
                    case 1176450:
                        return "5a";
                    case 1207140:
                        return "5b";
                    case 1278750:
                        return "L6";
                    default:
                        Log.e(TAG, "UNKNOWN Galileo Frequency!" + frequency);
                        return "U_Galileo";
                }
            default:
                return "UNKNOWN";
        }
    }

    /*
    private String toStringClock(GnssClock gnssClock) {
        final String format = "%-4s = %s\n";
        StringBuilder builder = new StringBuilder();
        DecimalFormat numberFormat = new DecimalFormat("#0.000");
        if (gnssClock.hasLeapSecond()) {
            builder.append(String.format(format, "LeapSecond", gnssClock.getLeapSecond()));
        }

        builder.append(String.format(format, "TimeNanos", gnssClock.getTimeNanos()));
        if (gnssClock.hasTimeUncertaintyNanos()) {
            builder.append(
                    String.format(format, "TimeUncertaintyNanos", gnssClock.getTimeUncertaintyNanos()));
        }

        if (gnssClock.hasFullBiasNanos()) {
            builder.append(String.format(format, "FullBiasNanos", gnssClock.getFullBiasNanos()));
        }

        if (gnssClock.hasBiasNanos()) {
            builder.append(String.format(format, "BiasNanos", gnssClock.getBiasNanos()));
        }
        if (gnssClock.hasBiasUncertaintyNanos()) {
            builder.append(
                    String.format(
                            format,
                            "BiasUncertaintyNanos",
                            numberFormat.format(gnssClock.getBiasUncertaintyNanos())));
        }

        if (gnssClock.hasDriftNanosPerSecond()) {
            builder.append(
                    String.format(
                            format,
                            "DriftNanosPerSecond",
                            numberFormat.format(gnssClock.getDriftNanosPerSecond())));
        }

        if (gnssClock.hasDriftUncertaintyNanosPerSecond()) {
            builder.append(
                    String.format(
                            format,
                            "DriftUncertaintyNanosPerSecond",
                            numberFormat.format(gnssClock.getDriftUncertaintyNanosPerSecond())));
        }

        builder.append(
                String.format(
                        format,
                        "HardwareClockDiscontinuityCount",
                        gnssClock.getHardwareClockDiscontinuityCount()));

        return builder.toString();
    }

    private String toStringMeasurement(GnssMeasurement measurement) {
        final String format = "%-4s = %s\n";
        StringBuilder builder = new StringBuilder("\n");
        DecimalFormat numberFormat = new DecimalFormat("#0.000");
        DecimalFormat numberFormat1 = new DecimalFormat("#0.000E00");
        builder.append(String.format(format, "Svid", measurement.getSvid()));
        builder.append(String.format(format, "ConstellationType", measurement.getConstellationType()));
        builder.append(String.format(format, "TimeOffsetNanos", measurement.getTimeOffsetNanos()));

        builder.append(String.format(format, "State", measurement.getState()));

        builder.append(
                String.format(format, "ReceivedSvTimeNanos", measurement.getReceivedSvTimeNanos()));
        builder.append(
                String.format(
                        format,
                        "ReceivedSvTimeUncertaintyNanos",
                        measurement.getReceivedSvTimeUncertaintyNanos()));

        builder.append(String.format(format, "Cn0DbHz", numberFormat.format(measurement.getCn0DbHz())));

        builder.append(
                String.format(
                        format,
                        "PseudorangeRateMetersPerSecond",
                        numberFormat.format(measurement.getPseudorangeRateMetersPerSecond())));
        builder.append(
                String.format(
                        format,
                        "PseudorangeRateUncertaintyMetersPerSeconds",
                        numberFormat.format(measurement.getPseudorangeRateUncertaintyMetersPerSecond())));

        if (measurement.getAccumulatedDeltaRangeState() != GnssMeasurement.ADR_STATE_UNKNOWN) {
            builder.append(
                    String.format(
                            format, "AccumulatedDeltaRangeState", measurement.getAccumulatedDeltaRangeState()));

            builder.append(
                    String.format(
                            format,
                            "AccumulatedDeltaRangeMeters",
                            numberFormat.format(measurement.getAccumulatedDeltaRangeMeters())));
            builder.append(
                    String.format(
                            format,
                            "AccumulatedDeltaRangeUncertaintyMeters",
                            numberFormat1.format(measurement.getAccumulatedDeltaRangeUncertaintyMeters())));
        }

        if (measurement.hasCarrierFrequencyHz()) {
            builder.append(
                    String.format(format, "CarrierFrequencyHz", measurement.getCarrierFrequencyHz()));
        }

        if (measurement.hasCarrierCycles()) {
            builder.append(String.format(format, "CarrierCycles", measurement.getCarrierCycles()));
        }

        if (measurement.hasCarrierPhase()) {
            builder.append(String.format(format, "CarrierPhase", measurement.getCarrierPhase()));
        }

        if (measurement.hasCarrierPhaseUncertainty()) {
            builder.append(
                    String.format(
                            format, "CarrierPhaseUncertainty", measurement.getCarrierPhaseUncertainty()));
        }

        builder.append(
                String.format(format, "MultipathIndicator", measurement.getMultipathIndicator()));

        if (measurement.hasSnrInDb()) {
            builder.append(String.format(format, "SnrInDb", measurement.getSnrInDb()));
        }



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (measurement.hasAutomaticGainControlLevelDb()) {
                builder.append(
                        String.format(format, "AgcDb", measurement.getAutomaticGainControlLevelDb()));
            }
            if (measurement.hasCarrierFrequencyHz()) {
                builder.append(String.format(format, "CarrierFreqHz", measurement.getCarrierFrequencyHz()));
            }
        }

        return builder.toString();
    }
    */

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {
        Log.i("My Listener", "Status changed to: " + gnssMeasurementsStatusToString(status));
    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
        logNavigationMessageEvent("onGnssNavigationMessageReceived: " + event);
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
        logNavigationMessageEvent("onStatusChanged: " + getGnssNavigationMessageStatus(status));
    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {
        logStatusEvent("onGnssStatusChanged: " + gnssStatusToString(gnssStatus));
    }

    @Override
    public void onNmeaReceived(long timestamp, String s) {
        logNmeaEvent(String.format("onNmeaReceived: timestamp=%d, %s", timestamp, s));
    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {
        Log.i("My Listener", "Listener Registered!");
    }

    private void logNavigationMessageEvent(String event) {
        Log.e("MAIN", "Log Navigation event!");
    }

    private void logStatusEvent(String event) {
        Log.e("MAIN", "Log STATUS event!");
    }

    private void logNmeaEvent(String event) {
        Log.e("MAIN", "Log NMEA event!");
    }

    private String locationStatusToString(int status) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                return "AVAILABLE";
            case LocationProvider.OUT_OF_SERVICE:
                return "OUT_OF_SERVICE";
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                return "TEMPORARILY_UNAVAILABLE";
            default:
                return "<Unknown>";
        }
    }

    private String gnssMeasurementsStatusToString(int status) {
        switch (status) {
            case GnssMeasurementsEvent.Callback.STATUS_NOT_SUPPORTED:
                return "NOT_SUPPORTED";
            case GnssMeasurementsEvent.Callback.STATUS_READY:
                return "READY";
            case GnssMeasurementsEvent.Callback.STATUS_LOCATION_DISABLED:
                return "GNSS_LOCATION_DISABLED";
            default:
                return "<Unknown>";
        }
    }

    private String getGnssNavigationMessageStatus(int status) {
        switch (status) {
            case GnssNavigationMessage.STATUS_UNKNOWN:
                return "Status Unknown";
            case GnssNavigationMessage.STATUS_PARITY_PASSED:
                return "READY";
            case GnssNavigationMessage.STATUS_PARITY_REBUILT:
                return "Status Parity Rebuilt";
            default:
                return "<Unknown>";
        }
    }

    private String gnssStatusToString(GnssStatus gnssStatus) {

        StringBuilder builder = new StringBuilder("SATELLITE_STATUS | [Satellites:\n");
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            builder
                    .append("Constellation = ")
                    .append(getConstellationName(gnssStatus.getConstellationType(i)))
                    .append(", ");
            builder.append("Svid = ").append(gnssStatus.getSvid(i)).append(", ");
            builder.append("Cn0DbHz = ").append(gnssStatus.getCn0DbHz(i)).append(", ");
            builder.append("Elevation = ").append(gnssStatus.getElevationDegrees(i)).append(", ");
            builder.append("Azimuth = ").append(gnssStatus.getAzimuthDegrees(i)).append(", ");
            builder.append("hasEphemeris = ").append(gnssStatus.hasEphemerisData(i)).append(", ");
            builder.append("hasAlmanac = ").append(gnssStatus.hasAlmanacData(i)).append(", ");
            builder.append("usedInFix = ").append(gnssStatus.usedInFix(i)).append("\n");
        }
        builder.append("]");
        return builder.toString();
    }

    private void logLocationEvent(String event) {
        Log.e("MAIN", "Log LOCATION event!");
    }

    private String getConstellationName(int id) {
        switch (id) {
            case 1:
                return "GPS";
            case 2:
                return "SBAS";
            case 3:
                return "GLONASS";
            case 4:
                return "QZSS";
            case 5:
                return "BEIDOU";
            case 6:
                return "GALILEO";
            default:
                return "UNKNOWN";
        }
    }

}
