package ru.mc_positron.math.noise;


import lombok.NonNull;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;

public abstract class Noise {

    protected int[] perm;
    protected Vector3d offset = Vector3d.ZERO;
    protected double octaves = 8;
    protected double persistence;
    protected double expansion;

    public Noise(double octaves, double persistence, double expansion) {
        this.octaves = octaves;
        this.persistence = persistence;
        this.expansion = expansion;
    }

    public static double grad(int hash, double x, double y, double z) {
        hash &= 15;
        double u = hash < 8 ? x : y;
        double v = hash < 4 ? y : ((hash == 12 || hash == 14) ? x :
                z);

        return ((hash & 1) == 0 ? u : -u) + ((hash & 2) == 0 ? v : -v);
    }

    public abstract double originalNoise2D(@NonNull Vector2d vec);

    public abstract double originalNoise3D(@NonNull Vector3d vec);

    public double noise2D(@NonNull Vector2d vec) {
        return noise2D(vec, false);
    }

    public double noise2D(@NonNull Vector2d vec, boolean normalized) {
        double result = 0;
        double amp = 1;
        double freq = 1;
        double max = 0;

        vec = vec.mul(expansion);

        for (int i = 0; i < octaves; ++i) {
            result += originalNoise2D(vec.mul(freq)) * amp;
            max += amp;
            freq *= 2;
            amp *= persistence;
        }

        if (normalized) {
            result /= max;
        }

        return result;
    }

    public double noise3D(@NonNull Vector3d vec) {
        return noise3D(vec, false);
    }

    public double noise3D(@NonNull Vector3d vec, boolean normalized) {
        double result = 0;
        double amp = 1;
        double freq = 1;
        double max = 0;

        vec = vec.mul(expansion);

        for (int i = 0; i < octaves; ++i) {
            result += originalNoise3D(vec.mul(freq)) * amp;
            max += amp;
            freq *= 2;
            amp *= persistence;
        }

        if (normalized) {
            result /= max;
        }

        return result;
    }

    public void setOffset(@NonNull Vector3d newOffset) {
        offset = newOffset;
    }
}
