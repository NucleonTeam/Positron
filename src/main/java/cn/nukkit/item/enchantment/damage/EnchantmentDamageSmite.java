package cn.nukkit.item.enchantment.damage;

import cn.nukkit.entity.Entity;

public class EnchantmentDamageSmite extends EnchantmentDamage {

    public EnchantmentDamageSmite() {
        super(ID_DAMAGE_SMITE, "undead", Rarity.UNCOMMON, TYPE.SMITE);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 20;
    }

    @Override
    public double getDamageBonus(Entity entity) {
        return 0;
    }
}
