package cn.nukkit.inventory.transaction;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.mob.inventory.RepairItemEvent;
import cn.nukkit.inventory.AnvilInventory;
import cn.nukkit.inventory.FakeBlockMenu;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.transaction.action.RepairItemAction;
import cn.nukkit.inventory.transaction.action.InventoryAction;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.network.protocol.types.NetworkInventoryAction;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.List;

public class RepairItemTransaction extends InventoryTransaction {

    private Item inputItem;
    private Item materialItem;
    private Item outputItem;

    private int cost;

    public RepairItemTransaction(Player source, List<InventoryAction> actions) {
        super(source, actions);
    }

    @Override
    public boolean canExecute() {
        Inventory inventory = getSource().getWindowById(Player.ANVIL_WINDOW_ID);
        if (inventory == null) {
            return false;
        }
        AnvilInventory anvilInventory = (AnvilInventory) inventory;
        return this.inputItem != null && this.outputItem != null && this.inputItem.equals(anvilInventory.getInputSlot(), true, true)
                && (!this.hasMaterial() || this.materialItem.equals(anvilInventory.getMaterialSlot(), true, true))
                && this.checkRecipeValid();
    }

    @Override
    public boolean execute() {
        if (this.hasExecuted() || !this.canExecute()) {
            this.source.removeAllWindows(false);
            this.sendInventories();
            return false;
        }
        AnvilInventory inventory = (AnvilInventory) getSource().getWindowById(Player.ANVIL_WINDOW_ID);

        if (inventory.getCost() != this.cost && !this.source.isCreative()) {
            this.source.getServer().getLogger().debug("Got unexpected cost " + inventory.getCost() + " from " + this.source.getName() + "(expected " + this.cost + ")");
        }

        RepairItemEvent event = new RepairItemEvent(inventory, this.inputItem, this.outputItem, this.materialItem, this.cost, this.source);
        this.source.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.source.removeAllWindows(false);
            this.sendInventories();
            return true;
        }

        for (InventoryAction action : this.actions) {
            if (action.execute(this.source)) {
                action.onExecuteSuccess(this.source);
            } else {
                action.onExecuteFail(this.source);
            }
        }

        FakeBlockMenu holder = inventory.getHolder();
        Block block = this.source.getWorld().getBlock(holder.getPosition().toInt());

        if (!this.source.isCreative()) {
            this.source.setExperience(this.source.getExperience(), this.source.getExperienceLevel() - event.getCost());
        }
        return true;
    }

    @Override
    public void addAction(InventoryAction action) {
        super.addAction(action);
        if (action instanceof RepairItemAction) {
            switch (((RepairItemAction) action).getType()) {
                case NetworkInventoryAction.SOURCE_TYPE_ANVIL_INPUT:
                    this.inputItem = action.getTargetItem();
                    break;
                case NetworkInventoryAction.SOURCE_TYPE_ANVIL_RESULT:
                    this.outputItem = action.getSourceItem();
                    break;
                case NetworkInventoryAction.SOURCE_TYPE_ANVIL_MATERIAL:
                    this.materialItem = action.getTargetItem();
                    break;
            }
        }
    }

    private boolean checkRecipeValid() {
        int cost = 0;
        int baseRepairCost = this.inputItem.getRepairCost();

        if (this.isMapRecipe()) {
            if (!this.matchMapRecipe()) {
                return false;
            }
            baseRepairCost = 0;
        } else if (this.hasMaterial()) {
            baseRepairCost += this.materialItem.getRepairCost();

            if (this.inputItem.getMaxDurability() != -1 && this.matchRepairItem()) {
                int maxRepairDamage = this.inputItem.getMaxDurability() / 4;
                int repairDamage = Math.min(this.inputItem.getDamage(), maxRepairDamage);
                if (repairDamage <= 0) {
                    return false;
                }

                int damage = this.inputItem.getDamage();
                for (; repairDamage > 0 && cost < this.materialItem.getCount(); cost++) {
                    damage = damage - repairDamage;
                    repairDamage = Math.min(damage, maxRepairDamage);
                }
                if (this.outputItem.getDamage() != damage) {
                    return false;
                }
            } else {
                boolean consumeEnchantedBook = false;
                if (!consumeEnchantedBook && (this.inputItem.getMaxDurability() == -1 || this.inputItem.getId() != this.materialItem.getId())) {
                    return false;
                }

                if (!consumeEnchantedBook && this.inputItem.getMaxDurability() != -1) {
                    int damage = this.inputItem.getDamage() - this.inputItem.getMaxDurability() + this.materialItem.getDamage() - this.inputItem.getMaxDurability() * 12 / 100 + 1;
                    if (damage < 0) {
                        damage = 0;
                    }

                    if (damage < this.inputItem.getDamage()) {
                        if (this.outputItem.getDamage() != damage) {
                            return false;
                        }
                        cost += 2;
                    }
                }

                Int2IntMap enchantments = new Int2IntOpenHashMap();
                enchantments.defaultReturnValue(-1);
                for (Enchantment enchantment : this.inputItem.getEnchantments()) {
                    enchantments.put(enchantment.getId(), enchantment.getLevel());
                }

                boolean hasCompatibleEnchantments = false;
                boolean hasIncompatibleEnchantments = false;
                for (Enchantment materialEnchantment : this.materialItem.getEnchantments()) {
                    Enchantment enchantment = this.inputItem.getEnchantment(materialEnchantment.getId());
                    int inputLevel = enchantment != null ? enchantment.getLevel() : 0;
                    int materialLevel = materialEnchantment.getLevel();
                    int outputLevel = inputLevel == materialLevel ? materialLevel + 1 : Math.max(materialLevel, inputLevel);

                    boolean canEnchant = materialEnchantment.canEnchant(this.inputItem);
                    for (Enchantment inputEnchantment : this.inputItem.getEnchantments()) {
                        if (inputEnchantment.getId() != materialEnchantment.getId() && !materialEnchantment.isCompatibleWith(inputEnchantment)) {
                            canEnchant = false;
                            cost++;
                        }
                    }

                    if (!canEnchant) {
                        hasIncompatibleEnchantments = true;
                    } else {
                        hasCompatibleEnchantments = true;
                        if (outputLevel > materialEnchantment.getMaxLevel()) {
                            outputLevel = materialEnchantment.getMaxLevel();
                        }

                        enchantments.put(materialEnchantment.getId(), outputLevel);
                        int rarityFactor;
                        switch (materialEnchantment.getRarity()) {
                            case COMMON:
                                rarityFactor = 1;
                                break;
                            case UNCOMMON:
                                rarityFactor = 2;
                                break;
                            case RARE:
                                rarityFactor = 4;
                                break;
                            case VERY_RARE:
                            default:
                                rarityFactor = 8;
                                break;
                        }

                        if (consumeEnchantedBook) {
                            rarityFactor = Math.max(1, rarityFactor / 2);
                        }

                        cost += rarityFactor * Math.max(0, outputLevel - inputLevel);
                        if (this.inputItem.getCount() > 1) {
                            cost = 40;
                        }
                    }
                }

                Enchantment[] outputEnchantments = this.outputItem.getEnchantments();
                if (hasIncompatibleEnchantments && !hasCompatibleEnchantments || enchantments.size() != outputEnchantments.length) {
                    return false;
                }

                for (Enchantment enchantment : outputEnchantments) {
                    if (enchantments.get(enchantment.getId()) != enchantment.getLevel()) {
                        return false;
                    }
                }
            }
        }

        int renameCost = 0;
        if (!this.inputItem.getCustomName().equals(this.outputItem.getCustomName())) {
            if (this.outputItem.getCustomName().length() > 30) {
                return false;
            }
            renameCost = 1;
            cost += renameCost;
        }

        this.cost = baseRepairCost + cost;
        if (renameCost == cost && renameCost > 0 && this.cost >= 40) {
            this.cost = 39;
        }
        if (baseRepairCost < 0 || cost < 0 || cost == 0 && !this.isMapRecipe() || this.cost > 39 && !this.source.isCreative()) {
            return false;
        }

        int nextBaseRepairCost = this.inputItem.getRepairCost();
        if (!this.isMapRecipe()) {
            if (this.hasMaterial() && nextBaseRepairCost < this.materialItem.getRepairCost()) {
                nextBaseRepairCost = this.materialItem.getRepairCost();
            }
            if (renameCost == 0 || renameCost != cost) {
                nextBaseRepairCost = 2 * nextBaseRepairCost + 1;
            }
        }
        if (this.outputItem.getRepairCost() != nextBaseRepairCost) {
            this.source.getServer().getLogger().debug("Got unexpected base cost " + this.outputItem.getRepairCost() + " from " + this.source.getName() + "(expected " + nextBaseRepairCost + ")");
            return false;
        }

        return true;
    }

    private boolean hasMaterial() {
        return this.materialItem != null && !this.materialItem.isNull();
    }

    private boolean isMapRecipe() {
        return false;
    }

    private boolean matchMapRecipe() {
        return false;
    }

    private boolean matchRepairItem() {
        return false;
    }

    public Item getInputItem() {
        return this.inputItem;
    }

    public Item getMaterialItem() {
        return this.materialItem;
    }

    public Item getOutputItem() {
        return this.outputItem;
    }

    public int getCost() {
        return this.cost;
    }

    public static boolean checkForRepairItemPart(List<InventoryAction> actions) {
        for (InventoryAction action : actions) {
            if (action instanceof RepairItemAction) {
                return true;
            }
        }
        return false;
    }
}
