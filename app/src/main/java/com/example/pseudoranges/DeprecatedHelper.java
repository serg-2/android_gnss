package com.example.pseudoranges;

public abstract class DeprecatedHelper {
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

}
