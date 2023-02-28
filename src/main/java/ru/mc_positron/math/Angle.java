package ru.mc_positron.math;

import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;

public final class Angle {

    private Angle() {

    }

    public static double radToDeg(double rad){
        return rad / Math.PI * 180;
    }

    public static double degToRad(double deg) {
        return deg / 180 * Math.PI;
    }

    public double angleBetween(Vector3d v1, Vector3d v2) {
        return Math.acos(Math.min(Math.max(v1.normalize().dot(v2.normalize()), -1.0d), 1.0d));
    }

    public double angleBetween(Vector3f v1, Vector3f v2) {
        return Math.acos(Math.min(Math.max(v1.normalize().dot(v2.normalize()), -1.0f), 1.0f));
    }
}
