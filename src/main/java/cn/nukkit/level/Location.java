package cn.nukkit.level;

import cn.nukkit.math.Vector3;
import cn.nukkit.utils.LevelException;

public class Location extends Position {

    public double yaw;
    public double pitch;
    public double headYaw;

    public Location(double x, double y, double z, double yaw, double pitch, Level level) {
        this(x, y, z, yaw, pitch, 0, level);
    }

    public Location(double x, double y, double z, double yaw, double pitch, double headYaw, Level level) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;
        this.level = level;
    }

    public double getYaw() {
        return this.yaw;
    }

    public double getPitch() {
        return this.pitch;
    }

    public double getHeadYaw() {
        return this.headYaw;
    }

    @Override
    public String toString() {
        return "Location (level=" + (this.isValid() ? this.getLevel().getName() : "null") + ", x=" + this.x + ", y=" + this.y + ", z=" + this.z + ", yaw=" + this.yaw + ", pitch=" + this.pitch + ", headYaw=" + this.headYaw + ")";
    }

    @Override
    public Location getLocation() {
        if (this.isValid()) return new Location(this.x, this.y, this.z, this.yaw, this.pitch, this.headYaw, this.level);
        else throw new LevelException("Undefined Level reference");
    }

    @Override
    public Location add(double x) {
        return this.add(x, 0, 0);
    }

    @Override
    public Location add(double x, double y) {
        return this.add(x, y, 0);
    }

    @Override
    public Location add(double x, double y, double z) {
        return new Location(this.x + x, this.y + y, this.z + z, this.yaw, this.pitch, this.headYaw, this.level);
    }

    @Override
    public Location add(Vector3 x) {
        return new Location(this.x + x.getX(), this.y + x.getY(), this.z + x.getZ(), this.yaw, this.pitch, this.headYaw, this.level);
    }

    @Override
    public Location subtract(double x, double y, double z) {
        return this.add(-x, -y, -z);
    }

    @Override
    public Location divide(double number) {
        return new Location(this.x / number, this.y / number, this.z / number, this.yaw, this.pitch, this.headYaw, this.level);
    }

    @Override
    public Location floor() {
        return new Location(this.getFloorX(), this.getFloorY(), this.getFloorZ(), this.yaw, this.pitch, this.headYaw, this.level);
    }

    @Override
    public Location round() {
        return new Location(Math.round(this.x), Math.round(this.y), Math.round(this.z), this.yaw, this.pitch, this.headYaw, this.level);
    }

    @Override
    public Location abs() {
        return new Location((int) Math.abs(this.x), (int) Math.abs(this.y), (int) Math.abs(this.z), this.yaw, this.pitch, this.headYaw, this.level);
    }

    @Override
    public Location clone() {
        return (Location) super.clone();
    }
}
