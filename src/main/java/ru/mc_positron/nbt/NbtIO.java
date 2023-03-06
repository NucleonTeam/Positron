package ru.mc_positron.nbt;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import cn.nukkit.nbt.stream.PGZIPOutputStream;
import cn.nukkit.utils.ThreadCache;
import lombok.NonNull;
import ru.mc_positron.nbt.tag.Tag;

import java.io.*;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

public final class NbtIO {

    private NbtIO() {

    }

    public static @NonNull NbtMap readBytes(byte[] data) throws IOException {
        return readBytes(data, ByteOrder.BIG_ENDIAN);
    }

    public static @NonNull NbtMap readBytes(byte[] data, @NonNull ByteOrder endianness) throws IOException {
        return read(new ByteArrayInputStream(data), endianness);
    }

    public static @NonNull NbtMap readBytes(byte[] data, @NonNull ByteOrder endianness, boolean network) throws IOException {
        return read(new ByteArrayInputStream(data), endianness, network);
    }

    public static @NonNull NbtMap readFile(@NonNull File file) throws IOException {
        return readFile(file, ByteOrder.BIG_ENDIAN);
    }

    public static @NonNull NbtMap readFile(@NonNull File file, @NonNull ByteOrder endianness) throws IOException {
        if (!file.exists()) throw new IllegalArgumentException("File " + file + " not fount");

        return read(new FileInputStream(file), endianness);
    }

    public static @NonNull NbtMap readCompressed(@NonNull InputStream inputStream) throws IOException {
        return readCompressed(inputStream, ByteOrder.BIG_ENDIAN);
    }

    public static @NonNull NbtMap readCompressed(@NonNull InputStream inputStream, @NonNull ByteOrder endianness) throws IOException {
        return read(new BufferedInputStream(new GZIPInputStream(inputStream)), endianness);
    }

    public static @NonNull NbtMap read(@NonNull InputStream inputStream) throws IOException {
        return read(inputStream, ByteOrder.BIG_ENDIAN);
    }

    public static @NonNull NbtMap read(@NonNull InputStream inputStream, @NonNull ByteOrder endianness) throws IOException {
        return read(inputStream, endianness, false);
    }

    public static @NonNull NbtMap read(@NonNull InputStream inputStream, @NonNull ByteOrder endianness, boolean network) throws IOException {
        try (var stream = new NBTInputStream(inputStream, endianness, network)) {
            var type = stream.readByte();

            if (type != Tag.Id.COMPOUND) throw new IOException("Root tag must be a named compound tag");

            return Tag.Compound(stream.readUTF()).read(stream);
        }
    }

    public static byte[] write(@NonNull NbtMap map) throws IOException {
        return write(map, ByteOrder.BIG_ENDIAN);
    }

    public static byte[] write(@NonNull NbtMap map, @NonNull ByteOrder endianness) throws IOException {
        return write(map, endianness, false);
    }

    public static byte[] write(@NonNull NbtMap map, @NonNull ByteOrder endianness, boolean network) throws IOException {
        var outputStream = ThreadCache.fbaos.get().reset();
        try (var stream = new NBTOutputStream(outputStream, endianness, network)) {
            stream.writeByte(Tag.Id.COMPOUND);
            stream.writeUTF("");

            Tag.Compound("").write(stream, map);
            return outputStream.toByteArray();
        }
    }

    public static void writeFile(@NonNull NbtMap map, @NonNull File file) throws IOException {
        writeFile(map, file, ByteOrder.BIG_ENDIAN);
    }

    public static void writeFile(@NonNull NbtMap map, @NonNull File file, @NonNull ByteOrder endianness) throws IOException {
        writeStream(map, new FileOutputStream(file), endianness);
    }

    public static void writeStream(@NonNull NbtMap map, @NonNull OutputStream outputStream) throws IOException {
        writeStream(map, outputStream, ByteOrder.BIG_ENDIAN);
    }

    public static void writeStream(@NonNull NbtMap map, @NonNull OutputStream outputStream, @NonNull ByteOrder endianness) throws IOException {
        writeStream(map, outputStream, endianness, false);
    }

    public static void writeStream(@NonNull NbtMap map, @NonNull OutputStream outputStream, @NonNull ByteOrder endianness, boolean network) throws IOException {
        try (var stream = new NBTOutputStream(outputStream, endianness, network)) {
            stream.write(write(map, endianness, network));
        }
    }

    public static byte[] writeNetwork(@NonNull NbtMap map) throws IOException {
        var outputStream = ThreadCache.fbaos.get().reset();
        try (var stream = new NBTOutputStream(outputStream, ByteOrder.LITTLE_ENDIAN, true)) {
            stream.write(write(map));
        }
        return outputStream.toByteArray();
    }

    public static void writeGZIPCompressed(@NonNull NbtMap map, @NonNull OutputStream outputStream) throws IOException {
        writeGZIPCompressed(map, outputStream, ByteOrder.BIG_ENDIAN);
    }

    public static void writeGZIPCompressed(@NonNull NbtMap map, @NonNull OutputStream outputStream, @NonNull ByteOrder endianness) throws IOException {
        writeStream(map, new PGZIPOutputStream(outputStream), endianness);
    }
}
