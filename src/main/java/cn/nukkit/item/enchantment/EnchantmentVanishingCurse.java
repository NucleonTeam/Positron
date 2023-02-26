package cn.nukkit.item.enchantment;

import cn.nukkit.item.Item;

public class EnchantmentVanishingCurse extends Enchantment {
    protected EnchantmentVanishingCurse() {
        super(ID_VANISHING_CURSE, "curse.vanishing", Rarity.VERY_RARE, EnchantmentType.BREAKABLE);
    }

    @Override
    public boolean canEnchant(Item item) {
        return super.canEnchant(item);
    }
}
