package com.example.pseudoranges;

import static com.example.pseudoranges.Consts.UNKNOWN_PHASE;

import androidx.annotation.NonNull;

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

    // Optional hasSnrInDb
    public double SnrInDb;

    // Optional hasCarrierFrequencyHz
    public float CarrierFreqHz;

    // Additional
    public double PRTIME_OLD;
    public double PRTIME_NEW;
    public double PRM;
    public int INDEX;
    public BandEnum BandName;

    // Phase
    public double PhaseShift;
    public String PHASE = UNKNOWN_PHASE;

    public long AgeData;
    public boolean Valid;

    // Some New
    public String DeltaRangeString;

    @NonNull
    @Override
    public String toString() {
        return "Index: %s Valid: %b PRM: %f".formatted(
            BandName,
            Valid,
            PRM
        );
    }
}
