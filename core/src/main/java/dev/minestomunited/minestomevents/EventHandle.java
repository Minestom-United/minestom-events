package dev.minestomunited.minestomevents;

import net.minestom.server.event.EventListener;

public record EventHandle(EventListener<?> listener) {

    public void unregister() {
        EventsNode.get().removeListener(listener);
    }
}
