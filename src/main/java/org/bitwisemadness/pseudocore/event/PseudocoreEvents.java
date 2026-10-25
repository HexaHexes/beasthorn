package org.bitwisemadness.pseudocore.event;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.item.PseudocoreItem;

/**
 * Wiring for capture.
 *
 * <p>Capture hangs off {@link PlayerInteractEvent.EntityInteract} rather than
 * {@code Item.interactLivingEntity} for a specific reason: NeoForge's
 * {@code Player.interactOn} asks {@code entity.interact()} <b>first</b> and only
 * consults the held item when that returns {@code PASS}. Any mob whose own
 * interaction consumes the click — a mount like Tensura's Direwolf, a tameable
 * animal, anything with a container — wins, and the item method is never called
 * at all. {@code CommonHooks.onInteractEntity} posts this event before that
 * decision, so cancelling it here captures the mob and stops the mount from
 * happening in the same click.</p>
 *
 * <p>The event fires on both sides, which is what lets the capture guards
 * predict the client's outcome to match the server's. The bus is not spelled out
 * on the annotation: {@code AutomaticEventSubscriber} picks the game bus or the
 * mod bus per listener from the event type, and {@code PlayerInteractEvent} is a
 * plain {@code Event}, so it lands on the game bus.</p>
 */
@EventBusSubscriber(modid = Pseudocore.MOD_ID)
public final class PseudocoreEvents {

    private PseudocoreEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof PseudocoreItem)) {
            return;
        }

        InteractionResult result = PseudocoreItem.tryCapture(event.getEntity(), target, event.getItemStack());
        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
