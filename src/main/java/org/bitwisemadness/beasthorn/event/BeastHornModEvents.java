package org.bitwisemadness.beasthorn.event;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.bitwisemadness.beasthorn.BeastHorn;
import org.bitwisemadness.beasthorn.registry.ModItems;

/**
 * Mod-bus wiring that is not client-specific.
 *
 * <p>Split from {@code BeastHornClientEvents} because the creative tab event is
 * not dist-restricted, and because a class holding a client-only handler
 * signature must never be loaded on a dedicated server.</p>
 *
 * <p>No explicit bus on the annotation: {@code AutomaticEventSubscriber} routes
 * per listener from the event type, and {@code BuildCreativeModeTabContentsEvent}
 * is an {@code IModBusEvent}, so it lands on the mod bus on its own.</p>
 */
@EventBusSubscriber(modid = BeastHorn.MOD_ID)
public final class BeastHornModEvents {

    private BeastHornModEvents() {
    }

    /**
     * Put the horn in Tools &amp; Utilities.
     *
     * <p>NeoForge does not place a freshly registered item into any creative tab
     * on its own — it has no default tab — so without this the horn exists but is
     * unreachable except through the search tab or {@code /give}. Tools &amp;
     * Utilities is where vanilla puts the other single-purpose tools.</p>
     */
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.TOOLS_AND_UTILITIES.equals(event.getTabKey())) {
            event.accept(ModItems.BEAST_HORN.get());
        }
    }
}
