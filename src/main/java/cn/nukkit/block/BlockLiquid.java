package cn.nukkit.block;

import cn.nukkit.entity.Entity;
import cn.nukkit.event.block.BlockFromToEvent;
import cn.nukkit.event.block.LiquidFlowEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.level.Level;
import cn.nukkit.level.particle.SmokeParticle;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.LevelEventPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BlockLiquid extends BlockTransparentMeta {

    private final byte CAN_FLOW_DOWN = 1;
    private final byte CAN_FLOW = 0;
    private final byte BLOCKED = -1;
    public int adjacentSources = 0;
    protected Vector3d flowVector = null;
    private Long2ByteMap flowCostVisited = new Long2ByteOpenHashMap();

    protected BlockLiquid(int meta) {
        super(meta);
    }

    @Override
    public boolean canBeFlowedInto() {
        return true;
    }

    protected AxisAlignedBB recalculateBoundingBox() {
        return null;
    }

    public Item[] getDrops(Item item) {
        return new Item[0];
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public boolean isBreakable(Item item) {
        return false;
    }

    @Override
    public boolean canBeReplaced() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public double getMaxY() {
        return getPosition().y() + 1 - getFluidHeightPercent();
    }

    @Override
    protected AxisAlignedBB recalculateCollisionBoundingBox() {
        return this;
    }

    public float getFluidHeightPercent() {
        float d = (float) this.getDamage();
        if (d >= 8) {
            d = 0;
        }

        return (d + 1) / 9f;
    }

    protected int getFlowDecay(Block block) {
        if (block.getId() != this.getId()) {
            return -1;
        }
        return block.getDamage();
    }

    protected int getEffectiveFlowDecay(Block block) {
        if (block.getId() != this.getId()) {
            return -1;
        }
        int decay = block.getDamage();
        if (decay >= 8) {
            decay = 0;
        }
        return decay;
    }

    public void clearCaches() {
        this.flowVector = null;
        this.flowCostVisited.clear();
    }

    public Vector3d getFlowVector() {
        if (this.flowVector != null) {
            return this.flowVector;
        }
        var vector = new Vector3i(0, 0, 0);
        int decay = this.getEffectiveFlowDecay(this);
        for (int j = 0; j < 4; ++j) {
            int x = getPosition().x();
            int y = getPosition().y();
            int z = getPosition().z();
            switch (j) {
                case 0 -> --x;
                case 1 -> x++;
                case 2 -> z--;
                default -> z++;
            }
            Block sideBlock = getWorld().getBlock(x, y, z);
            int blockDecay = this.getEffectiveFlowDecay(sideBlock);
            if (blockDecay < 0) {
                if (!sideBlock.canBeFlowedInto()) {
                    continue;
                }
                blockDecay = this.getEffectiveFlowDecay(getWorld().getBlock(x, y - 1, z));
                if (blockDecay >= 0) {
                    int realDecay = blockDecay - (decay - 8);
                    vector = vector.add(sideBlock.getPosition().sub(getPosition()).mul(realDecay));
                }
            } else {
                int realDecay = blockDecay - decay;
                vector = vector.add(sideBlock.getPosition().sub(getPosition()).mul(realDecay));
            }
        }

        var result = vector.toDouble();
        if (this.getDamage() >= 8) {
            if (!this.canFlowInto(getWorld().getBlock(getPosition().sub(0, 0, 1))) ||
                    !this.canFlowInto(getWorld().getBlock(getPosition().add(0, 0, 1))) ||
                    !this.canFlowInto(getWorld().getBlock(getPosition().sub(1, 0, 0))) ||
                    !this.canFlowInto(getWorld().getBlock(getPosition().add(1, 0, 0))) ||
                    !this.canFlowInto(getWorld().getBlock(getPosition().sub(0, -1, 1))) ||
                    !this.canFlowInto(getWorld().getBlock(getPosition().add(0, 1, 1))) ||
                    !this.canFlowInto(getWorld().getBlock(getPosition().sub(1, -1, 0))) ||
                    !this.canFlowInto(getWorld().getBlock(getPosition().add(1, 1, 0)))) {
                result = result.normalize().add(0, -6, 0);
            }
        }
        return this.flowVector = result.normalize();
    }

    @Override
    public void addVelocityToEntity(Entity entity, Vector3d vector) {
        if (entity.canBeMovedByCurrents()) {
            Vector3d flow = this.getFlowVector();
            vector = vector.add(flow);
        }
    }

    public int getFlowDecayPerBlock() {
        return 1;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            this.checkForHarden();
            getWorld().scheduleUpdate(this, this.tickRate());
            return 0;
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            int decay = this.getFlowDecay(this);
            int multiplier = this.getFlowDecayPerBlock();
            if (decay > 0) {
                int smallestFlowDecay = -100;
                this.adjacentSources = 0;
                smallestFlowDecay = this.getSmallestFlowDecay(getWorld().getBlock(getPosition().sub(0, 0, 1)), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(getWorld().getBlock(getPosition().add(0, 0, 1)), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(getWorld().getBlock(getPosition().sub(1, 0, 0)), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(getWorld().getBlock(getPosition().add(1, 0, 0)), smallestFlowDecay);
                int newDecay = smallestFlowDecay + multiplier;
                if (newDecay >= 8 || smallestFlowDecay < 0) {
                    newDecay = -1;
                }
                int topFlowDecay = this.getFlowDecay(getWorld().getBlock(getPosition().add(0, 1, 0)));
                if (topFlowDecay >= 0) {
                    newDecay = topFlowDecay | 0x08;
                }
                if (newDecay != decay) {
                    decay = newDecay;
                    boolean decayed = decay < 0;
                    Block to;
                    if (decayed) {
                        to = Block.get(BlockID.AIR);
                    } else {
                        to = getBlock(decay);
                    }
                    BlockFromToEvent event = new BlockFromToEvent(this, to);
                    getWorld().getServer().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        getWorld().setBlock(getPosition(), event.getTo(), true, true);
                        if (!decayed) {
                            getWorld().scheduleUpdate(this, this.tickRate());
                        }
                    }
                }
            }
            if (decay >= 0) {
                Block bottomBlock = getWorld().getBlock(getPosition().sub(0, 1, 0));
                this.flowIntoBlock(bottomBlock, decay | 0x08);
                if (decay == 0 || !bottomBlock.canBeFlowedInto()) {
                    int adjacentDecay;
                    if (decay >= 8) {
                        adjacentDecay = 1;
                    } else {
                        adjacentDecay = decay + multiplier;
                    }
                    if (adjacentDecay < 8) {
                        boolean[] flags = this.getOptimalFlowDirections();
                        if (flags[0]) {
                            this.flowIntoBlock(getWorld().getBlock(getPosition().sub(1, 0, 0)), adjacentDecay);
                        }
                        if (flags[1]) {
                            this.flowIntoBlock(getWorld().getBlock(getPosition().add(1, 0, 0)), adjacentDecay);
                        }
                        if (flags[2]) {
                            this.flowIntoBlock(getWorld().getBlock(getPosition().sub(0, 0, 1)), adjacentDecay);
                        }
                        if (flags[3]) {
                            this.flowIntoBlock(getWorld().getBlock(getPosition().add(0, 0, 1)), adjacentDecay);
                        }
                    }
                }
                this.checkForHarden();
            }
        }
        return 0;
    }

    protected void flowIntoBlock(Block block, int newFlowDecay) {
        if (this.canFlowInto(block) && !(block instanceof BlockLiquid)) {
            LiquidFlowEvent event = new LiquidFlowEvent(block, this, newFlowDecay);
            getWorld().getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                if (block.getId() > 0) {
                    getWorld().useBreakOn(block.getPosition(), null);
                }
                getWorld().scheduleUpdate(block, this.tickRate());
            }
        }
    }

    private int calculateFlowCost(int blockX, int blockY, int blockZ, int accumulatedCost, int maxCost, int originOpposite, int lastOpposite) {
        int cost = 1000;
        for (int j = 0; j < 4; ++j) {
            if (j == originOpposite || j == lastOpposite) {
                continue;
            }
            int x = blockX;
            int y = blockY;
            int z = blockZ;
            if (j == 0) {
                --x;
            } else if (j == 1) {
                ++x;
            } else if (j == 2) {
                --z;
            } else if (j == 3) {
                ++z;
            }
            long hash = Level.blockHash(x, y, z);
            if (!this.flowCostVisited.containsKey(hash)) {
                Block blockSide = getWorld().getBlock(x, y, z);
                if (!this.canFlowInto(blockSide)) {
                    this.flowCostVisited.put(hash, BLOCKED);
                } else if (getWorld().getBlock(x, y - 1, z).canBeFlowedInto()) {
                    this.flowCostVisited.put(hash, CAN_FLOW_DOWN);
                } else {
                    this.flowCostVisited.put(hash, CAN_FLOW);
                }
            }
            byte status = this.flowCostVisited.get(hash);
            if (status == BLOCKED) {
                continue;
            } else if (status == CAN_FLOW_DOWN) {
                return accumulatedCost;
            }
            if (accumulatedCost >= maxCost) {
                continue;
            }
            int realCost = this.calculateFlowCost(x, y, z, accumulatedCost + 1, maxCost, originOpposite, j ^ 0x01);
            if (realCost < cost) {
                cost = realCost;
            }
        }
        return cost;
    }

    @Override
    public double getHardness() {
        return 100d;
    }

    @Override
    public double getResistance() {
        return 500;
    }

    private boolean[] getOptimalFlowDirections() {
        int[] flowCost = new int[]{
                1000,
                1000,
                1000,
                1000
        };
        int maxCost = 4 / this.getFlowDecayPerBlock();
        for (int j = 0; j < 4; ++j) {
            int x = getPosition().x();
            int y = getPosition().y();
            int z = getPosition().z();
            if (j == 0) {
                --x;
            } else if (j == 1) {
                ++x;
            } else if (j == 2) {
                --z;
            } else {
                ++z;
            }
            Block block = getWorld().getBlock(x, y, z);
            if (!this.canFlowInto(block)) {
                this.flowCostVisited.put(Level.blockHash(x, y, z), BLOCKED);
            } else if (getWorld().getBlock(x, y - 1, z).canBeFlowedInto()) {
                this.flowCostVisited.put(Level.blockHash(x, y, z), CAN_FLOW_DOWN);
                flowCost[j] = maxCost = 0;
            } else if (maxCost > 0) {
                this.flowCostVisited.put(Level.blockHash(x, y, z), CAN_FLOW);
                flowCost[j] = this.calculateFlowCost(x, y, z, 1, maxCost, j ^ 0x01, j ^ 0x01);
                maxCost = Math.min(maxCost, flowCost[j]);
            }
        }
        this.flowCostVisited.clear();
        double minCost = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double d = flowCost[i];
            if (d < minCost) {
                minCost = d;
            }
        }
        boolean[] isOptimalFlowDirection = new boolean[4];
        for (int i = 0; i < 4; ++i) {
            isOptimalFlowDirection[i] = (flowCost[i] == minCost);
        }
        return isOptimalFlowDirection;
    }

    private int getSmallestFlowDecay(Block block, int decay) {
        int blockDecay = this.getFlowDecay(block);
        if (blockDecay < 0) {
            return decay;
        } else if (blockDecay == 0) {
            ++this.adjacentSources;
        } else if (blockDecay >= 8) {
            blockDecay = 0;
        }
        return (decay >= 0 && blockDecay >= decay) ? decay : blockDecay;
    }

    protected void checkForHarden() {
    }

    protected void triggerLavaMixEffects(Vector3 pos) {
        Random random = ThreadLocalRandom.current();
        getWorld().addLevelEvent(pos.add(0.5, 0.5, 0.5), LevelEventPacket.EVENT_SOUND_FIZZ, (int) ((random.nextFloat() - random.nextFloat()) * 800) + 2600);

        for (int i = 0; i < 8; ++i) {
            getWorld().addParticle(new SmokeParticle(pos.add(Math.random(), 1.2, Math.random())));
        }
    }

    public abstract BlockLiquid getBlock(int meta);

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public void onEntityCollide(Entity entity) {
        entity.resetFallDistance();
    }

    protected boolean liquidCollide(Block cause, Block result) {
        BlockFromToEvent event = new BlockFromToEvent(this, result);
        getWorld().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        getWorld().setBlock(getPosition(), event.getTo(), true, true);
        getWorld().addLevelSoundEvent(getPosition().toFloat().add(0.5, 0.5, 0.5), LevelSoundEventPacket.SOUND_FIZZ);
        return true;
    }

    protected boolean canFlowInto(Block block) {
        return block.canBeFlowedInto() && !(block instanceof BlockLiquid && block.getDamage() == 0);
    }

    @Override
    public Item toItem() {
        return new ItemBlock(Block.get(BlockID.AIR));
    }
}
