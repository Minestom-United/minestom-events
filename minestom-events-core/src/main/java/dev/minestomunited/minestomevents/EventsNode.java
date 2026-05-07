package dev.minestomunited.minestomevents;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

public final class EventsNode {
    private static EventNode<Event> root;

    private EventsNode() {}

    public static void init(EventNode<Event> parent) {
        root = EventNode.all("minestom-events");
        parent.addChild(root);
    }

    public static EventNode<Event> get() {
        if (root == null) {
            throw new IllegalStateException("EventsNode not initialized — call EventsNode.init(parent) first.");
        }
        return root;
    }
}
