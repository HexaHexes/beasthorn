package org.bitwisemadness.pseudocore.event;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.item.PseudocoreItem;
import org.bitwisemadness.pseudocore.registry.ModItems;

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
 * <p>The dragon line rides the same cure (#37): a tamed Ice and Fire dragon is
 * the most rideable mob in the pack, so its own {@code interact()} — mounting —
 * swallowed every dragon pseudocore click before the tier gates could run, which
 * is why captures did nothing and no refusal message ever appeared. Dragon
 * capture is dispatched from here as well, and a refusal still passes the click
 * back: a dragon a tier declined can be ridden as if the item were not held.</p>
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

        if (event.getItemStack().getItem() instanceof PseudocoreItem) {
            InteractionResult result = PseudocoreItem.tryCapture(event.getEntity(), target, event.getItemStack());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }

        // Dragon pseudocores: the same trap, the same cure. The membership
        // check goes through ModItems rather than an instanceof of the item
        // class: this class must stay loadable without Ice and Fire, and an
        // instanceof here would load the item class - and through it IaF's -
        // on first use. The list is empty without IaF, and its entries only
        // exist with it, so dispatching through the Item interface can only
        // ever reach the dragon line when it is safe to load.
        if (isDragonPseudocore(event.getItemStack())) {
            InteractionResult result = event.getItemStack().getItem()
                    .interactLivingEntity(event.getItemStack(), event.getEntity(), target, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        }
    }

    private static boolean isDragonPseudocore(net.minecraft.world.item.ItemStack stack) {
        return !ModItems.DRAGON_TIERS.isEmpty()
                && ModItems.DRAGON_TIERS.stream().anyMatch(tier -> tier.get() == stack.getItem());
    }
}
