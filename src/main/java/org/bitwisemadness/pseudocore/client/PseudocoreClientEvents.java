package org.bitwisemadness.pseudocore.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.registry.ModItems;

/**
 * Client-only wiring. Registers the orb tint handler.
 *
 * <p>Split from the tint logic itself so no client-only class is ever loaded
 * on a dedicated server.</p>
 *
 * <p>The bus is not spelled out: {@code AutomaticEventSubscriber} routes each
 * listener from its event type, and {@code RegisterColorHandlersEvent} is an
 * {@code IModBusEvent}, so it is registered on the mod bus automatically. (The
 * annotation's own {@code bus} attribute is deprecated for removal precisely
 * because this detection replaced it.)</p>
 */
@EventBusSubscriber(modid = Pseudocore.MOD_ID, value = Dist.CLIENT)
public final class PseudocoreClientEvents {

    private PseudocoreClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(PseudocoreTintHandler.TINT, ModItems.IRON_PSEUDOCORE.get());
    }
}
