package com.example.pseudoranges.parsers;

import android.location.GnssClock;

import com.example.pseudoranges.Clock;
import com.example.pseudoranges.MainActivity;

public class ClockParser {

    private final Clock clock;

    public ClockParser(MainActivity mainActivity) {
        clock = mainActivity.mainViewModel.gc();
    }

    public void parseClock(GnssClock gnssClock, int sizeOfMeasurements) {
        clock.AgeData = System.currentTimeMillis();

        clock.TimeNanos = gnssClock.getTimeNanos();
        clock.HardwareClockDiscontinuityCount = gnssClock.getHardwareClockDiscontinuityCount();

        // True
        if (gnssClock.hasLeapSecond()) {
            clock.LeapSecond = gnssClock.getLeapSecond();
        }

        // True
        if (gnssClock.hasTimeUncertaintyNanos()) {
            clock.TimeUncertaintyNanos = gnssClock.getTimeUncertaintyNanos();
        }

        // True
        if (gnssClock.hasFullBiasNanos()) {
            clock.FullBiasNanos = gnssClock.getFullBiasNanos();
        }

        // True
        if (gnssClock.hasBiasNanos()) {
            clock.BiasNanos = gnssClock.getBiasNanos();
        }

        // True, but no values
        if (gnssClock.hasBiasUncertaintyNanos()) {
            clock.BiasUncertaintyNanos = gnssClock.getBiasUncertaintyNanos();
        }

        // True
        if (gnssClock.hasDriftNanosPerSecond()) {
            clock.DriftNanosPerSecond = gnssClock.getDriftNanosPerSecond();
        }

        // True
        if (gnssClock.hasDriftUncertaintyNanosPerSecond()) {
            clock.DriftUncertaintyNanosPerSecond = gnssClock.getDriftUncertaintyNanosPerSecond();
        }

        // Additional
        double weekNumber = Math.floor(-(gnssClock.getFullBiasNanos() * 1e-9 / 604800));
        double weekNumberNanos = weekNumber * 604800 * 1e9;

        double tRxNanos = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - weekNumberNanos;

        if (gnssClock.hasBiasNanos()) {
            tRxNanos = tRxNanos - gnssClock.getBiasNanos();
        }

        clock.TRxNanos = tRxNanos;

        // 251
        //gnssClock.getHardwareClockDiscontinuityCount();

        clock.ReceivedMeasurements = sizeOfMeasurements;
    }

}
