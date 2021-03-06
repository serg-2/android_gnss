package com.example.pseudoranges;

import static com.example.pseudoranges.MyListener.unknown_phase;

public class Satellite {

    public double TimeOffsetNanos;
    public int State;
    public long ReceivedSvTimeNanos;
    public long ReceivedSvTimeUncertaintyNanos;
    public double Cn0DbHz;
    public double PseudorangeRateMetersPerSecond;
    public double PseudorangeRateUncertaintyMetersPerSeconds;
    public int MultipathIndicator;

    // Optional AccumulatedDeltaRangeState DEFINED
    public int AccumulatedDeltaRangeState;
    public double AccumulatedDeltaRangeMeters;
    public double AccumulatedDeltaRangeUncertaintyMeters;

    // Optional hasCarrierFrequencyHz
    public float CarrierFrequencyHz;

    // Optional hasCarrierCycles
    public long CarrierCycles;

    // Optional hasCarrierPhase
    public double CarrierPhase;

    // Optional hasCarrierPhaseUncertainty
    public double CarrierPhaseUncertainty;

    // Optional hasSnrInDb
    public double SnrInDb;

    // Optional hasAutomaticGainControlLevelDb
    public double AgcDb;

    // Optional hasCarrierFrequencyHz
    public float CarrierFreqHz;

    // Additional
    public double PRTIME_OLD;
    public double PRTIME_NEW;
    public double PRM;
    public int INDEX;
    public String BandName;

    // Phase
    public double PhaseShift;
    public String PHASE = unknown_phase;

    public long AgeData;
    public boolean Valid;
}
