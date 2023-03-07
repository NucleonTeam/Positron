package cn.nukkit.math;

import lombok.NonNull;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector3i;
import ru.mc_positron.math.BlockFace;

public class Vector3 implements Cloneable {

    public double x;
    public double y;
    public double z;

    public Vector3() {
        this(0, 0, 0);
    }

    public Vector3(double x) {
        this(x, 0, 0);
    }

    public Vector3(double x, double y) {
        this(x, y, 0);
    }

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3(@NonNull Vector3d vec) {
        this(vec.x(), vec.y(), vec.z());
    }

    public @NonNull Vector3d toNewVector() {
        return new Vector3d(x, y, z);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public Vector3 setX(double x) {
        this.x = x;
        return this;
    }

    public Vector3 setY(double y) {
        this.y = y;
        return this;
    }

    public Vector3 setZ(double z) {
        this.z = z;
        return this;
    }


    public int getFloorX() {
        return (int) Math.floor(this.x);
    }

    public int getFloorY() {
        return (int) Math.floor(this.y);
    }

    public int getFloorZ() {
        return (int) Math.floor(this.z);
    }

    public int getChunkX() {
        return getFloorX() >> 4;
    }

    public int getChunkZ() {
        return getFloorZ() >> 4;
    }

    public double getUp() {
        return this.y;
    }

    public Vector3 add(double x) {
        return this.add(x, 0, 0);
    }

    public Vector3 add(double x, double y) {
        return this.add(x, y, 0);
    }

    public Vector3 add(double x, double y, double z) {
        return new Vector3(this.x + x, this.y + y, this.z + z);
    }

    public Vector3 add(Vector3 x) {
        return new Vector3(this.x + x.getX(), this.y + x.getY(), this.z + x.getZ());
    }

    public Vector3 subtract(double x, double y, double z) {
        return this.add(-x, -y, -z);
    }

    public Vector3 divide(double number) {
        return new Vector3(this.x / number, this.y / number, this.z / number);
    }

    public Vector3 floor() {
        return new Vector3(this.getFloorX(), this.getFloorY(), this.getFloorZ());
    }

    public Vector3 round() {
        return new Vector3(Math.round(this.x), Math.round(this.y), Math.round(this.z));
    }

    public Vector3 abs() {
        return new Vector3((int) Math.abs(this.x), (int) Math.abs(this.y), (int) Math.abs(this.z));
    }

    public Vector3 getSide(BlockFace face) {
        return this.getSide(face, 1);
    }

    public Vector3 getSide(BlockFace face, int step) {
        return new Vector3(this.getX() + face.getXOffset() * step, this.getY() + face.getYOffset() * step, this.getZ() + face.getZOffset() * step);
    }

    // Get as a Vector3 for better performance. Do not override in Block!
    public Vector3 getSideVec(BlockFace face) {
        return new Vector3(this.getX() + face.getXOffset(), this.getY() + face.getYOffset(), this.getZ() + face.getZOffset());
    }

    public Vector3 up() {
        return up(1);
    }

    public Vector3 up(int step) {
        return getSide(BlockFace.UP, step);
    }

    public Vector3 down() {
        return down(1);
    }

    public Vector3 down(int step) {
        return getSide(BlockFace.DOWN, step);
    }

    public Vector3 north() {
        return north(1);
    }

    public Vector3 north(int step) {
        return getSide(BlockFace.NORTH, step);
    }

    public Vector3 south() {
        return south(1);
    }

    public Vector3 south(int step) {
        return getSide(BlockFace.SOUTH, step);
    }

    public Vector3 east() {
        return east(1);
    }

    public Vector3 east(int step) {
        return getSide(BlockFace.EAST, step);
    }

    public Vector3 west() {
        return west(1);
    }

    public Vector3 west(int step) {
        return getSide(BlockFace.WEST, step);
    }

    public double distanceSquared(Vector3 pos) {
        return Math.pow(this.x - pos.x, 2) + Math.pow(this.y - pos.y, 2) + Math.pow(this.z - pos.z, 2);
    }

    public double length() {
        return Math.sqrt(this.lengthSquared());
    }

    public double lengthSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public Vector3 normalize() {
        double len = this.lengthSquared();
        if (len > 0) {
            return this.divide(Math.sqrt(len));
        }
        return new Vector3(0, 0, 0);
    }

    /**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     *
     * @param v vector
     * @param x x value
     * @return intermediate vector
     */
    public Vector3 getIntermediateWithXValue(Vector3 v, double x) {
        double xDiff = v.x - this.x;
        double yDiff = v.y - this.y;
        double zDiff = v.z - this.z;
        if (xDiff * xDiff < 0.0000001) {
            return null;
        }
        double f = (x - this.x) / xDiff;
        if (f < 0 || f > 1) {
            return null;
        } else {
            return new Vector3(this.x + xDiff * f, this.y + yDiff * f, this.z + zDiff * f);
        }
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     *
     * @param v vector
     * @param y y value
     * @return intermediate vector
     */
    public Vector3 getIntermediateWithYValue(Vector3 v, double y) {
        double xDiff = v.x - this.x;
        double yDiff = v.y - this.y;
        double zDiff = v.z - this.z;
        if (yDiff * yDiff < 0.0000001) {
            return null;
        }
        double f = (y - this.y) / yDiff;
        if (f < 0 || f > 1) {
            return null;
        } else {
            return new Vector3(this.x + xDiff * f, this.y + yDiff * f, this.z + zDiff * f);
        }
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     *
     * @param v vector
     * @param z z value
     * @return intermediate vector
     */
    public Vector3 getIntermediateWithZValue(Vector3 v, double z) {
        double xDiff = v.x - this.x;
        double yDiff = v.y - this.y;
        double zDiff = v.z - this.z;
        if (zDiff * zDiff < 0.0000001) {
            return null;
        }
        double f = (z - this.z) / zDiff;
        if (f < 0 || f > 1) {
            return null;
        } else {
            return new Vector3(this.x + xDiff * f, this.y + yDiff * f, this.z + zDiff * f);
        }
    }

    public Vector3 setComponents(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public String toString() {
        return "Vector3(x=" + this.x + ",y=" + this.y + ",z=" + this.z + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector3)) {
            return false;
        }

        Vector3 other = (Vector3) obj;

        return this.x == other.x && this.y == other.y && this.z == other.z;
    }

    @Override
    public int hashCode() {
        return ((int) x ^ ((int) z << 12)) ^ ((int) y << 24);
    }

    @Override
    public Vector3 clone() {
        try {
            return (Vector3) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public Vector3i asBlockVector3() {
        return new Vector3i(this.getFloorX(), this.getFloorY(), this.getFloorZ());
    }
}
