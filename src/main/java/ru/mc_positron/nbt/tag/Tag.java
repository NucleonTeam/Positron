package ru.mc_positron.nbt.tag;

import lombok.NonNull;

public final class Tag<T> {

    private final String key;

    private Tag(String key) {
        this.key = key;
    }

    public @NonNull String getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof Tag<?> tag) {
            return key.equals(tag.key);
        }

        return false;
    }

    public static @NonNull Tag<String> String(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<Long> Long(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<Integer> Integer(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<int[]> IntegerArray(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<Short> Short(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<Byte> Byte(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<byte[]> ByteArray(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<Boolean> Boolean(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<Float> Float(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<Double> Double(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<CompoundTag> Compound(@NonNull String key) {
        return new Tag<>(key);
    }

    public static @NonNull Tag<Void> End() {
        return new Tag<>(null);
    }

    public interface Id {
        byte END = 0;
        byte BYTE = 1;
        byte SHORT = 2;
        byte INT = 3;
        byte LONG = 4;
        byte FLOAT = 5;
        byte DOUBLE = 6;
        byte BYTE_ARRAY = 7;
        byte STRING = 8;
        byte LIST = 9;
        byte COMPOUND = 10;
        byte INT_ARRAY = 11;
    }
}
