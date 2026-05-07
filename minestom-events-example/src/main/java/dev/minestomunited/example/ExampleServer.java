package dev.minestomunited.example;

import dev.minestomunited.example.generated.Events;
import dev.minestomunited.minestomevents.EventFilter;
import dev.minestomunited.minestomevents.EventsNode;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;

public class ExampleServer {

    static void main() {
        MinecraftServer server = MinecraftServer.init();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer world = instanceManager.createInstanceContainer();
        world.setGenerator(unit -> unit.modifier().fillHeight(0, 1, Block.GRASS_BLOCK));

        EventsNode.init(MinecraftServer.getGlobalEventHandler());

        // Assign spawning instance before the player enters the world
        Events.onAsyncPlayerConfiguration(event -> {
            event.setSpawningInstance(world);
            event.getPlayer().setRespawnPoint(new Pos(0.5, 1, 0.5));
            EventDispatcher.call(new TestEvent());
        });

        Events.onPlayerDisconnect(event ->
            System.out.println(event.getPlayer().getUsername() + " disconnected"));

        Events.onTest(_ ->
            System.out.println("TestEvent fired"));

        // Scoped — fires only for block breaks in this instance
        Events.onPlayerBlockBreak(EventFilter.instance(world), event ->
            event.getPlayer().sendMessage(
                Component.text("Broke " + event.getBlock().name())));

        server.start("0.0.0.0", 25565);
    }
}
