package ru.mc_positron.nbt;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import ru.mc_positron.nbt.tag.Tag;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class Nbt {

    private final ConcurrentHashMap<Tag<?>, Object> map = new ConcurrentHashMap<>();

    public Nbt() {

    }

    void init(@NonNull Tag<?> tag, Object value) {
        map.put(tag, value);
    }

    public <T> @NonNull Nbt set(@NonNull Tag<T> tag, @NonNull T value) {
        map.put(tag, value);
        return this;
    }

    public <T> @NonNull Nbt setDefault(@NonNull Tag<T> tag, @NonNull T defaultValue) {
        return contains(tag)? this : set(tag, defaultValue);
    }

    void initList(@NonNull Tag<?> tag, @NonNull List<?> list) {
        map.put(tag, list);
    }

    public <T> @NonNull Nbt setList(@NonNull Tag<T> tag, @NonNull List<T> list) {
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

    public @NonNull Nbt remove(@NonNull String key) {
        for (var tag: map.keySet()) {
            if (key.equals(tag.getKey())) {
                map.remove(tag);
                return this;
            }
        }
        return this;
    }

    public @NonNull Nbt remove(@NonNull Tag<?> tag) {
        map.remove(tag);
        return this;
    }

    public boolean contains(@NonNull Tag<?> tag) {
        return map.containsKey(tag);
    }

    public boolean isList(@NonNull Tag<?> tag) {
        if (contains(tag)) {
            return map.get(tag) instanceof List<?>;
        }

        throw new IllegalArgumentException("Index '" + tag.getKey() + "' not fount");
    }

    public @NonNull Tag<?> getTag(@NonNull String key) {
        for (var tag: map.keySet()) {
            if (key.equals(tag.getKey())) {
                return tag;
            }
        }

        throw new IllegalArgumentException("Index '" + key + "' not fount");
    }

    public @NonNull Collection<Tag<?>> keys() {
        return ImmutableMap.copyOf(map).keySet();
    }
}
