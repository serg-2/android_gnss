package com.example.pseudoranges.models;

import android.app.Application;

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

    public MainViewModel(@NonNull Application application) {
        super(application);

    }

    public Clock gc() {
        return clock;
    }

    public Map<String, Map<Integer, Map<String, Satellite>>> gm() {
        return measurement;
    }

    public String toStringClockClass() {
        final String format = "%-4s = %s\n";
        StringBuilder builder = new StringBuilder();
        DecimalFormat numberFormat = new DecimalFormat("#0.000");

        builder.append(String.format(format, "TimeNanos", clock.TimeNanos));

        builder.append(
                String.format(
                        format,
                        "HardwareClockDiscontinuityCount",
                        clock.HardwareClockDiscontinuityCount));

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
        builder.append(
                String.format(
                        format,
                        "DriftNanosPerSecond",
                        numberFormat.format(clock.DriftNanosPerSecond)));

        builder.append(
                String.format(
                        format,
                        "DriftUncertaintyNanosPerSecond",
                        numberFormat.format(clock.DriftUncertaintyNanosPerSecond)));

        //builder.append("\n");

        builder.append(String.format(Locale.ENGLISH,
                "%s = %d\n",
                "Age",
                (System.currentTimeMillis() - clock.AgeData) / 1000));

        // Log.e("Clock", "Full Bias Nano: " + clock.FullBiasNanos + " Time Nanos: " + clock.TimeNanos + " bias Nanos: " + clock.BiasNanos);
        return builder.toString();
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
                    if (age > filterTime) {
                        continue;
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
