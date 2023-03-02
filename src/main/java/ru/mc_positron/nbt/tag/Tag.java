package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import lombok.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class Tag<T> {

    private final String key;
    private final Function<NBTInputStream, T> reader;

    private Tag(String key, Function<NBTInputStream, T> reader) {
        this.key = key;
        this.reader = reader;
    }

    public @NonNull String getKey() {
        return key;
    }

    @NonNull T read(@NonNull NBTInputStream stream) {
        return reader.apply(stream);
    }

    @NonNull List<T> readList(@NonNull NBTInputStream stream, int len) {
        var list = new ArrayList<T>();
        for (int i = 0; i < len; i++) {
            list.add(read(stream));
        }
        return list;
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
        return new Tag<>(key, stream -> {
            try {
                return stream.readUTF();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<Long> Long(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                return stream.readLong();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<Integer> Integer(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                return stream.readInt();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<int[]> IntegerArray(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                var data = new int[stream.readInt()];
                for (int i = 0; i < data.length; i++) data[i] = stream.readInt();
                return data;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<Short> Short(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                return stream.readShort();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<Byte> Byte(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                return stream.readByte();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<byte[]> ByteArray(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                var data = new byte[stream.readInt()];
                stream.readFully(data);
                return data;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<Boolean> Boolean(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                return stream.readBoolean();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<Float> Float(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                return stream.readFloat();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<Double> Double(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                return stream.readDouble();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull Tag<CompoundTag> Compound(@NonNull String key) {
        return new Tag<>(key, stream -> {
            try {
                var tag = new CompoundTag();
                Nbt.loadCompound(stream, tag);
                return tag;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static @NonNull Tag<?> pickTag(int id, @NonNull String key) {
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
