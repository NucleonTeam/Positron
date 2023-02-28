package ru.mc_positron.blockentity;

import lombok.NonNull;

public interface NameableBlockEntity {

    @NonNull String getName();

    void setName(@NonNull String name);

    boolean hasName();
}
