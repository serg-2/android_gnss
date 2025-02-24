package com.example.pseudoranges;

public class Clock {
    // Nanoseconds from boot
    public long BootTimeNanos;
    // Millis from last success
    public long AgeData;
    // Time was emulated? Or from gnss receiver
    public boolean EmulatedTime = false;

    public int HardwareClockDiscontinuityCount;

    // Optional hasLeapSecond
    public int LeapSecond;

    // Optional hasTimeUncertaintyNanos
    public double TimeUncertaintyNanos;

    // Optional hasFullBiasNanos
    public long FullBiasNanos;

    // Optional hasBiasNanos
    public double BiasNanos;

    // Optional hasBiasUncertaintyNanos
    public double BiasUncertaintyNanos;

    // Optional hasDriftNanosPerSecond
    public double DriftNanosPerSecond;

    // Optional hasDriftUncertaintyNanosPerSecond
    public double DriftUncertaintyNanosPerSecond;

    // time Of received By receiver
    public double TimeOfReceivedInternalTime;

}

