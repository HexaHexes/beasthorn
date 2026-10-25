package org.bitwisemadness.pseudocore;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;
import org.bitwisemadness.pseudocore.registry.ModConditions;
import org.bitwisemadness.pseudocore.registry.ModDataComponents;
import org.bitwisemadness.pseudocore.registry.ModGameRules;
import org.bitwisemadness.pseudocore.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Pseudocore.MOD_ID)
public final class Pseudocore {
    public static final String MOD_ID = "pseudocore";
    public static final Logger LOGGER = LoggerFactory.getLogger("Pseudocore");

    public Pseudocore(IEventBus modBus, ModContainer container) {
        ModItems.ITEMS.register(modBus);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModConditions.CONDITION_CODECS.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, PseudocoreConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.COMMON, PseudocoreConfig.INTEGRATIONS_SPEC);

        modBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // GameRules.register mutates a static map vanilla reads during game
        // construction, so it must run on the enqueueWork thread.
        event.enqueueWork(ModGameRules::register);
    }
}
