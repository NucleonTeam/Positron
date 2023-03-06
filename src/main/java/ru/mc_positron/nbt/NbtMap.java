package ru.mc_positron.nbt;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import ru.mc_positron.nbt.tag.Tag;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class NbtMap {

    private final ConcurrentHashMap<Tag<?, ?>, Object> map = new ConcurrentHashMap<>();

    public NbtMap() {

    }

    void init(@NonNull Tag<?, ?> tag, Object value) {
        map.put(tag, value);
    }

    public <T, V> @NonNull NbtMap set(@NonNull Tag<T, V> tag, @NonNull V value) {
        map.put(tag, value);
        return this;
    }

    public <T, V> @NonNull NbtMap setDefault(@NonNull Tag<T, V> tag, @NonNull V defaultValue) {
        return contains(tag)? this : set(tag, defaultValue);
    }

    void initList(@NonNull Tag<?, ?> tag, @NonNull List<?> list) {
        map.put(tag, list);
    }

    public <T, V> @NonNull NbtMap setList(@NonNull Tag<T, V> tag, @NonNull List<V> list) {
        map.put(tag, list);
        return this;
    }

    public <V> @NonNull V get(@NonNull Tag<?, V> tag) throws IllegalArgumentException {
        if (contains(tag)) return (V) map.get(tag);

        throw new IllegalArgumentException("Index '" + tag.getKey() + "' not fount");
    }

    public <T, V> V get(@NonNull Tag<T, V> tag, V defaultValue) throws IllegalArgumentException {
        return contains(tag)? get(tag) : defaultValue;
    }

    public <T, V> @NonNull List<V> getList(@NonNull Tag<T, V> tag) {
        if (contains(tag)) return (List<V>) map.get(tag);

        throw new IllegalArgumentException("Index '" + tag.getKey() + "' not fount");
    }

    public @NonNull NbtMap remove(@NonNull String key) {
        for (var tag: map.keySet()) {
            if (key.equals(tag.getKey())) {
                map.remove(tag);
                return this;
            }
        }
        return this;
    }

    public @NonNull NbtMap remove(@NonNull Tag<?, ?> tag) {
        map.remove(tag);
        return this;
    }

    public boolean contains(@NonNull Tag<?, ?> tag) {
        return map.containsKey(tag);
    }

    public boolean isList(@NonNull Tag<?, ?> tag) {
        if (contains(tag)) {
            return map.get(tag) instanceof List<?>;
        }

        throw new IllegalArgumentException("Index '" + tag.getKey() + "' not fount");
    }

    public @NonNull Tag<?, ?> getTag(@NonNull String key) {
        for (var tag: map.keySet()) {
            if (key.equals(tag.getKey())) {
                return tag;
            }
        }

        throw new IllegalArgumentException("Index '" + key + "' not fount");
    }

    public @NonNull Collection<Tag<?, ?>> keys() {
        return ImmutableMap.copyOf(map).keySet();
    }
}
