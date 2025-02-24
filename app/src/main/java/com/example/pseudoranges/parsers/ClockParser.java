package com.example.pseudoranges.parsers;

import android.location.GnssClock;

import com.example.pseudoranges.Clock;
import com.example.pseudoranges.MainActivity;

public class ClockParser {

    private final Clock clock;

    public ClockParser(MainActivity mainActivity) {
        this.clock = mainActivity.mainViewModel.gc();
    }

    public static final int SECONDS_IN_WEEK = 3600 * 24 * 7;
    public static final double WEEKS_IN_YEAR = 52.1429;

    public void parseClock(GnssClock gnssClock) {
        clock.BootTimeNanos = gnssClock.getTimeNanos();
        clock.AgeData = System.currentTimeMillis();

        // MAIN! True, if possible to calculate. Almanacs? ephemeris?
        long nanosSince1980;
        if (gnssClock.hasFullBiasNanos()) {
            nanosSince1980 = gnssClock.getFullBiasNanos();
            clock.EmulatedTime = false;
        } else {
            nanosSince1980 = emulateFullBiasNanos(clock.AgeData, clock.BootTimeNanos);
            clock.EmulatedTime = true;
        }
        clock.FullBiasNanos = nanosSince1980;


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

        double timeReceivedInternalReceiver = gnssClock.getTimeNanos() - (nanosSince1980 + weekNumberNanos);

        if (gnssClock.hasBiasNanos()) {
            timeReceivedInternalReceiver = timeReceivedInternalReceiver - gnssClock.getBiasNanos();
        }

        // Main Received time in ns. Local GNSS receiver internal Time.
        clock.TimeOfReceivedInternalTime = timeReceivedInternalReceiver;

        // 251
        //gnssClock.getHardwareClockDiscontinuityCount();
    }

    private long emulateFullBiasNanos(long millisSystem, long nanosFromBoot) {
        // Millis System
        // UTC midnight, January 1, 1970 UTC

        // Nanos in Full Bias (minus sign)
        // GPS time since 0000Z, January 6, 1980, in nanoseconds.

        // Don't know from where 18 extra seconds.
        long fixMillis = 17359L;

        final long millisBetween = (long) (3650 + 1 + 1 + 5) * 24 * 3600 * 1000 ;

        return -1 * ((millisSystem - millisBetween + fixMillis) * 1000_000 - nanosFromBoot);
    }

}
