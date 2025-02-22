package com.example.pseudoranges;

import static com.example.pseudoranges.BandEnum.*;

import android.location.GnssStatus;
import android.util.Log;

public abstract class BandHelper {

    private static final String TAG = "BandHelper";

    public static BandEnum getBandName(int constellationType, float carrierFrequencyHz) {
        // Frequency in kHz
        int frequencyRounded = Math.round(carrierFrequencyHz / 100) * 100;
        switch (constellationType) {
            case GnssStatus.CONSTELLATION_GPS:
                return switch (frequencyRounded) {
                    case 1_575_420_000 -> L1;
                    case 1_227_600_000 -> L2;
                    case 1_176_450_000 -> L5;
                    default -> {
                        Log.e(TAG, "UNKNOWN GPS Frequency!" + frequencyRounded);
                        yield UNKNOWN_GPS;
                    }
                };
            case GnssStatus.CONSTELLATION_SBAS:
                return switch (frequencyRounded) {
                    case 1_575_420_000 -> L1;
                    case 1_176_450_000 -> L5;
                    default -> {
                        Log.e(TAG, "UNKNOWN SBAS Frequency!" + frequencyRounded);
                        yield UNKNOWN_SBAS;
                    }
                };
            case GnssStatus.CONSTELLATION_GLONASS:
                if (frequencyRounded >= 1_598_062_500 && frequencyRounded <= 1_605_375_000) {
                    return L1;
                } else if (frequencyRounded >= 1_242_937_500 && frequencyRounded <= 1_248_625_000) {
                    return L2;
                } else {
                    Log.e(TAG, "UNKNOWN GLONASS Frequency!" + frequencyRounded);
                    return UNKNOWN_GLONASS;
                }
            case GnssStatus.CONSTELLATION_QZSS:
                return switch (frequencyRounded) {
                    case 1_575_420_000 -> L1;
                    case 1_227_600_000 -> L2;
                    case 1_176_450_000 -> L5;
                    case 1_278_750_000 -> L6;
                    default -> {
                        Log.e(TAG, "UNKNOWN QZSS Frequency!" + frequencyRounded);
                        yield UNKNOWN_QZSS;
                    }
                };
            case GnssStatus.CONSTELLATION_BEIDOU:
                return switch (frequencyRounded) {
                    case 1_561_098_000, 1_575_420_000 -> L1;
                    case 1_207_140_000, 1_176_450_000 -> L2;
                    case 1_268_520_000 -> L3;
                    default -> {
                        Log.e(TAG, "UNKNOWN BeiDou Frequency!" + frequencyRounded);
                        yield UNKNOWN_BEIDOU;
                    }
                };
            case GnssStatus.CONSTELLATION_GALILEO:
                return switch (frequencyRounded) {
                    case 1_575_420_000 -> L1;
                    case 1_176_450_000 -> L5a;
                    case 1_207_140_000 -> L5b;
                    case 1_278_750_000 -> L6;
                    default -> {
                        Log.e(TAG, "UNKNOWN Galileo Frequency!" + frequencyRounded);
                        yield UNKNOWN_GALILEO;
                    }
                };
            default:
                return UNKNOWN;
        }
    }
}
