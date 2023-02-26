package ru.mc_positron.math.noise;

import lombok.NonNull;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;

import java.util.Random;


public class SimplexNoise extends Noise {

    protected final static double SQRT_3 = Math.sqrt(3);
    protected final static double F2 = 0.5 * (SQRT_3 - 1);
    protected final static double G2 = (3 - SQRT_3) / 6;
    protected final static double G22 = G2 * 2.0 - 1;
    protected final static double F3 = 1.0 / 3.0;
    protected final static double G3 = 1.0 / 6.0;

    public static final int[][] grad3 = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
    };
    protected final Random random;
    protected final double offsetW;

    public SimplexNoise(@NonNull Random random, double octaves, double persistence, double expansion) {
        super(octaves, persistence, expansion);

        this.random = random;

        offset = new Vector3d(
                random.nextFloat() * 256,
                random.nextFloat() * 256,
                random.nextFloat() * 256
        );
        offsetW = random.nextFloat() * 256;

        perm = new int[512];
        for (int i = 0; i < 256; ++i) {
            perm[i] = random.nextInt(256) - 127;
        }
        for (int i = 0; i < 256; ++i) {
            int pos = random.nextInt(255) - 127 + i;
            int old = perm[i];
            perm[i] = perm[pos];
            perm[pos] = old;
            perm[i + 256] = perm[i];
        }
    }

    @Override
    public double originalNoise3D(@NonNull Vector3d vec) {
        vec = vec.add(offset);

        // Skew the input space to determine which simplex cell we're in
        double s = (vec.x() + vec.y() + vec.z()) * F3;
        int i = (int) (vec.x() + s);
        int j = (int) (vec.y() + s);
        int k = (int) (vec.z() + s);
        double t = (i + j + k) * G3;

        // Unskew the cell origin back to (x,y,z) space
        var vec0 = vec.sub(i + t, j + t, k + t);

        // For the 3D case, the simplex shape is a slightly irregular tetrahedron.
        int i1, j1, k1, i2, j2, k2;

        // Determine which simplex we are in.
        if (vec0.x() >= vec0.y()) {
            if (vec0.y() >= vec0.z()) {
                i1 = 1; j1 = 0; k1 = 0;
                i2 = 1; j2 = 1; k2 = 0;
            } else if (vec0.x() >= vec0.z()) {
                i1 = 1; j1 = 0; k1 = 0;
                i2 = 1; j2 = 0; k2 = 1;
            } else {
                i1 = 0; j1 = 0; k1 = 1;
                i2 = 1; j2 = 0; k2 = 1;
            }
        } else {
            if (vec0.y() < vec0.z()) {
                i1 = 0; j1 = 0; k1 = 1;
                i2 = 0; j2 = 1; k2 = 1;
            } else if (vec0.x() < vec0.z()) {
                i1 = 0; j1 = 1; k1 = 0;
                i2 = 0; j2 = 1; k2 = 1;
            } else {
                i1 = 0; j1 = 1; k1 = 0;
                i2 = 1; j2 = 1; k2 = 0;
            }
        }

        // A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in (x,y,z),
        // a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in (x,y,z), and
        // a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in (x,y,z), where
        // c = 1/6.
        var vec1 = vec0.sub(i1 - G3, j1 - G3, k1 - G3);
        var vec2 = vec0.sub(i2 - 2.0 * G3, j2 - 2.0 * G3, k2 - 2.0 * G3);
        var vec3 = vec0.sub(1.0 - 3.0 * G3, 1.0 - 3.0 * G3, 1.0 - 3.0 * G3);

        // Work out the hashed gradient indices of the four simplex corners
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;

        double n = 0;

        // Calculate the contribution from the four corners
        double t0 = 0.6 - vec0.x() * vec0.x() - vec0.y() * vec0.y() - vec0.z() * vec0.z();
        if (t0 > 0) {
            int[] gi0 = grad3[this.perm[ii + this.perm[jj + this.perm[kk]]] % 12];
            n += t0 * t0 * t0 * t0 * (gi0[0] * vec0.x() + gi0[1] * vec0.y() + gi0[2] * vec0.z());
        }

        double t1 = 0.6 - vec1.x() * vec1.x() - vec1.y() * vec1.y() - vec1.z() * vec1.z();
        if (t1 > 0) {
            int[] gi1 = grad3[this.perm[ii + i1 + this.perm[jj + j1 + this.perm[kk + k1]]] % 12];
            n += t1 * t1 * t1 * t1 * (gi1[0] * vec1.x() + gi1[1] * vec1.y() + gi1[2] * vec1.z());
        }

        double t2 = 0.6 - vec2.x() * vec2.x() - vec2.y() * vec2.y() - vec2.z() * vec2.z();
        if (t2 > 0) {
            int[] gi2 = grad3[this.perm[ii + i2 + this.perm[jj + j2 + this.perm[kk + k2]]] % 12];
            n += t2 * t2 * t2 * t2 * (gi2[0] * vec2.x() + gi2[1] * vec2.y() + gi2[2] * vec2.z());
        }

        double t3 = 0.6 - vec3.x() * vec3.x() - vec3.y() * vec3.y() - vec3.z() * vec3.z();
        if (t3 > 0) {
            int[] gi3 = grad3[this.perm[ii + 1 + this.perm[jj + 1 + this.perm[kk + 1]]] % 12];
            n += t3 * t3 * t3 * t3 * (gi3[0] * vec3.x() + gi3[1] * vec3.y() + gi3[2] * vec3.z());
        }

        // Add contributions from each corner to get the noise value.
        // The result is scaled to stay just inside [-1,1]
        return 32.0 * n;
    }

    @Override
    public double originalNoise2D(@NonNull Vector2d vec) {
        vec = vec.add(offset.toVector2());

        // Skew the input space to determine which simplex cell we're in
        double s = (vec.x() + vec.y()) * F2; // Hairy factor for 2D
        int i = (int) (vec.x() + s);
        int j = (int) (vec.y() + s);
        double t = (i + j) * G2;

        // Unskew the cell origin back to (x,y) space
        var vec0 = vec.sub(i + t, j + t);

        // For the 2D case, the simplex shape is an equilateral triangle.
        int i1, j1;

        // Determine which simplex we are in.
        if (vec0.x() > vec0.y()) {
            i1 = 1; j1 = 0;
        } else {
            i1 = 0; j1 = 1;
        }
        // upper triangle, YX order: (0,0).(0,1).(1,1)

        // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
        // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
        // c = (3-sqrt(3))/6
        var vec1 = vec0.sub(i1 - G2, j1 - G2);
        var vec2 = vec0.add(G22, G22);

        // Work out the hashed gradient indices of the three simplex corners
        int ii = i & 255;
        int jj = j & 255;

        double n = 0;

        // Calculate the contribution from the three corners
        double t0 = 0.5 - vec0.x() * vec0.x() - vec0.y() * vec0.y();
        if (t0 > 0) {
            int[] gi0 = grad3[this.perm[ii + this.perm[jj]] % 12];
            n += t0 * t0 * t0 * t0 * (gi0[0] * vec0.x() + gi0[1] * vec0.y()); // (x,y) of grad3 used for 2D gradient
        }

        double t1 = 0.5 - vec1.x() * vec1.x() - vec1.y() * vec1.y();
        if (t1 > 0) {
            int[] gi1 = grad3[this.perm[ii + i1 + this.perm[jj + j1]] % 12];
            n += t1 * t1 * t1 * t1 * (gi1[0] * vec1.x() + gi1[1] * vec1.y());
        }

        double t2 = 0.5 - vec2.x() * vec2.x() - vec2.y() * vec2.y();
        if (t2 > 0) {
            int[] gi2 = grad3[this.perm[ii + 1 + this.perm[jj + 1]] % 12];
            n += t2 * t2 * t2 * t2 * (gi2[0] * vec2.x() + gi2[1] * vec2.y());
        }

        // Add contributions from each corner to get the noise value.
        // The result is scaled to return values in the interval [-1,1].
        return 70.0 * n;
    }
}
