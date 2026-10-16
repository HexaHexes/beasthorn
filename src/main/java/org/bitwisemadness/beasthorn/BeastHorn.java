package org.bitwisemadness.beasthorn;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.bitwisemadness.beasthorn.config.BeastHornConfig;
import org.bitwisemadness.beasthorn.registry.ModDataComponents;
import org.bitwisemadness.beasthorn.registry.ModGameRules;
import org.bitwisemadness.beasthorn.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BeastHorn.MOD_ID)
public final class BeastHorn {
    public static final String MOD_ID = "beasthorn";
    public static final Logger LOGGER = LoggerFactory.getLogger("Beast Horn");

    public BeastHorn(IEventBus modBus, ModContainer container) {
        ModItems.ITEMS.register(modBus);
        ModDataComponents.DATA_COMPONENTS.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, BeastHornConfig.COMMON_SPEC);

        modBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // GameRules.register mutates a static map vanilla reads during game
        // construction, so it must run on the enqueueWork thread.
        event.enqueueWork(ModGameRules::register);
    }
}
