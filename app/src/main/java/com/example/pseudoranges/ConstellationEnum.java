package com.example.pseudoranges;

public enum ConstellationEnum {
    GPS(1),
    SBAS(2),
    GLONASS(3),
    QZSS(4),
    BEIDOU(5),
    GALILEO(6),
    UNKNOWN(0),
    UNIVERSAL(15);

    private final int value;

    ConstellationEnum(int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

    public static ConstellationEnum fromId(int id) {
        for(ConstellationEnum e : values()) {
            if(e.getValue() == id) return e;
        }
        return UNKNOWN;
    }
}
