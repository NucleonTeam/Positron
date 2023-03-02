package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import lombok.NonNull;

import java.io.*;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

public final class Nbt {

    private Nbt() {

    }

    public static @NonNull CompoundTag readBytes(byte[] data) throws IOException {
        return readBytes(data, ByteOrder.BIG_ENDIAN);
    }

    public static @NonNull CompoundTag readBytes(byte[] data, @NonNull ByteOrder endianness) throws IOException {
        return read(new ByteArrayInputStream(data), endianness);
    }

    public static @NonNull CompoundTag readBytes(byte[] data, @NonNull ByteOrder endianness, boolean network) throws IOException {
        return read(new ByteArrayInputStream(data), endianness, network);
    }

    public static @NonNull CompoundTag readFile(@NonNull File file) throws IOException {
        return readFile(file, ByteOrder.BIG_ENDIAN);
    }

    public static @NonNull CompoundTag readFile(@NonNull File file, @NonNull ByteOrder endianness) throws IOException {
        if (!file.exists()) throw new IllegalArgumentException("File " + file + " not fount");

        return read(new FileInputStream(file), endianness);
    }

    public static @NonNull CompoundTag readCompressed(@NonNull InputStream inputStream) throws IOException {
        return readCompressed(inputStream, ByteOrder.BIG_ENDIAN);
    }

    public static @NonNull CompoundTag readCompressed(@NonNull InputStream inputStream, @NonNull ByteOrder endianness) throws IOException {
        return read(new BufferedInputStream(new GZIPInputStream(inputStream)), endianness);
    }

    public static @NonNull CompoundTag read(@NonNull InputStream inputStream) throws IOException {
        return read(inputStream, ByteOrder.BIG_ENDIAN);
    }

    public static @NonNull CompoundTag read(@NonNull InputStream inputStream, @NonNull ByteOrder endianness) throws IOException {
        return read(inputStream, endianness, false);
    }

    public static @NonNull CompoundTag read(@NonNull InputStream inputStream, @NonNull ByteOrder endianness, boolean network) throws IOException {
        try (var stream = new NBTInputStream(inputStream, endianness, network)) {
            var type = stream.readByte();

            if (type != Tag.Id.COMPOUND) throw new IOException("Root tag must be a named compound tag");

            var root = new CompoundTag();
            stream.readUTF(); // skipping root key because it will empty
            loadCompound(stream, root);

            return root;
        }
    }

    static void loadCompound(@NonNull NBTInputStream stream, @NonNull CompoundTag parent) throws IOException {
        while (true) {
            var type = stream.readByte();

            if (type == Tag.Id.END) return;
            loadTag(stream, parent, type);
        }
    }

    private static void loadTag(@NonNull NBTInputStream stream, @NonNull CompoundTag parent, int id) throws IOException {
        var key = stream.readUTF();
        Tag<?> tag;

        if (id == Tag.Id.LIST) {
            var type = stream.readByte();
            var len = stream.readInt();
            tag = Tag.pickTag(type, key);

            parent.initList(tag, tag.readList(stream, len));
            return;
        }

        tag = Tag.pickTag(id, key);
        parent.init(tag, tag.read(stream));
    }
}
