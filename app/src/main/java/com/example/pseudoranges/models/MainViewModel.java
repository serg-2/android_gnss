package com.example.pseudoranges.models;

import android.app.Application;
import android.location.GnssMeasurement;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.pseudoranges.Clock;
import com.example.pseudoranges.Satellite;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainViewModel extends AndroidViewModel {

    // private MutableLiveData<String> tmpString = new MutableLiveData<>();
    private Map<String, Map<Integer, Map<String, Satellite>>> measurement = new LinkedHashMap<>();
    private Clock clock = new Clock();
    private int measurementState = 0;

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public Clock gc() {
        return clock;
    }

    public int getState() {
        return measurementState;
    }

    public void setState(int newState) {
        measurementState = newState;
    }

    public Map<String, Map<Integer, Map<String, Satellite>>> gm() {
        return measurement;
    }

    public String toStringClockClass() {
        final String format = "%-4s = %s\n";
        DecimalFormat numberFormat = new DecimalFormat("#0.000");

        String builder = String.format(format, "TimeNanos", clock.TimeNanos) +
                String.format(
                        format,
                        "HardwareClockDiscontinuityCount",
                        clock.HardwareClockDiscontinuityCount) +

        /* Not interesting
        builder.append(String.format(format, "LeapSecond", clock.LeapSecond));

        builder.append(
                String.format(format, "TimeUncertaintyNanos", clock.TimeUncertaintyNanos));

        builder.append(String.format(format, "FullBiasNanos", clock.FullBiasNanos));

        builder.append(String.format(format, "BiasNanos", clock.BiasNanos));

        builder.append(
                String.format(
                        format,
                        "BiasUncertaintyNanos",
                        numberFormat.format(clock.BiasUncertaintyNanos)));
         */
                String.format(
                        format,
                        "DriftNanosPerSecond",
                        numberFormat.format(clock.DriftNanosPerSecond)) +
                String.format(
                        format,
                        "DriftUncertaintyNanosPerSecond",
                        numberFormat.format(clock.DriftUncertaintyNanosPerSecond)) +

                //builder.append("\n");

                String.format(Locale.ENGLISH,
                        "%s = %d\n",
                        "Age",
                        (System.currentTimeMillis() - clock.AgeData) / 1000) +

                // Log.e("Clock", "Full Bias Nano: " + clock.FullBiasNanos + " Time Nanos: " + clock.TimeNanos + " bias Nanos: " + clock.BiasNanos);

                // Add getAccumulatedDeltaRangeState STATE
                String.format(Locale.ENGLISH,
                        "%s = %s\n",
                        "ADR State ",
                        parseAccumulatedDeltaRangeState(getState()));

        return builder;
    }

    private String parseAccumulatedDeltaRangeState(int state) {
        String sValue = "";
        // ADR_STATE_UNKNOWN = 0
        if (((byte) state) == GnssMeasurement.ADR_STATE_UNKNOWN) {
            sValue += "ADR_STATE_UNKNOWN "; // 0
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_VALID) == GnssMeasurement.ADR_STATE_VALID) {
            sValue += "ADR_STATE_VALID "; // 1
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_RESET) == GnssMeasurement.ADR_STATE_RESET) {
            sValue += "ADR_STATE_RESET "; // 2
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_CYCLE_SLIP) == GnssMeasurement.ADR_STATE_CYCLE_SLIP) {
            sValue += "ADR_STATE_CYCLE_SLIP "; // 4
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_HALF_CYCLE_RESOLVED) == GnssMeasurement.ADR_STATE_HALF_CYCLE_RESOLVED) {
            sValue += "ADR_STATE_HALF_CYCLE_RESOLVED "; // 8
        }
        if (((byte) state & GnssMeasurement.ADR_STATE_HALF_CYCLE_REPORTED) == GnssMeasurement.ADR_STATE_HALF_CYCLE_REPORTED) {
            sValue += "ADR_STATE_HALF_CYCLE_REPORTED "; // 16
        }
        return sValue;
    }

    public String toStringMeasurementMap(String filterConstellation, Integer filterTime) {
        StringBuilder builder = new StringBuilder("\n");
        builder.append("Созвездие\t Спутник Расст(км) фаза возраст\n");
        for (Map.Entry<String, Map<Integer, Map<String, Satellite>>> constellationEntry : measurement.entrySet()) {
            if (!Objects.equals(filterConstellation, constellationEntry.getKey()) || Objects.equals(constellationEntry.getKey(), "UNKNOWN")) {
                continue;
            }
            for (Map.Entry<Integer, Map<String, Satellite>> freqEntry : constellationEntry.getValue().entrySet()) {
                for (Map.Entry<String, Satellite> satelliteEntry : freqEntry.getValue().entrySet()) {
                    // Skip bad values
                    if (!satelliteEntry.getValue().Valid) {
                        continue;
                    }
                    // Age Seconds
                    long age = (System.currentTimeMillis() - satelliteEntry.getValue().AgeData) / 1000;
                    // Skip old values
                    if (filterTime != 0) {
                        if (age > filterTime) {
                            continue;
                        }
                    }
                    builder.append(String.format(
                            Locale.ENGLISH,
                            "%-7s \t\t\t%3d(%s)  %6.0f \t\t\t%s\t\t\t%s\n",
                            //constellationEntry.getKey(), satelliteEntry.getValue().INDEX, satelliteEntry.getValue().PRM, satelliteEntry.getValue().CarrierPhase));
                            constellationEntry.getKey(), satelliteEntry.getValue().INDEX, satelliteEntry.getKey(), satelliteEntry.getValue().PRM / 1000, satelliteEntry.getValue().PHASE, age)
                    );
                }
            }
        }
        return builder.toString();
    }

}
