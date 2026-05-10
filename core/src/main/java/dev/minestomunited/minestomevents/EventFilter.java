package dev.minestomunited.minestomevents;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;

public sealed interface EventFilter
    permits EventFilter.PlayerFilter, EventFilter.InstanceFilter,
    EventFilter.EntityFilter, EventFilter.InventoryFilter {

    record PlayerFilter(Player player) implements EventFilter {

    }

    record InstanceFilter(Instance instance) implements EventFilter {

    }

    record EntityFilter(Entity entity) implements EventFilter {

    }

    record InventoryFilter(Inventory inventory) implements EventFilter {

    }

    static EventFilter player(Player player) {
        return new PlayerFilter(player);
    }

    static EventFilter instance(Instance instance) {
        return new InstanceFilter(instance);
    }

    static EventFilter entity(Entity entity) {
        return new EntityFilter(entity);
    }

    static EventFilter inventory(Inventory inventory) {
        return new InventoryFilter(inventory);
    }
}
