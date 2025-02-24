package com.example.pseudoranges;

public class Clock {
    public long BootTimeNanos;
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

    // Additional
    public double TRxNanos;
    public long AgeData;

    // New
    public int ReceivedMeasurements;
}

