package com.example.pseudoranges;

import static com.example.pseudoranges.ConstellationEnum.*;

public abstract class NMEAHelper {

    public static String parseNmea(String s) {
        // GP- GPS, GL - GLONAS, GA - galileo, GB(BD) - Beidou, GQ - qznss, GN - different

        // GGA - содержит данные о последнем местоположении.
        // GSV - Строка с идентификатором GSV содержит информацию о всех наблюдаемых спутниках.
        // DTM - Datum reference information
        // GNS - GNSS fix data
        // VTG - Строка с идентификатором VTG содержит скорость и курс относительно земли.
        // RMC - Строка с идентификатором RMC содержит рекомендуемый минимум навигационных данных.
        // GSA - DOP and active satellites
        String prefix = s.substring(1,6);
        ConstellationEnum constellation = switch (prefix.substring(0,2)) {
            case "GN" -> UNIVERSAL;
            case "GP" -> GPS;
            case "GL" -> GLONASS;
            case "GA" ->GALILEO;
            case "GB", "BD" -> BEIDOU;
            case "GQ" -> QZSS;
            default -> UNKNOWN;
        };
        if (constellation.equals(UNKNOWN)) {
            return "UNKNOWN Constellation: " + s;
        }
        return switch (prefix.substring(2, 5)) {
            case "GGA" -> "";
            case "GSV" -> "";
            case "DTM" -> "";
            case "GNS" -> "";
            case "VTG" -> "";
            case "RMC" -> "";
            case "GSA" -> "";
            default -> "UNKNOWN message: " + s;
        };
    }

}
