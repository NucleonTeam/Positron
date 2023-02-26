package ru.mc_positron.math;

import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector2f;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;

public final class LinearInterpolation {

    private LinearInterpolation() {

    }

    public static Vector3d interpolate(Vector3d from, Vector3d to, double t) {
        return from.add(to.sub(from).mul(t));
    }

    public static Vector3f interpolate(Vector3f from, Vector3f to, double t) {
        return from.add(to.sub(from).mul(t));
    }

    public static Vector2d interpolate(Vector2d from, Vector2d to, double t) {
        return from.add(to.sub(from).mul(t));
    }

    public static Vector2f interpolate(Vector2f from, Vector2f to, double t) {
        return from.add(to.sub(from).mul(t));
    }

    public static double interpolate(double from, double to, double t) {
        return from + (to - from) * t;
    }

    public static float interpolate(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
