package com.example.pseudoranges.parsers;

import android.location.GnssClock;
import android.util.Log;

import com.example.pseudoranges.Clock;
import com.example.pseudoranges.MainActivity;

public class ClockParser {

    private final Clock clock;

    public ClockParser(MainActivity mainActivity) {
        clock = mainActivity.mainViewModel.gc();
    }

    public static final int SECONDS_IN_WEEK = 3600 * 24 * 7;
    public static final double WEEKS_IN_YEAR = 52.1429;

    public void parseClock(GnssClock gnssClock, int sizeOfMeasurements) {
        // True MAIN!
        long nanosSince1980;
        if (gnssClock.hasFullBiasNanos()) {
            nanosSince1980 = gnssClock.getFullBiasNanos();
            clock.FullBiasNanos = nanosSince1980;
        } else {
            clock.FullBiasNanos = gnssClock.getFullBiasNanos();
            return;
        }

        clock.AgeData = System.currentTimeMillis();
        clock.BootTimeNanos = gnssClock.getTimeNanos();

        clock.HardwareClockDiscontinuityCount = gnssClock.getHardwareClockDiscontinuityCount();

        // True
        if (gnssClock.hasLeapSecond()) {
            clock.LeapSecond = gnssClock.getLeapSecond();
        }

        // True
        if (gnssClock.hasTimeUncertaintyNanos()) {
            clock.TimeUncertaintyNanos = gnssClock.getTimeUncertaintyNanos();
        }

        // True. 0?
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
        double numberOfWeeks = Math.floor(-(nanosSince1980 * 1e-9 / SECONDS_IN_WEEK));
        double weekNumberNanos = numberOfWeeks * SECONDS_IN_WEEK * 1e9;

        double tRxNanos = gnssClock.getTimeNanos() - (nanosSince1980 + weekNumberNanos);

        if (gnssClock.hasBiasNanos()) {
            tRxNanos = tRxNanos - gnssClock.getBiasNanos();
        }

        clock.TRxNanos = tRxNanos;

        // 251
        //gnssClock.getHardwareClockDiscontinuityCount();

        clock.ReceivedMeasurements = sizeOfMeasurements;
    }

}
