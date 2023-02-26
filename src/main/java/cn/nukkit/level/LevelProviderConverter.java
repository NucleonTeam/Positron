package cn.nukkit.level;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.anvil.Anvil;
import cn.nukkit.level.format.anvil.Chunk;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.format.generic.ChunkConverter;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.LevelException;
import cn.nukkit.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LevelProviderConverter {

    private LevelProvider provider;
    private Class<? extends LevelProvider> toClass;
    private Level level;
    private String path;

    LevelProviderConverter(Level level, String path) {
        this.level = level;
        this.path = path;
    }

    LevelProviderConverter from(LevelProvider provider) {
        this.provider = provider;
        return this;
    }

    LevelProviderConverter to(Class<? extends LevelProvider> toClass) {
        if (toClass != Anvil.class) {
            throw new IllegalArgumentException("To type can be only Anvil");
        }
        this.toClass = toClass;
        return this;
    }

    LevelProvider perform() throws IOException {
        new File(path).mkdir();
        File dat = new File(provider.getPath(), "level.dat.old");
        new File(provider.getPath(), "level.dat").renameTo(dat);
        Utils.copyFile(dat, new File(path, "level.dat"));
        LevelProvider result;
        try {
            result = toClass.getConstructor(Level.class, String.class).newInstance(level, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (toClass == Anvil.class) {
            result.doGarbageCollection();
        }
        return result;
    }

    private static int getChunkX(byte[] key) {
        return (key[3] << 24) |
                (key[2] << 16) |
                (key[1] << 8) |
                key[0];
    }

    private static int getChunkZ(byte[] key) {
        return (key[7] << 24) |
                (key[6] << 16) |
                (key[5] << 8) |
                key[4];
    }
}
