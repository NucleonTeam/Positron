package ru.mc_positron.math;

import lombok.NonNull;
import org.spongepowered.math.vector.Vector3d;

public final class Point {

    private final Vector3d position;
    private final double yaw, pitch, headYaw;

    private Point(@NonNull Vector3d position, double yaw, double pitch, double headYaw) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;
    }

    public @NonNull Vector3d getPosition() {
        return position;
    }

    public double getYaw() {
        return yaw;
    }

    public double getHeadYaw() {
        return headYaw;
    }

    public double getPitch() {
        return pitch;
    }

    public static @NonNull Point of(@NonNull Vector3d position, double yaw, double pitch, double headYaw) {
        return new Point(position, yaw, pitch, headYaw);
    }
}
