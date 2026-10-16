package org.bitwisemadness.beasthorn.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.bitwisemadness.beasthorn.item.component.ContainedMobComponent;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "beasthorn");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ContainedMobComponent>> CONTAINED_MOB =
            DATA_COMPONENTS.registerComponentType("contained_mob", builder -> builder
                    .persistent(ContainedMobComponent.CODEC)
                    .networkSynchronized(ContainedMobComponent.STREAM_CODEC));

    public static void register() {
    }
}
