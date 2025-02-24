package com.example.pseudoranges.models;

import static com.example.pseudoranges.parsers.ClockParser.SECONDS_IN_WEEK;
import static com.example.pseudoranges.parsers.ClockParser.WEEKS_IN_YEAR;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.pseudoranges.Clock;
import com.example.pseudoranges.ConstellationEnum;
import com.example.pseudoranges.BandEnum;
import com.example.pseudoranges.Satellite;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainViewModel extends AndroidViewModel {

    // private MutableLiveData<String> tmpString = new MutableLiveData<>();
    /*
        Hashmap
            Key: GPS, SBAS, GLONASS, QZSS, BEIDOU, GALILEO
            Value:
                Hashmap
                    Key: satellite Id
                    Value:
                        Hashmap:
                            key: frequency level (L1 or L2 or L5 for example)
                            Value: Satellite

     */
    private final Map<ConstellationEnum, Map<Integer, Map<BandEnum, Satellite>>> measurement = new LinkedHashMap<>();
    private final Clock clock = new Clock();

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public Clock gc() {
        return clock;
    }

    public Map<ConstellationEnum, Map<Integer, Map<BandEnum, Satellite>>> gm() {
        return measurement;
    }

    public String toStringClockClass() {
        /* Not interesting
            "%-4s = %s\n".formatted("LeapSecond", clock.LeapSecond)
            "%-4s = %s\n".formatted("TimeUncertaintyNanos", clock.TimeUncertaintyNanos)
            "%-4s = %s\n".formatted("FullBiasNanos", clock.FullBiasNanos)
            "%-4s = %s\n".formatted("BiasNanos", clock.BiasNanos)
            "%-4s = %.3f\n".formatted("BiasUncertaintyNanos", clock.BiasUncertaintyNanos)
         */
        // Log.e("Clock", "Full Bias Nano: " + clock.FullBiasNanos + " Time Nanos: " + clock.TimeNanos + " bias Nanos: " + clock.BiasNanos);
        return """
            Time from boot = %.0f
            Years From 1980 = %.0f
            
            HardwareClockDiscontinuityCount = %d
            DriftNanosPerSecond = %.3f
            DriftUncertaintyNanosPerSecond = %.3f
            Age = %d sec
            Received Measurements = %d
            """.formatted(
            clock.BootTimeNanos * 1e-9,
            clock.FullBiasNanos * 1e-9 / SECONDS_IN_WEEK / WEEKS_IN_YEAR,
            clock.HardwareClockDiscontinuityCount,
            clock.DriftNanosPerSecond,
            clock.DriftUncertaintyNanosPerSecond,
            (System.currentTimeMillis() - clock.AgeData) / 1000,
            clock.ReceivedMeasurements
        );
    }

    synchronized public String toStringMeasurementMap(ConstellationEnum filterConstellation, Integer filterTime, boolean checkValidity) {
        return measurement
            .entrySet()
            .stream()
            .filter(constellation -> constellation.getKey().equals(filterConstellation))
            .flatMap(constellation ->
                constellation
                    .getValue()
                    .values()
                    .stream()
                    .map(
                        stringSatelliteMap -> stringSatelliteMap
                            .entrySet()
                            .stream()
                            .filter(satellite -> {
                                if (checkValidity) {
                                    return satellite.getValue().Valid;
                                } else {
                                    return true;
                                }
                            })
                            .map(satellite -> {
                                    long age = (System.currentTimeMillis() - satellite.getValue().AgeData) / 1000;
                                    if (filterTime != 0 && age > filterTime) {
                                        return null;
                                    }
                                    return "%-7s \t\t\t%3d(%s)  %6.0f \t\t\t%s\t\t\t%s".formatted(
                                        /* GPS */  constellation.getKey(),
                                        /* satellite Id*/ satellite.getValue().INDEX,
                                        /*satellite frequency L1 or L5 or L2*/ satellite.getKey().name(),
                                        /*range meters /1000 = km*/ satellite.getValue().PRM / 1000d,
                                        /*kind of phase*/ satellite.getValue().PHASE,
                                        /*age of info in secs*/ age
                                    );
                                }
                            )
                    )
            )
            .flatMap(Function.identity())
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n", "Созвездие\t Спутник Расст(км) фаза возраст\n", ""));
    }

}
