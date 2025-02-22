package com.example.pseudoranges.parsers;

import static android.location.GnssMeasurement.ADR_STATE_VALID;
import static com.example.pseudoranges.BandHelper.getBandName;
import static com.example.pseudoranges.Consts.LEAP_SECONDS;
import static com.example.pseudoranges.Consts.SPEED_LIGHT;
import static com.example.pseudoranges.Consts.UNKNOWN_PHASE;

import android.location.GnssMeasurement;
import android.location.GnssStatus;
import android.util.Log;

import com.example.pseudoranges.BandEnum;
import com.example.pseudoranges.Clock;
import com.example.pseudoranges.ConstellationEnum;
import com.example.pseudoranges.MainActivity;
import com.example.pseudoranges.Satellite;
import com.example.pseudoranges.models.MainViewModel;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MeasurementParser {

    private final Map<ConstellationEnum, Map<Integer, Map<BandEnum, Satellite>>> mainMap;
    private final Clock clock;
    private final MainViewModel viewModel;

    public MeasurementParser(MainActivity mainActivity) {
        viewModel = mainActivity.mainViewModel;
        mainMap = viewModel.gm();
        clock = viewModel.gc();
    }

    public void parseMeasurement(GnssMeasurement measurement) {
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) {
            //Log.e("MAIN", "Meas: " + measurement);
        }

        ConstellationEnum constellation = ConstellationEnum.fromId(measurement.getConstellationType());
        Integer svid = measurement.getSvid();

        BandEnum bandName = getBandName(measurement.getConstellationType(), measurement.getCarrierFrequencyHz());

        // Check Constellation exists in map
        if (!mainMap.containsKey(constellation)) {
            mainMap.put(constellation, new LinkedHashMap<>());
        }

        // Check SVID exists in Constellation
        if (!mainMap.get(constellation).containsKey(svid)) {
            mainMap.get(constellation).put(svid, new LinkedHashMap<>());
        }

        // Check Freq exists in SVID
        if (!mainMap.get(constellation).get(svid).containsKey(bandName)) {
            mainMap.get(constellation).get(svid).put(bandName, new Satellite());
        }

        Satellite nSatellite = mainMap.get(constellation).get(svid).get(bandName);

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
            nSatellite.DeltaRangeString = parseAccumulatedDeltaRangeState(measurement.getAccumulatedDeltaRangeState());
        } else {
            nSatellite.AccumulatedDeltaRangeState = 0;
            nSatellite.AccumulatedDeltaRangeMeters = 0;
            nSatellite.AccumulatedDeltaRangeUncertaintyMeters = 0;
        }

        // True
        if (measurement.hasCarrierFrequencyHz()) {
            nSatellite.CarrierFrequencyHz = measurement.getCarrierFrequencyHz();
        }

        // False
        if ((measurement.getAccumulatedDeltaRangeState() & ADR_STATE_VALID) == ADR_STATE_VALID) {
            nSatellite.AccumulatedDeltaRangeMeters = measurement.getAccumulatedDeltaRangeMeters();
        }

        // False
        if (measurement.hasSnrInDb()) {
            nSatellite.SnrInDb = measurement.getSnrInDb();
        }

        // True
        if (measurement.hasCarrierFrequencyHz()) {
            nSatellite.CarrierFreqHz = measurement.getCarrierFrequencyHz();
        }

        // Additional
        double tRxNanos;
        if (measurement.getTimeOffsetNanos() != 0) {
            tRxNanos = clock.TRxNanos + measurement.getTimeOffsetNanos();
        } else {
            tRxNanos = clock.TRxNanos;
        }

        double tRxSeconds = tRxNanos * 1e-9;
        double tTxSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;

        // GLONASS
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GLONASS) {
            double tRxSeconds_GLO = tRxSeconds % 86400;
            double tTxSeconds_GLO = tTxSeconds - 10800 + LEAP_SECONDS;
            if (tTxSeconds_GLO < 0) {
                tTxSeconds_GLO = tTxSeconds_GLO + 86400;
            }
            tRxSeconds = tRxSeconds_GLO;
            tTxSeconds = tTxSeconds_GLO;
        }

        // BEIDOU
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_BEIDOU) {
            double tRxSeconds_BDS = tRxSeconds;
            double tTxSeconds_BDS = tTxSeconds + LEAP_SECONDS - 4;
            if (tTxSeconds_BDS > 604800) {
                tTxSeconds_BDS = tTxSeconds_BDS - 604800;
            }
            tRxSeconds = tRxSeconds_BDS;
            tTxSeconds = tTxSeconds_BDS;
        }

        double prSeconds = tRxSeconds - tTxSeconds;
        nSatellite.PRTIME_OLD = prSeconds;

        boolean iRollover = prSeconds > 604800d / 2;
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
        // > 12_000km and < 60_000km
        nSatellite.Valid = prSeconds > 0.04 && prSeconds < 0.2;

        double prm = prSeconds * SPEED_LIGHT;

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
        double wavelength = SPEED_LIGHT / measurement.getCarrierFrequencyHz();
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
            nSatellite.PHASE = UNKNOWN_PHASE;
        }
        // End PhaseShift calculation ---------------------------------------------

        // Log features
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) {
            //Log.e("Satellite", "tRxSeconds: "+ tRxNanos+ " getReceivedSvTimeNanos(): " + measurement.getReceivedSvTimeNanos());
            //Log.e("Satellite" , String.format("Difference: %5.3f сек", prSeconds));
            //Log.e("Satellite" , String.format("PseudoRange: %7.0f км", prm / 1000));
        }
    }

    private String parseAccumulatedDeltaRangeState(int state) {
        String sValue = "";
        // ADR_STATE_UNKNOWN = 0
        if (((byte) state) == GnssMeasurement.ADR_STATE_UNKNOWN) {
            sValue += "ADR_STATE_UNKNOWN "; // 0
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_VALID) == GnssMeasurement.ADR_STATE_VALID) {
            sValue += "ADR_STATE_VALID "; // 1
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_RESET) == GnssMeasurement.ADR_STATE_RESET) {
            sValue += "ADR_STATE_RESET "; // 2
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_CYCLE_SLIP) == GnssMeasurement.ADR_STATE_CYCLE_SLIP) {
            sValue += "ADR_STATE_CYCLE_SLIP "; // 4
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_HALF_CYCLE_RESOLVED) == GnssMeasurement.ADR_STATE_HALF_CYCLE_RESOLVED) {
            sValue += "ADR_STATE_HALF_CYCLE_RESOLVED "; // 8
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_HALF_CYCLE_REPORTED) == GnssMeasurement.ADR_STATE_HALF_CYCLE_REPORTED) {
            sValue += "ADR_STATE_HALF_CYCLE_REPORTED "; // 16
        }
        return sValue;
    }


}
