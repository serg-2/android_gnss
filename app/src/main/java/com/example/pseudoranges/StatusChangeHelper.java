package com.example.pseudoranges;

import android.location.GnssStatus;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class StatusChangeHelper {

    // Almanac vs Ephemeris
    // https://hitechniques.ie/blog/understanding-gnss-operations-almanac-ephemeris-and-receiver-start-modes/
    // https://en.wikipedia.org/wiki/GPS_signals
    // Almanac - valid for up to two weeks.
    // Ephemeris - valid for four hours

    public static String gnssStatusToString(GnssStatus gnssStatus) {
        return IntStream.range(0, gnssStatus.getSatelliteCount())
            .mapToObj(i ->
                "Constellation = %s, Svid = %d, Cn0DbHz = %f, Elevation = %f, Azimuth = %f, hasEphemeris = %b, hasAlmanac = %b, usedInFix = %b".formatted(
                    ConstellationEnum.fromId(gnssStatus.getConstellationType(i)).name(),
                    gnssStatus.getSvid(i),
                    gnssStatus.getCn0DbHz(i),
                    gnssStatus.getElevationDegrees(i),
                    gnssStatus.getAzimuthDegrees(i),
                    gnssStatus.hasEphemerisData(i),
                    gnssStatus.hasAlmanacData(i),
                    gnssStatus.usedInFix(i)
                )

            )
            .collect(Collectors.joining("\n", "SATELLITE_STATUS | [Satellites:\n", "]"));
    }

}
