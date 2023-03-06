package ru.mc_positron.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import lombok.NonNull;
import ru.mc_positron.nbt.NbtMap;

import java.io.IOException;
import java.util.ArrayList;

public class CompoundTag extends Tag<NbtMap, NbtMap> {

    CompoundTag(String key) {
        super(key, Id.COMPOUND);
    }

    @Override
    public @NonNull NbtMap read(@NonNull NBTInputStream stream) throws IOException {
        var nbt = new NbtMap();
        while (true) {
            var type = stream.readByte();

            if (type == Tag.Id.END) return nbt;

            var key = stream.readUTF();
            Tag childTag;

            if (type == Tag.Id.LIST) {
                var listType = stream.readByte();
                var len = stream.readInt();
                var list = new ArrayList<>();
                childTag = Tag.pickTag(listType, key);

                for (int i = 0; i < len; i++) {
                    list.add(childTag.read(stream));
                }

                nbt.setList(childTag, list);
                continue;
            }

            childTag = Tag.pickTag(type, key);
            nbt.set(childTag, childTag.read(stream));
        }
    }

    @Override
    public void write(@NonNull NBTOutputStream stream, @NonNull NbtMap map) throws IOException {
        for (var tag: map.keys()) {
            if (map.isList(tag)) {
                stream.writeByte(Id.LIST);
                stream.writeByte(tag.getId());
                tag.writeList(stream, map);
                continue;
            }

            stream.writeByte(tag.getId());
            ((Tag) tag).write(stream, map.get(tag));
        }
        stream.writeByte(Id.END);
    }
}
