# minestom-events

A Gradle plugin that generates a typed event API for [Minestom](https://minestom.net). One named method per event — no manual `EventNode` wiring.

```java
Events.onPlayerBlockBreak(event ->
    event.getPlayer().sendMessage(Component.text("Broke " + event.getBlock().name())));
```

At build time the plugin scans your classpath and sources, then generates an `Events` class with a method for every Minestom event (and your own).

## Setup

Requires Java 25.

`build.gradle.kts`:

```kotlin
plugins {
    java
    id("dev.minestomunited.minestom-events")  version "<version>"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2026.05.11-1.21.11")
    implementation("dev.minestomunited:minestom-events-core:<version>")
}

minestomEvents {
    outputPackage.set("com.example.generated")
}
```

The plugin resolves from the Gradle Plugin Portal and the core library from Maven Central — no extra repositories needed.

## Usage

Init the node once, then register handlers:

```java
import com.example.generated.Events;
import dev.minestomunited.minestomevents.EventsNode;

EventsNode.init(MinecraftServer.getGlobalEventHandler());

Events.onPlayerDisconnect(event ->
    System.out.println(event.getPlayer().getUsername() + " left"));
```

Filter to a player, instance, entity, or inventory:

```java
Events.onPlayerBlockBreak(EventFilter.instance(instance), event -> ...);
```

Your own events get methods too — anything that `implements Event`:

```java
public class TestEvent implements Event {}

Events.onTest(event -> System.out.println("fired"));
```

You can unregister the handler by calling

```java
EventHandler handler = Events.onPlayerBlockBreak(EventFilter.instance(instance), event -> ...);
handler.unregister();
```

## Config

```kotlin
minestomEvents {
    outputPackage.set("com.example.generated")   // package for generated Events class
    scanPackages.set(listOf("net.minestom"))      // limit scan; empty = all
}
```