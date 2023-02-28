package ru.mc_positron.registry;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import ru.mc_positron.entity.attribute.Attribute;
import ru.mc_positron.entity.attribute.Attributes;

import java.util.Collection;
import java.util.HashMap;

public final class Entities {

    private final HashMap<String, Attribute> attributes = new HashMap<>();
    private final HashMap<Integer, Attribute> attributesNumId = new HashMap<>();

    Entities() {
        registerDefaultAttributes();
        registerDefaultEntities();
    }

    public void registerAttribute(@NonNull Attribute attribute) {
        Registry.requireNotInitialized();
        if (attributes.containsKey(attribute.getIdentifier())) {
            throw new IllegalArgumentException("Attribute " + attribute.getIdentifier() + "(" + attribute.getId() + ") already registered");
        }

        attribute.register();

        attributes.put(attribute.getIdentifier(), attribute);
        attributesNumId.put(attribute.getId(), attribute);
    }

    public @NonNull Attribute getAttributeById(int id) {
        if (!attributesNumId.containsKey(id)) {
            throw new IllegalArgumentException("Attribute with id " + id + " is not registered");
        }

        return attributesNumId.get(id);
    }

    public @NonNull Attribute getAttributeByIdentifier(@NonNull String identifier) {
        if (!attributes.containsKey(identifier)) {
            throw new IllegalArgumentException("Attribute with identifier " + identifier + " is not registered");
        }

        return attributes.get(identifier);
    }

    public @NonNull Collection<Attribute> getAttributes() {
        return ImmutableMap.copyOf(attributes).values();
    }

    void completeInitialization() {

    }

    private void registerDefaultAttributes() {
        registerAttribute(Attributes.ABSORPTION);
        registerAttribute(Attributes.SATURATION);
        registerAttribute(Attributes.EXHAUSTION);
        registerAttribute(Attributes.KNOCKBACK_RESISTANCE);
        registerAttribute(Attributes.MAX_HEALTH);
        registerAttribute(Attributes.MOVEMENT_SPEED);
        registerAttribute(Attributes.FOLLOW_RANGE);
        registerAttribute(Attributes.HUNGER);
        registerAttribute(Attributes.ATTACK_DAMAGE);
        registerAttribute(Attributes.EXPERIENCE_LEVEL);
        registerAttribute(Attributes.EXPERIENCE);
        registerAttribute(Attributes.LUCK);
    }

    private void registerDefaultEntities() {

    }
}
