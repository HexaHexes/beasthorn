package org.bitwisemadness.beasthorn.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.bitwisemadness.beasthorn.registry.ModGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Ice and Fire CE's own Dragon Horn transfer dragon ownership to whoever
 * releases it, gated behind the {@code beastHornDragonOwnershipTransfer}
 * gamerule (default off).
 *
 * <p>This mixin only targets IaF CE's Dragon Horn — the Beast Horn is entirely
 * separate and does not depend on it. The mixin config it ships in is declared
 * with {@code requiredMods=["iceandfire"]} and additionally guarded by
 * {@link org.bitwisemadness.beasthorn.mixin.IafMixinPlugin}, so it is skipped
 * entirely when IaF CE is not installed.</p>
 *
 * <p>Injection point: the single {@code Level#addFreshEntity} call inside
 * {@code DragonHornItem#useOn}. Verified against IaF CE 2.1.4 bytecode — the
 * dragon is in local slot 6 at that point, and it is the only addFreshEntity in
 * the method. Injecting just before it means the dragon joins the world already
 * owned by the releasee, so the first client sync carries the right owner.</p>
 */
@Mixin(value = com.iafenvoy.iceandfire.item.DragonHornItem.class, remap = true)
public abstract class DragonHornItemMixin {

    @Inject(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private void beasthorn$transferDragonOwnership(
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir,
            @Local Entity entity) {

        // Deliberately checked as vanilla TamableAnimal rather than IaF's
        // DragonBaseEntity. The three methods used below (isOwnedBy, setTame,
        // tame) all live on TamableAnimal, so narrowing to it keeps this mixin
        // free of any dependency on IaF's bundled 'uranus' library - which is
        // jar-in-jar inside the IaF jar and not otherwise visible here. Every
        // entity the Dragon Horn can hold is a tamed dragon, so this is also
        // not a weaker check in practice.
        if (!(entity instanceof TamableAnimal dragon)) {
            return;
        }

        Player player = context.getPlayer();
        if (player == null) {
            // Released by a dispenser or similar - no player to hand ownership to.
            return;
        }

        Level level = context.getLevel();
        if (!ModGameRules.isDragonHornTransferEnabled(level)) {
            return;
        }

        // Already the owner - nothing to transfer.
        if (dragon.isOwnedBy(player)) {
            return;
        }

        // Reuse IaF/vanilla's own taming path rather than assigning the owner
        // UUID by hand. setTame(true, true) sets the tamed flag and syncs it;
        // tame(player) then assigns the owner and fires the tame feedback.
        // This mirrors what IaF's CREATIVE_DRAGON_MEAL item does, so it is the
        // mod's own proven "make this player the owner" sequence.
        dragon.setTame(true, true);
        dragon.tame(player);
    }
}
