package ru.mc_positron.blockentity;

import cn.nukkit.item.Item;
import lombok.NonNull;

public interface ContainerBlockEntity {

    @NonNull Item getItem(int index);

    void setItem(int index, @NonNull Item item);

    @NonNull Item[] getItems();

    int getSize();
}
