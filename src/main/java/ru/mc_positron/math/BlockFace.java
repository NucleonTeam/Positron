package ru.mc_positron.math;

import com.google.common.collect.Iterators;
import lombok.Getter;
import lombok.NonNull;
import org.spongepowered.math.vector.Vector3i;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Predicate;

public enum BlockFace {

    DOWN(0, 1, -1, "down", AxisDirection.NEGATIVE, new Vector3i(0, -1, 0)),
    UP(1, 0, -1, "up", AxisDirection.POSITIVE, new Vector3i(0, 1, 0)),
    NORTH(2, 3, 2, "north", AxisDirection.NEGATIVE, new Vector3i(0, 0, -1)),
    SOUTH(3, 2, 0, "south", AxisDirection.POSITIVE, new Vector3i(0, 0, 1)),
    WEST(4, 5, 1, "west", AxisDirection.NEGATIVE, new Vector3i(-1, 0, 0)),
    EAST(5, 4, 3, "east", AxisDirection.POSITIVE, new Vector3i(1, 0, 0));

    private static final BlockFace[] VALUES = new BlockFace[6];
    private static final BlockFace[] HORIZONTALS = new BlockFace[4];

    static {
        DOWN.axis = Axis.Y;
        UP.axis = Axis.Y;
        NORTH.axis = Axis.Z;
        SOUTH.axis = Axis.Z;
        WEST.axis = Axis.X;
        EAST.axis = Axis.X;

        for (BlockFace face : values()) {
            VALUES[face.index] = face;

            if (face.getAxis().isHorizontal()) {
                HORIZONTALS[face.horizontalIndex] = face;
            }
        }
    }

    @Getter private final int index;
    private final int opposite;
    @Getter private final int horizontalIndex;
    @Getter private final String name;
    @Getter private Axis axis;
    @Getter private final AxisDirection axisDirection;
    private final Vector3i unitVector;

    BlockFace(int index, int opposite, int horizontalIndex, String name, AxisDirection axisDirection, Vector3i unitVector) {
        this.index = index;
        this.opposite = opposite;
        this.horizontalIndex = horizontalIndex;
        this.name = name;
        this.axisDirection = axisDirection;
        this.unitVector = unitVector;
    }

    public static @NonNull BlockFace fromIndex(int index) {
        return VALUES[FastMath.abs(index % VALUES.length)];
    }

    public static @NonNull BlockFace fromHorizontalIndex(int index) {
        return HORIZONTALS[FastMath.abs(index % HORIZONTALS.length)];
    }

    public static @NonNull BlockFace random(@NonNull Random rand) {
        return VALUES[rand.nextInt(VALUES.length)];
    }

    public int getHorizontalIndex() {
        return horizontalIndex;
    }

    public @NonNull Vector3i getUnitVector() {
        return unitVector;
    }

    public int getXOffset() {
        return axis == Axis.X ? axisDirection.getOffset() : 0;
    }

    public int getYOffset() {
        return axis == Axis.Y ? axisDirection.getOffset() : 0;
    }

    public int getZOffset() {
        return axis == Axis.Z ? axisDirection.getOffset() : 0;
    }

    public @NonNull BlockFace getOpposite() {
        return fromIndex(opposite);
    }

    public @NonNull BlockFace rotateY() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> throw new RuntimeException("Unable to get Y-rotated face of " + this);
        };
    }

    public @NonNull BlockFace rotateYCCW() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
            default -> throw new RuntimeException("Unable to get counter-clockwise Y-rotated face of " + this);
        };
    }

    public @NonNull Vector3i moveFrom(@NonNull Vector3i vec) {
        return vec.add(unitVector);
    }

    public @NonNull Vector3i moveFrom(@NonNull Vector3i vec, int steps) {
        return vec.add(unitVector.mul(steps));
    }

    public static @NonNull Vector3i moveUp(@NonNull Vector3i vec, int steps) {
        return vec.add(BlockFace.UP.unitVector.mul(steps));
    }

    public static @NonNull Vector3i moveUp(@NonNull Vector3i vec) {
        return vec.add(BlockFace.UP.unitVector);
    }

    public static @NonNull Vector3i moveDown(@NonNull Vector3i vec, int steps) {
        return vec.add(BlockFace.DOWN.unitVector.mul(steps));
    }

    public static @NonNull Vector3i moveDown(@NonNull Vector3i vec) {
        return vec.add(BlockFace.DOWN.unitVector);
    }

    public static @NonNull Vector3i moveNorth(@NonNull Vector3i vec, int steps) {
        return vec.add(BlockFace.NORTH.unitVector.mul(steps));
    }

    public static @NonNull Vector3i moveNorth(@NonNull Vector3i vec) {
        return vec.add(BlockFace.NORTH.unitVector);
    }

    public static @NonNull Vector3i moveSouth(@NonNull Vector3i vec, int steps) {
        return vec.add(BlockFace.SOUTH.unitVector.mul(steps));
    }

    public static @NonNull Vector3i moveSouth(@NonNull Vector3i vec) {
        return vec.add(BlockFace.SOUTH.unitVector);
    }

    public static @NonNull Vector3i moveWest(@NonNull Vector3i vec, int steps) {
        return vec.add(BlockFace.WEST.unitVector.mul(steps));
    }

    public static @NonNull Vector3i moveWest(@NonNull Vector3i vec) {
        return vec.add(BlockFace.WEST.unitVector);
    }

    public static @NonNull Vector3i moveEast(@NonNull Vector3i vec, int steps) {
        return vec.add(BlockFace.EAST.unitVector.mul(steps));
    }

    public static @NonNull Vector3i moveEast(@NonNull Vector3i vec) {
        return vec.add(BlockFace.EAST.unitVector);
    }

    public enum Axis implements Predicate<BlockFace> {
        X("x"),
        Y("y"),
        Z("z");

        static {
            X.plane = Plane.HORIZONTAL;
            Y.plane = Plane.VERTICAL;
            Z.plane = Plane.HORIZONTAL;
        }

        @Getter private final String name;
        private Plane plane;

        Axis(String name) {
            this.name = name;
        }

        public @NonNull Plane getPlane() {
            return plane;
        }

        public boolean isVertical() {
            return plane == Plane.VERTICAL;
        }

        public boolean isHorizontal() {
            return plane == Plane.HORIZONTAL;
        }

        public boolean test(BlockFace face) {
            return face != null && face.getAxis() == this;
        }
    }

    @Getter
    public enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int offset;
        private final String description;

        AxisDirection(int offset, String description) {
            this.offset = offset;
            this.description = description;
        }
    }

    public enum Plane implements Predicate<BlockFace>, Iterable<BlockFace> {
        HORIZONTAL,
        VERTICAL;

        static {
            HORIZONTAL.faces = new BlockFace[]{NORTH, EAST, SOUTH, WEST};
            VERTICAL.faces = new BlockFace[]{UP, DOWN};
        }

        private BlockFace[] faces;

        public @NonNull BlockFace random(@NonNull Random rand) {
            return faces[rand.nextInt(faces.length)];
        }

        public boolean test(BlockFace face) {
            return face != null && face.getAxis().getPlane() == this;
        }

        public @NonNull Iterator<BlockFace> iterator() {
            return Iterators.forArray(faces);
        }
    }
}
