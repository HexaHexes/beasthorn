package org.bitwisemadness.beasthorn.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.bitwisemadness.beasthorn.BeastHorn;
import org.bitwisemadness.beasthorn.registry.ModItems;

/**
 * Client-only wiring. Registers the orb tint handler.
 *
 * <p>Split from the tint logic itself so no client-only class is ever loaded
 * on a dedicated server.</p>
 */
@EventBusSubscriber(modid = BeastHorn.MOD_ID, value = Dist.CLIENT)
public final class BeastHornClientEvents {

    private BeastHornClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(BeastHornTintHandler.TINT, ModItems.BEAST_HORN.get());
    }
}
