package ru.mc_positron.entity.attribute;

public interface Attributes {

    Attribute ABSORPTION = new Attribute(0, "minecraft:absorption", 0.00f, 340282346638528859811704183484516925440.00f, 0.00f);
    Attribute SATURATION = new Attribute(1, "minecraft:player.saturation", 0.00f, 20.00f, 5.00f);
    Attribute EXHAUSTION = new Attribute(2, "minecraft:player.exhaustion", 0.00f, 5.00f, 0.41f);
    Attribute KNOCKBACK_RESISTANCE = new Attribute(3, "minecraft:knockback_resistance", 0.00f, 1.00f, 0.00f);
    Attribute MAX_HEALTH = new Attribute(4, "minecraft:health", 0.00f, 20.00f, 20.00f);
    Attribute MOVEMENT_SPEED = new Attribute(5, "minecraft:movement", 0.00f, 340282346638528859811704183484516925440.00f, 0.10f);
    Attribute FOLLOW_RANGE = new Attribute(6, "minecraft:follow_range", 0.00f, 2048.00f, 16.00f, false);
    Attribute HUNGER = new Attribute(7, "minecraft:player.hunger", 0.00f, 20.00f, 20.00f);
    Attribute ATTACK_DAMAGE = new Attribute(8, "minecraft:attack_damage", 0.00f, 340282346638528859811704183484516925440.00f, 1.00f, false);
    Attribute EXPERIENCE_LEVEL = new Attribute(9, "minecraft:player.level", 0.00f, 24791.00f, 0.00f);
    Attribute EXPERIENCE = new Attribute(10, "minecraft:player.experience", 0.00f, 1.00f, 0.00f);
    Attribute LUCK = new Attribute(11, "minecraft:luck", -1024, 1024, 0);
}
