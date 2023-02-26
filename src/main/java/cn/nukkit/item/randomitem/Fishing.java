package cn.nukkit.item.randomitem;

import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.potion.Potion;
import cn.nukkit.utils.DyeColor;

import static cn.nukkit.item.randomitem.RandomItem.*;

/**
 * Created by Snake1999 on 2016/1/15.
 * Package cn.nukkit.item.randomitem in project nukkit.
 */
public final class Fishing {

    public static final Selector ROOT_FISHING = putSelector(new Selector(ROOT));

    public static final Selector FISHES = putSelector(new Selector(ROOT_FISHING), 0.85F);
    public static final Selector TREASURES = putSelector(new Selector(ROOT_FISHING), 0.05F);
    public static final Selector JUNKS = putSelector(new Selector(ROOT_FISHING), 0.1F);

    public static Item getFishingResult(Item rod) {
        int fortuneLevel = 0;
        int lureLevel = 0;
        if (rod != null) {
            if (rod.getEnchantment(Enchantment.ID_FORTUNE_FISHING) != null) {
                fortuneLevel = rod.getEnchantment(Enchantment.ID_FORTUNE_FISHING).getLevel();
            } else if (rod.getEnchantment(Enchantment.ID_LURE) != null) {
                lureLevel = rod.getEnchantment(Enchantment.ID_LURE).getLevel();
            }
        }
        return getFishingResult(fortuneLevel, lureLevel);
    }

    public static Item getFishingResult(int fortuneLevel, int lureLevel) {
        float treasureChance = NukkitMath.clamp(0.05f + 0.01f * fortuneLevel - 0.01f * lureLevel, 0, 1);
        float junkChance = NukkitMath.clamp(0.05f - 0.025f * fortuneLevel - 0.01f * lureLevel, 0, 1);
        float fishChance = NukkitMath.clamp(1 - treasureChance - junkChance, 0, 1);
        putSelector(FISHES, fishChance);
        putSelector(TREASURES, treasureChance);
        putSelector(JUNKS, junkChance);
        Object result = selectFrom(ROOT_FISHING);
        if (result instanceof Item) return (Item) result;
        return null;
    }
}
