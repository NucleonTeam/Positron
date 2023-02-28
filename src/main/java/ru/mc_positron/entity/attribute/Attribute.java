package ru.mc_positron.entity.attribute;

import lombok.Getter;
import lombok.NonNull;
import ru.mc_positron.registry.Registry;

@Getter
public final class Attribute {

    private final int id;
    private final String identifier;
    private final float minValue;
    private final float maxValue;
    private final float defaultValue;
    private final boolean sync;
    private boolean registered = false;

    public Attribute(int id, @NonNull String identifier, float minValue, float maxValue, float defaultValue) {
        this(id, identifier, minValue, maxValue, defaultValue, true);
    }

    public Attribute(int id, @NonNull String identifier, float minValue, float maxValue, float defaultValue, boolean sync) {
        this.id = id;
        this.identifier = identifier;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.sync = sync;
    }

    public void register() {
        Registry.requireNotInitialized();
        if (registered) {
            throw new IllegalStateException("Attribute already registered");
        }

        registered = true;
    }

    public @NonNull Attribute with(float minValue, float value, float maxValue) {
        var result = new Attribute(id, identifier, minValue, maxValue, value, sync);
        result.registered = registered;
        return result;
    }

    public @NonNull Attribute.Entry withValue(float value) {
        return new Entry(this, value);
    }

    public @NonNull Attribute.Entry withValue(float value, float maxValue) {
        return with(minValue, defaultValue, maxValue).withValue(value);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (obj instanceof Attribute target) {
            return id == target.id && identifier.equals(target.identifier);
        }

        return false;
    }

    @Getter
    public static class Entry {

        private final Attribute attribute;
        private float value;

        public Entry(@NonNull Attribute attribute) {
            this(attribute, attribute.getDefaultValue());
        }

        public Entry(@NonNull Attribute attribute, float value) {
            if (!attribute.registered) {
                throw new IllegalArgumentException("Attribute " + attribute.identifier + "(" + attribute.id + ") is not registered");
            }

            this.attribute = attribute;
            this.value = value;
        }

        public void setValue(float value) {
            if (value < attribute.minValue || value > attribute.maxValue) {
                throw new IllegalArgumentException("Value " + value + " exceeds the range!");
            }

            this.value = value;
        }
    }
}
