package dev.minestomunited.minestomevents;

import java.util.UUID;
import java.util.function.Consumer;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

public final class EventRegistrar {

    private EventRegistrar() {
    }

    public static <E extends Event> void register(Class<E> eventClass, Consumer<E> handler) {
        EventsNode.get().addListener(eventClass, handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends Event> void register(Class<E> eventClass, EventFilter filter, Consumer<E> handler) {
        EventNode child = switch (filter) {
            case EventFilter.PlayerFilter f -> EventNode.value("pf-" + UUID.randomUUID(),
                net.minestom.server.event.EventFilter.PLAYER, p -> p == f.player());
            case EventFilter.InstanceFilter f -> EventNode.value("if-" + UUID.randomUUID(),
                net.minestom.server.event.EventFilter.INSTANCE, i -> i == f.instance());
            case EventFilter.EntityFilter f -> EventNode.value("ef-" + UUID.randomUUID(),
                net.minestom.server.event.EventFilter.ENTITY, e -> e == f.entity());
            case EventFilter.InventoryFilter f -> EventNode.value("ivf-" + UUID.randomUUID(),
                net.minestom.server.event.EventFilter.INVENTORY, inv -> inv == f.inventory());
        };
        child.addListener(eventClass, handler);
        EventsNode.get().addChild(child);
    }
}
