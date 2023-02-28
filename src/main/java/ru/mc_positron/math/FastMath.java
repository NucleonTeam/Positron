package ru.mc_positron.math;

public final class FastMath {

    private static final float[] a = new float[65536];

    static {
        for (int i = 0; i < 65536; i++)
            a[i] = (float) Math.sin(i * 3.141592653589793D * 2.0D / 65536.0D);
    }

    private FastMath() {

    }

    public static float sqrt(float value) {
        return (float) Math.sqrt(value);
    }

    public static float sin(float value) {
        return a[((int) (value * 10430.378F) & 0xFFFF)];
    }

    public static float cos(float value) {
        return a[((int) (value * 10430.378F + 16384.0F) & 0xFFFF)];
    }

    public static int floor(double value) {
        int i = (int) value;

        return (value < (double) i) ? (i - 1) : i;
    }

    public static int log2(int value) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(value);
    }

    public static double max(double first, double second, double third, double fourth) {
        if (first > second && first > third && first > fourth) return first;
        if (second > third && second > fourth) return second;
        return Math.max(third, fourth);
    }

    public static int ceil(float floatNumber) {
        int truncated = (int) floatNumber;
        return floatNumber > truncated ? truncated + 1 : truncated;
    }

    public static int floorDouble(double value) {
        int i = (int) value;
        return value >= i ? i : i - 1;
    }

    public static int ceilDouble(double value) {
        int i = (int) (value + 1);
        return value >= i ? i : i - 1;
    }

    public static int floorFloat(float value) {
        int i = (int) value;
        return value >= i ? i : i - 1;
    }

    public static int ceilFloat(float value) {
        int i = (int) (value + 1);
        return value >= i ? i : i - 1;
    }

    public static int abs(int number) {
        return number > 0? number : -number;
    }

    public static double round(double d) {
        return round(d, 0);
    }

    public static double round(double d, int precision) {
        return ((double) Math.round(d * Math.pow(10, precision))) / Math.pow(10, precision);
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : (Math.min(value, max));
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : (Math.min(value, max));
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : (Math.min(value, max));
    }

    public static double getDirection(double diffX, double diffZ) {
        diffX = Math.abs(diffX);
        diffZ = Math.abs(diffZ);

        return Math.max(diffX, diffZ);
    }
}
