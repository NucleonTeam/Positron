package cn.nukkit.inventory;

import cn.nukkit.item.Item;

import java.util.Map;
import java.util.TreeMap;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class Fuel {
    public static final Map<Integer, Short> duration = new TreeMap<>();

    static {
        duration.put(Item.COAL, (short) 1600);
        duration.put(Item.WOODEN_AXE, (short) 200);
        duration.put(Item.WOODEN_PICKAXE, (short) 200);
        duration.put(Item.WOODEN_SWORD, (short) 200);
        duration.put(Item.WOODEN_SHOVEL, (short) 200);
        duration.put(Item.WOODEN_HOE, (short) 200);
        duration.put(Item.STICK, (short) 100);
        duration.put(Item.BUCKET, (short) 20000);
        duration.put(Item.BOW, (short) 200);
        duration.put(Item.BOWL, (short) 200);
        duration.put(Item.BOAT, (short) 1200);
        duration.put(Item.BLAZE_ROD, (short) 2400);
        duration.put(Item.FISHING_ROD, (short) 300);
        duration.put(Item.WOODEN_DOOR, (short) 200);
        duration.put(Item.SPRUCE_DOOR, (short) 200);
        duration.put(Item.BIRCH_DOOR, (short) 200);
        duration.put(Item.JUNGLE_DOOR, (short) 200);
        duration.put(Item.ACACIA_DOOR, (short) 200);
        duration.put(Item.DARK_OAK_DOOR, (short) 200);
        duration.put(Item.BANNER, (short) 300);
        duration.put(Item.SIGN, (short) 200);
    }
}
