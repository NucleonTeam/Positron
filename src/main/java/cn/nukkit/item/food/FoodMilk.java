package cn.nukkit.item.food;

import cn.nukkit.Player;

public class FoodMilk extends Food {
    @Override
    protected boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        player.removeAllEffects();
        return true;
    }
}
