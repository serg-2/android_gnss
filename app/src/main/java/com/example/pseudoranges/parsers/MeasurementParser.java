package com.example.pseudoranges.parsers;

import static android.location.GnssMeasurement.*;
import static android.location.GnssMeasurement.ADR_STATE_VALID;
import static android.location.GnssMeasurement.STATE_BDS_D2_BIT_SYNC;
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

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MeasurementParser {

    private final Map<ConstellationEnum, Map<Integer, Map<BandEnum, Satellite>>> mainMap;
    private final Clock clock;
    private final MainViewModel viewModel;

    private long someTime = 0;

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
        if (measurement.getAccumulatedDeltaRangeState() != ADR_STATE_UNKNOWN) {
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

        // Main received Time of signal
        double receivedNanos;
        if (measurement.getTimeOffsetNanos() != 0) {
            receivedNanos = clock.TimeOfReceivedInternalTime + measurement.getTimeOffsetNanos();
        } else {
            receivedNanos = clock.TimeOfReceivedInternalTime;
        }

        // MAIN
        double receivedSecondsFloat = receivedNanos * 1e-9;

        // The received satellite time is relative to the beginning of the system week
        // for all constellations
        // except for Glonass where it is relative to the beginning of the Glonass system day.
        double timeFromSignalSecondsFloat = measurement.getReceivedSvTimeNanos() * 1e-9;

        // GLONASS fixes
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GLONASS) {
            double tRxSeconds_GLO = receivedSecondsFloat % 86400;
            double tTxSeconds_GLO = timeFromSignalSecondsFloat - 10800 + LEAP_SECONDS;
            if (tTxSeconds_GLO < 0) {
                tTxSeconds_GLO = tTxSeconds_GLO + 86400;
            }
            receivedSecondsFloat = tRxSeconds_GLO;
            timeFromSignalSecondsFloat = tTxSeconds_GLO;
        }

        // BEIDOU fixes
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_BEIDOU) {
            double tRxSeconds_BDS = receivedSecondsFloat;
            double tTxSeconds_BDS = timeFromSignalSecondsFloat + LEAP_SECONDS - 4;
            if (tTxSeconds_BDS > 604800) {
                tTxSeconds_BDS = tTxSeconds_BDS - 604800;
            }
            receivedSecondsFloat = tRxSeconds_BDS;
            timeFromSignalSecondsFloat = tTxSeconds_BDS;
        }

        double prSeconds = receivedSecondsFloat - timeFromSignalSecondsFloat;
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
                receivedSecondsFloat = receivedSecondsFloat - delS;
                prSeconds = receivedSecondsFloat - timeFromSignalSecondsFloat;
                iRollover = false;
            }
        }
        nSatellite.PRTIME_NEW = prSeconds;

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

        boolean fixedState = (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GLONASS) &&
            checkReceivedTimeState(measurement.getState(), STATE_GLO_TOD_KNOWN) ||
            (measurement.getConstellationType() != GnssStatus.CONSTELLATION_GLONASS) &&
                checkReceivedTimeState(measurement.getState(), STATE_TOW_KNOWN);

        // Old valid
        // Check Valid here
        // Valid or not? > 50 ms
        // nSatellite.Valid = timeFromSignalSecondsFloat > 0.05;
        nSatellite.Valid = fixedState;
        
        if (fixedState) {
            return;
        }

        // Timings
        // https://developer.android.com/reference/android/location/GnssMeasurement#getReceivedSvTimeNanos()

        // Some log
        if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS &&
           // (nSatellite.INDEX == 22 || nSatellite.INDEX == 23)&&
            nSatellite.BandName.equals(BandEnum.L1)) {

            // STATE_CODE_LOCK
            if ((nSatellite.State) == 1) {
                if (measurement.getReceivedSvTimeNanos() * 1e-6 > 0.005 && measurement.getReceivedSvTimeNanos() * 1e-6 < 1) {
                    someTime = measurement.getReceivedSvTimeNanos();
                    return;
                }
            }
            //     1                 2              32
            // STATE_CODE_LOCK STATE_BIT_SYNC STATE_SYMBOL_SYNC
            if ((nSatellite.State) == 35) {
                if (measurement.getReceivedSvTimeNanos() * 1e-6 > 8 && measurement.getReceivedSvTimeNanos() * 1e-6 < 20) {
                    return;
                }
            }
            //      1             2                   4                   8                 16384
            // STATE_CODE_LOCK STATE_BIT_SYNC STATE_SUBFRAME_SYNC STATE_TOW_DECODED STATE_TOW_KNOWN ~ 156369349.537308
            if ((nSatellite.State) == 16399) {
                if (measurement.getReceivedSvTimeNanos() * 1e-9 > .1) {
                    return;
                }
            }
            Log.e("Satellite", "State : " + parseReceivedTimeState(nSatellite.State));
            Log.e("Satellite", "GetReceivedSvTimeSecs(): " + measurement.getReceivedSvTimeNanos() * 1e-9);
            Log.e("Satellite", String.format("Difference: %5.3f сек", prSeconds));
            Log.e("Satellite", String.format("PseudoRange: %7.0f км\n", prm / 1000));
        }
    }

    private boolean checkReceivedTimeState(int state, int toCheck) {
        return ((state & toCheck) == toCheck);
    }

    private String parseReceivedTimeState(int state) {
        String sValue = "";
        if (state == STATE_UNKNOWN) {
            sValue += "STATE_UNKNOWN "; // 0
        }
        if ((state & STATE_CODE_LOCK) == STATE_CODE_LOCK) {
            sValue += "STATE_CODE_LOCK "; // 1
        }
        if ((state & STATE_BIT_SYNC) == STATE_BIT_SYNC) {
            sValue += "STATE_BIT_SYNC "; // 2
        }
        if ((state & STATE_SUBFRAME_SYNC) == STATE_SUBFRAME_SYNC) {
            sValue += "STATE_SUBFRAME_SYNC "; // 4
        }
        if ((state & STATE_TOW_DECODED) == STATE_TOW_DECODED) {
            sValue += "STATE_TOW_DECODED "; // 8
        }
        if ((state & STATE_MSEC_AMBIGUOUS) == STATE_MSEC_AMBIGUOUS) {
            sValue += "STATE_MSEC_AMBIGUOUS "; // 16
        }
        if ((state & STATE_SYMBOL_SYNC) == STATE_SYMBOL_SYNC) {
            sValue += "STATE_SYMBOL_SYNC "; // 32
        }
        if ((state & STATE_GLO_STRING_SYNC) == STATE_GLO_STRING_SYNC) {
            sValue += "STATE_GLO_STRING_SYNC "; // 64
        }
        if ((state & STATE_GLO_TOD_DECODED) == STATE_GLO_TOD_DECODED) {
            sValue += "STATE_GLO_TOD_DECODED "; // 128
        }
        if ((state & STATE_BDS_D2_BIT_SYNC) == STATE_BDS_D2_BIT_SYNC) {
            sValue += "STATE_BDS_D2_BIT_SYNC "; // 256
        }
        if ((state & STATE_BDS_D2_SUBFRAME_SYNC) == STATE_BDS_D2_SUBFRAME_SYNC) {
            sValue += "STATE_BDS_D2_SUBFRAME_SYNC "; // 512
        }
        if ((state & STATE_GAL_E1BC_CODE_LOCK) == STATE_GAL_E1BC_CODE_LOCK) {
            sValue += "STATE_GAL_E1BC_CODE_LOCK "; // 1024
        }
        if ((state & STATE_GAL_E1C_2ND_CODE_LOCK) == STATE_GAL_E1C_2ND_CODE_LOCK) {
            sValue += "STATE_GAL_E1C_2ND_CODE_LOCK "; // 2048
        }
        if ((state & STATE_GAL_E1B_PAGE_SYNC) == STATE_GAL_E1B_PAGE_SYNC) {
            sValue += "STATE_GAL_E1B_PAGE_SYNC "; // 4096
        }
        if ((state & STATE_SBAS_SYNC) == STATE_SBAS_SYNC) {
            sValue += "STATE_SBAS_SYNC "; // 8192
        }
        if ((state & STATE_TOW_KNOWN) == STATE_TOW_KNOWN) {
            sValue += "STATE_TOW_KNOWN "; // 16384
        }
        if ((state & STATE_GLO_TOD_KNOWN) == STATE_GLO_TOD_KNOWN) {
            sValue += "STATE_GLO_TOD_KNOWN "; //  32768
        }
        if ((state & STATE_2ND_CODE_LOCK) == STATE_2ND_CODE_LOCK) {
            sValue += "STATE_2ND_CODE_LOCK "; //  65536
        }
        return sValue;
    }

    private String parseAccumulatedDeltaRangeState(int state) {
        String sValue = "";
        // ADR_STATE_UNKNOWN = 0
        if (((byte) state) == ADR_STATE_UNKNOWN) {
            sValue += "ADR_STATE_UNKNOWN "; // 0
        }
        if (((byte) state & ADR_STATE_VALID) == ADR_STATE_VALID) {
            sValue += "ADR_STATE_VALID "; // 1
        }
        if (((byte) state & ADR_STATE_RESET) == ADR_STATE_RESET) {
            sValue += "ADR_STATE_RESET "; // 2
        }
        if (((byte) state & ADR_STATE_CYCLE_SLIP) == ADR_STATE_CYCLE_SLIP) {
            sValue += "ADR_STATE_CYCLE_SLIP "; // 4
        }
        if (((byte) state & ADR_STATE_HALF_CYCLE_RESOLVED) == ADR_STATE_HALF_CYCLE_RESOLVED) {
            sValue += "ADR_STATE_HALF_CYCLE_RESOLVED "; // 8
        }
        if (((byte) state & ADR_STATE_HALF_CYCLE_REPORTED) == ADR_STATE_HALF_CYCLE_REPORTED) {
            sValue += "ADR_STATE_HALF_CYCLE_REPORTED "; // 16
        }
        return sValue;
    }


}
