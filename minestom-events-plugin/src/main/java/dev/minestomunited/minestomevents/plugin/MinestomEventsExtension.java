package dev.minestomunited.minestomevents.plugin;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class MinestomEventsExtension {

    public abstract Property<Boolean> getCompileOnly();

    public abstract Property<String> getOutputPackage();

    /**
     * Restrict scanning to these packages (and their subpackages). Empty = scan all.
     */
    public abstract ListProperty<String> getScanPackages();
}
