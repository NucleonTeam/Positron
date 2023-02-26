package cn.nukkit.math;

import java.util.Locale;

import static java.lang.Math.PI;

public final class Angle {

    public static Angle fromRadian(double doubleRadian) {
        return new Angle(doubleRadian, false);
    }

    public float asFloatRadian() {
        if (isOriginDouble) {
            if (isDegree) return (float) (doubleValue * PI / 180.0);
            else return (float) doubleValue;
        } else {
            if (isDegree) return floatValue * (float) PI / 180.0f;
            else return floatValue;
    }
    }

    public double asDoubleRadian() {
        if (isOriginDouble) {
            if (isDegree) return doubleValue * PI / 180.0;
            else return doubleValue;
        } else {
            if (isDegree) return floatValue * PI / 180.0;
            else return floatValue;
    }
    }

    public float asFloatDegree() {
        if (isOriginDouble) {
            if (isDegree) return (float) doubleValue;
            else return (float) (doubleValue * 180.0 / PI);
        } else {
            if (isDegree) return floatValue;
            else return floatValue * 180.0f / (float) PI;
    }
    }

    public double asDoubleDegree() {
        if (isOriginDouble) {
            if (isDegree) return doubleValue;
            else return doubleValue * 180.0 / PI;
        } else {
            if (isDegree) return floatValue;
            else return floatValue * 180.0 / PI;
    }
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "Angle[%s, %f%s = %f%s] [%d]",
                isOriginDouble ? "Double" : "Float",
                isOriginDouble ? doubleValue : floatValue,
                isDegree ? "deg" : "rad",
                isDegree ? (isOriginDouble ? asDoubleRadian() : asFloatRadian()) :
                        (isOriginDouble ? asDoubleDegree() : asFloatDegree()),
                isDegree ? "rad" : "deg",
                hashCode()
        );
    }

    @Override
    public int hashCode() {
        int hash;
        if (isOriginDouble) hash = Double.hashCode(doubleValue);
        else hash = Float.hashCode(floatValue);
        if (isDegree) hash = hash ^ 0xABCD1234;
        return hash;
    }

    private final float floatValue;
    private final double doubleValue;
    private final boolean isDegree, isOriginDouble;

    private Angle(double doubleValue, boolean isDegree) {
        this.isOriginDouble = true;
        this.floatValue = 0.0f;
        this.doubleValue = doubleValue;
        this.isDegree = isDegree;
    }

}
