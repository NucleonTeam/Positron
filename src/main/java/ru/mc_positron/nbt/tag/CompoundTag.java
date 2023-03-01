package ru.mc_positron.nbt.tag;

import lombok.NonNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class CompoundTag {

    private final ConcurrentHashMap<Tag<?>, Object> map = new ConcurrentHashMap<>();

    public CompoundTag() {

    }

    public <T> @NonNull CompoundTag set(@NonNull Tag<T> tag, @NonNull T value) {
        map.put(tag, value);
        return this;
    }

    public <T> @NonNull CompoundTag setDefault(@NonNull Tag<T> tag, @NonNull T defaultValue) {
        return contains(tag)? this : set(tag, defaultValue);
    }

    public <T> @NonNull CompoundTag setList(@NonNull Tag<T> tag, @NonNull List<T> list) {
        map.put(tag, list);
        return this;
    }

    public <T> @NonNull T get(@NonNull Tag<T> tag) throws IllegalArgumentException {
        if (contains(tag)) return (T) map.get(tag);

        throw new IllegalArgumentException("Index '" + tag.getKey() + "' not fount");
    }

    public <T> T get(@NonNull Tag<T> tag, T defaultValue) throws IllegalArgumentException {
        return contains(tag)? get(tag) : defaultValue;
    }

    public <T> @NonNull List<T> getList(@NonNull Tag<T> tag) {
        if (contains(tag)) return (List<T>) map.get(tag);

        throw new IllegalArgumentException("Index '" + tag.getKey() + "' not fount");
    }

    public @NonNull CompoundTag remove(@NonNull String key) {
        for (var tag: map.keySet()) {
            if (key.equals(tag.getKey())) {
                map.remove(tag);
                return this;
            }
        }
        return this;
    }

    public @NonNull CompoundTag remove(@NonNull Tag<?> tag) {
        map.remove(tag);
        return this;
    }

    public boolean contains(@NonNull Tag<?> tag) {
        return map.containsKey(tag);
    }

    public @NonNull Tag<?> getTag(@NonNull String key) {
        for (var tag: map.keySet()) {
            if (key.equals(tag.getKey())) {
                return tag;
            }
        }

        throw new IllegalArgumentException("Index '" + key + "' not fount");
    }
}
