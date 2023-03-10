package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;
import ru.mc_positron.nbt.NbtMap;

import java.io.IOException;

public abstract class Tag<T, V> {

    private final String key;
    private final int id;

    Tag(String key, int id) {
        this.key = key;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public @NonNull String getKey() {
        return key;
    }

    public abstract @NonNull V read(@NonNull NBTInputStream stream) throws IOException;

    public abstract void write(@NonNull NBTOutputStream stream, @NonNull V value) throws IOException;

    void writeList(@NonNull NBTOutputStream stream, @NonNull NbtMap map) throws IOException {
        var list = map.getList(this);
        stream.writeInt(list.size());
        for (var value: list) write(stream, value);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof Tag tag) {
            return key.equals(tag.key);
        }

        return false;
    }

    public static @NonNull StringTag String(@NonNull String key) {
        return new StringTag(key);
    }

    public static @NonNull LongTag Long(@NonNull String key) {
        return new LongTag(key);
    }

    public static @NonNull IntegerTag Integer(@NonNull String key) {
        return new IntegerTag(key);
    }

    public static @NonNull IntegerArrayTag IntegerArray(@NonNull String key) {
        return new IntegerArrayTag(key);
    }

    public static @NonNull ShortTag Short(@NonNull String key) {
        return new ShortTag(key);
    }

    public static @NonNull ByteTag Byte(@NonNull String key) {
        return new ByteTag(key);
    }

    public static @NonNull ByteArrayTag ByteArray(@NonNull String key) {
        return new ByteArrayTag(key);
    }

    public static @NonNull FloatTag Float(@NonNull String key) {
        return new FloatTag(key);
    }

    public static @NonNull DoubleTag Double(@NonNull String key) {
        return new DoubleTag(key);
    }

    public static @NonNull CompoundTag Compound(@NonNull String key) {
        return new CompoundTag(key);
    }

    public static @NonNull BooleanTag Boolean(@NonNull String key) {
        return new BooleanTag(key);
    }

    static @NonNull Tag<?, ?> pickTag(int id, @NonNull String key) {
        return switch (id) {
            case Tag.Id.BYTE -> Tag.Byte(key);
            case Tag.Id.SHORT -> Tag.Short(key);
            case Tag.Id.INT -> Tag.Integer(key);
            case Tag.Id.LONG -> Tag.Long(key);
            case Tag.Id.FLOAT -> Tag.Float(key);
            case Tag.Id.DOUBLE -> Tag.Double(key);
            case Tag.Id.STRING -> Tag.String(key);
            case Tag.Id.BYTE_ARRAY -> Tag.ByteArray(key);
            case Tag.Id.COMPOUND -> Tag.Compound(key);
            case Tag.Id.INT_ARRAY -> Tag.IntegerArray(key);
            default -> throw new IllegalArgumentException();
        };
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
