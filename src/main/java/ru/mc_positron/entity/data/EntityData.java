package ru.mc_positron.entity.data;

import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import lombok.NonNull;

import java.util.Objects;

public abstract class EntityData<T> {

    @Getter private int id;

    protected EntityData(int id) {
        this.id = id;
    }

    public abstract @NonNull EntityData.Type getType();

    public abstract T getData();

    public abstract void setData(T data);

    public final @NonNull EntityData<T> setId(int id) {
        this.id = id;
        return this;
    }

    public abstract void writeTo(@NonNull BinaryStream stream);

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EntityData &&
                ((EntityData<?>) obj).getId() == getId() &&
                Objects.equals(((EntityData<?>) obj).getData(), getData());
    }

    public interface Key {
        int FLAGS = 0;
        int HEALTH = 1; //int (minecart/boat)
        int VARIANT = 2; //int
        int COLOR = 3, COLOUR = 3; //byte
        int NAMETAG = 4; //string
        int OWNER_EID = 5; //long
        int TARGET_EID = 6; //long
        int AIR = 7; //short
        int POTION_COLOR = 8; //int (ARGB!)
        int POTION_AMBIENT = 9; //byte
        int JUMP_DURATION = 10; //long
        int HURT_TIME = 11; //int (minecart/boat)
        int HURT_DIRECTION = 12; //int (minecart/boat)
        int PADDLE_TIME_LEFT = 13; //float
        int PADDLE_TIME_RIGHT = 14; //float
        int EXPERIENCE_VALUE = 15; //int (xp orb)
        int DISPLAY_ITEM = 16; //int (id | (data << 16))
        int DISPLAY_OFFSET = 17; //int
        int HAS_DISPLAY = 18; //byte (must be 1 for minecart to show block inside)
        int SWELL = 19;
        int OLD_SWELL = 20;
        int SWELL_DIR = 21;
        int CHARGE_AMOUNT = 22;
        int ENDERMAN_HELD_RUNTIME_ID = 23; //short
        int ENTITY_AGE = 24; //short
        int PLAYER_FLAGS = 26; //byte
        int PLAYER_INDEX = 27;
        int PLAYER_BED_POSITION = 28; //block coords
        int FIREBALL_POWER_X = 29; //float
        int FIREBALL_POWER_Y = 30;
        int FIREBALL_POWER_Z = 31;
        int AUX_POWER = 32;
        int FISH_X = 33;
        int FISH_Z = 34;
        int FISH_ANGLE = 35;
        int POTION_AUX_VALUE = 36; //short
        int LEAD_HOLDER_EID = 37; //long
        int SCALE = 38; //float
        int HAS_NPC_COMPONENT = 39; //byte
        int NPC_SKIN_ID = 40; //string
        int URL_TAG = 41; //string
        int MAX_AIR = 42; //short
        int MARK_VARIANT = 43; //int
        int CONTAINER_TYPE = 44; //byte
        int CONTAINER_BASE_SIZE = 45; //int
        int CONTAINER_EXTRA_SLOTS_PER_STRENGTH = 46; //int
        int BLOCK_TARGET = 47; //block coords (ender crystal)
        int WITHER_INVULNERABLE_TICKS = 48; //int
        int WITHER_TARGET_1 = 49; //long
        int WITHER_TARGET_2 = 50; //long
        int WITHER_TARGET_3 = 51; //long
        int AERIAL_ATTACK = 52;
        int BOUNDING_BOX_WIDTH = 53; //float
        int BOUNDING_BOX_HEIGHT = 54; //float
        int FUSE_LENGTH = 55; //int
        int RIDER_SEAT_POSITION = 56; //vector3f
        int RIDER_ROTATION_LOCKED = 57; //byte
        int RIDER_MAX_ROTATION = 58; //float
        int RIDER_MIN_ROTATION = 59; //float
        int RIDER_ROTATION_OFFSET = 60;
        int AREA_EFFECT_CLOUD_RADIUS = 61; //float
        int AREA_EFFECT_CLOUD_WAITING = 62; //int
        int AREA_EFFECT_CLOUD_PARTICLE_ID = 63; //int
        int SHULKER_PEEK_ID = 64; //int
        int SHULKER_ATTACH_FACE = 65; //byte
        int SHULKER_ATTACHED = 66; //short
        int SHULKER_ATTACH_POS = 67; //block coords
        int TRADING_PLAYER_EID = 68; //long
        int TRADING_CAREER = 69;
        int HAS_COMMAND_BLOCK = 70;
        int COMMAND_BLOCK_COMMAND = 71; //string
        int COMMAND_BLOCK_LAST_OUTPUT = 72; //string
        int COMMAND_BLOCK_TRACK_OUTPUT = 73; //byte
        int CONTROLLING_RIDER_SEAT_NUMBER = 74; //byte
        int STRENGTH = 75; //int
        int MAX_STRENGTH = 76; //int
        int SPELL_CASTING_COLOR = 77; //int
        int LIMITED_LIFE = 78;
        int ARMOR_STAND_POSE_INDEX = 79; // int
        int ENDER_CRYSTAL_TIME_OFFSET = 80; // int
        int ALWAYS_SHOW_NAMETAG = 81; // byte
        int COLOR_2 = 82; // byte
        int NAME_AUTHOR = 83;
        int SCORE_TAG = 84; // String
        int BALLOON_ATTACHED_ENTITY = 85; // long
        int PUFFERFISH_SIZE = 86;
        int BUBBLE_TIME = 87;
        int AGENT = 88;
        int SITTING_AMOUNT = 89;
        int SITTING_AMOUNT_PREVIOUS = 90;
        int EATING_COUNTER = 91;
        int FLAGS_EXTENDED = 92;
        int LAYING_AMOUNT = 93;
        int LAYING_AMOUNT_PREVIOUS = 94;
        int DURATION = 95;
        int SPAWN_TIME = 96;
        int CHANGE_RATE = 97;
        int CHANGE_ON_PICKUP = 98;
        int PICKUP_COUNT = 99;
        int INTERACTIVE_TAG = 100; //string (button text)
        int TRADE_TIER = 101;
        int MAX_TRADE_TIER = 102;
        int TRADE_EXPERIENCE = 103;
        int SKIN_ID = 104; // int ???
        int SPAWNING_FRAMES = 105;
        int COMMAND_BLOCK_TICK_DELAY = 106;
        int COMMAND_BLOCK_EXECUTE_ON_FIRST_TICK = 107;
        int AMBIENT_SOUND_INTERVAL = 108;
        int AMBIENT_SOUND_INTERVAL_RANGE = 109;
        int AMBIENT_SOUND_EVENT_NAME = 110;
        int FALL_DAMAGE_MULTIPLIER = 111;
        int NAME_RAW_TEXT = 112;
        int CAN_RIDE_TARGET = 113;
        int LOW_TIER_CURED_DISCOUNT = 114;
        int HIGH_TIER_CURED_DISCOUNT = 115;
        int NEARBY_CURED_DISCOUNT = 116;
        int NEARBY_CURED_DISCOUNT_TIMESTAMP = 117;
        int HITBOX = 118;
        int IS_BUOYANT = 119;
        int BASE_RUNTIME_ID = 120;
        int FREEZING_EFFECT_STRENGTH = 121;
        int BUOYANCY_DATA = 122;
        int GOAT_HORN_COUNT = 123;
        int UPDATE_PROPERTIES = 124;
        int MOVEMENT_SOUND_DISTANCE_OFFSET = 125;
        int HEARTBEAT_INTERVAL_TICKS = 126;
        int HEARTBEAT_SOUND_EVENT = 127;
        int PLAYER_LAST_DEATH_POS = 128;
        int PLAYER_LAST_DEATH_DIMENSION = 129;
        int PLAYER_HAS_DIED = 130;
    }

    public enum Type {
        BYTE(0),
        SHORT(1),
        INT(2),
        FLOAT(3),
        STRING(4),
        NBT(5),
        POS(6),
        LONG(7),
        VECTOR(8);

        @Getter private final int code;

        Type(int code) {
            this.code = code;
        }

        public static Type of(int code) {
            for (Type type: values()) {
                if (code == type.code) return type;
            }
            return null;
        }
    }

    interface Flag {
        int ONFIRE = 0;
        int SNEAKING = 1;
        int RIDING = 2;
        int SPRINTING = 3;
        int ACTION = 4;
        int INVISIBLE = 5;
        int TEMPTED = 6;
        int INLOVE = 7;
        int SADDLED = 8;
        int POWERED = 9;
        int IGNITED = 10;
        int BABY = 11; //disable head scaling
        int CONVERTING = 12;
        int CRITICAL = 13;
        int CAN_SHOW_NAMETAG = 14;
        int ALWAYS_SHOW_NAMETAG = 15;
        int IMMOBILE = 16, NO_AI = 16;
        int SILENT = 17;
        int WALLCLIMBING = 18;
        int CAN_CLIMB = 19;
        int SWIMMER = 20;
        int CAN_FLY = 21;
        int WALKER = 22;
        int RESTING = 23;
        int SITTING = 24;
        int ANGRY = 25;
        int INTERESTED = 26;
        int CHARGED = 27;
        int TAMED = 28;
        int ORPHANED = 29;
        int LEASHED = 30;
        int SHEARED = 31;
        int GLIDING = 32;
        int ELDER = 33;
        int MOVING = 34;
        int BREATHING = 35;
        int CHESTED = 36;
        int STACKABLE = 37;
        int SHOWBASE = 38;
        int REARING = 39;
        int VIBRATING = 40;
        int IDLING = 41;
        int EVOKER_SPELL = 42;
        int CHARGE_ATTACK = 43;
        int WASD_CONTROLLED = 44;
        int CAN_POWER_JUMP = 45;
        int LINGER = 46;
        int HAS_COLLISION = 47;
        int GRAVITY = 48;
        int FIRE_IMMUNE = 49;
        int DANCING = 50;
        int ENCHANTED = 51;
        int SHOW_TRIDENT_ROPE = 52; // tridents show an animated rope when enchanted with loyalty after they are thrown and return to their owner. To be combined with OWNER_EID
        int CONTAINER_PRIVATE = 53; //inventory is private, doesn't drop contents when killed if true
        int IS_TRANSFORMING = 54;
        int SPIN_ATTACK = 55;
        int SWIMMING = 56;
        int BRIBED = 57; //dolphins have this set when they go to find treasure for the player
        int PREGNANT = 58;
        int LAYING_EGG = 59;
        int RIDER_CAN_PICK = 60;
        int TRANSITION_SETTING = 61;
        int EATING = 62;
        int LAYING_DOWN = 63;
        int SNEEZING = 64;
        int TRUSTING = 65;
        int ROLLING = 66;
        int SCARED = 67;
        int IN_SCAFFOLDING = 68;
        int OVER_SCAFFOLDING = 69;
        int FALL_THROUGH_SCAFFOLDING = 70;
        int BLOCKING = 71;
        int TRANSITION_BLOCKING = 72;
        int BLOCKED_USING_SHIELD = 73;
        int BLOCKED_USING_DAMAGED_SHIELD = 74;
        int SLEEPING = 75;
        int ENTITY_GROW_UP = 76;
        int TRADE_INTEREST = 77;
        int DOOR_BREAKER = 78;
        int BREAKING_OBSTRUCTION = 79;
        int DOOR_OPENER = 80;
        int IS_ILLAGER_CAPTAIN = 81;
        int STUNNED = 82;
        int ROARING = 83;
        int DELAYED_ATTACK = 84;
        int IS_AVOIDING_MOBS = 85;
        int IS_AVOIDING_BLOCKS = 86;
        int FACING_TARGET_TO_RANGE_ATTACK = 87;
        int HIDDEN_WHEN_INVISIBLE = 88;
        int IS_IN_UI = 89;
        int STALKING = 90;
        int EMOTING = 91;
        int CELEBRATING = 92;
        int ADMIRING = 93;
        int CELEBRATING_SPECIAL = 94;
        int RAM_ATTACK = 96;
        int PLAYING_DEAD = 97;
        int IN_ASCENDABLE_BLOCK = 98;
        int OVER_DESCENDABLE_BLOCK = 99;
        int CROAKING = 100;
        int EAT_MOB = 101;
        int JUMP_GOAL_JUMP = 102;
        int EMERGING = 103;
        int SNIFFING = 104;
        int DIGGING = 105;
        int SONIC_BOOM = 106;
    }
}
