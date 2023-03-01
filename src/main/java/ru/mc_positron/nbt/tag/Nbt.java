package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public final class Nbt {

    private Nbt() {

    }

    public static @NonNull CompoundTag read(@NonNull InputStream inputStream, @NonNull ByteOrder endianness, boolean network) throws IOException {
        try (var stream = new NBTInputStream(inputStream, endianness, network)) {
            var type = stream.readByte();

            if (type != Tag.Id.COMPOUND) throw new IOException("Root tag must be a named compound tag");

            var root = new CompoundTag();
            stream.readUTF(); // skipping root key because it will empty
            load(stream, root);

            return root;
        }
    }

    static void load(@NonNull NBTInputStream stream, @NonNull CompoundTag parent) throws IOException {
        while (true) {
            var type = stream.readByte();

            if (type == Tag.Id.END) return;
            load(stream, parent, type);
        }
    }

    private static void load(@NonNull NBTInputStream stream, @NonNull CompoundTag parent, int id) throws IOException {
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
