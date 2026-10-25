package org.bitwisemadness.pseudocore.mixin;

import com.iafenvoy.iceandfire.item.component.DragonHornComponent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.bitwisemadness.pseudocore.registry.ModGameRules;
import org.bitwisemadness.pseudocore.util.IafHornAccess;
import org.bitwisemadness.pseudocore.util.TooltipHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Two changes to Ice and Fire CE's own Dragon Horn.
 *
 * <p>The mixin only targets IaF CE's Dragon Horn — the Pseudocore is entirely
 * separate and does not depend on it. The mixin config it ships in is declared
 * with {@code requiredMods=["iceandfire"]} and additionally guarded by
 * {@link org.bitwisemadness.pseudocore.mixin.IafMixinPlugin}, so it is skipped
 * entirely when IaF CE is not installed.</p>
 *
 * <p><b>Ownership on release</b> — injected at the single {@code
 * Level#addFreshEntity} call inside {@code useOn}, so a released dragon joins
 * the world already owned by the releasee.</p>
 *
 * <p><b>Tooltip</b> — {@code appendHoverText} is cancelled at its head and
 * rewritten, so the Dragon Horn shows the same labelled readout as the
 * Pseudocore (Name, Race, Owner, Health, EP) followed by IaF's own Gender and
 * Stage/Days lines. Replacing rather than appending is what lets us control the
 * ordering; the cost is coupling to IaF's lang keys and AgeTicks bands, both
 * handled in {@link TooltipHelper}.</p>
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
    private void pseudocore$transferDragonOwnership(
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

    /**
     * Replace IaF's tooltip with the shared labelled readout plus IaF's own
     * gender and stage lines.
     *
     * <p>Two of IaF's three states are taken over rather than replicated. With
     * no stored dragon we emit the Pseudocore's empty pair of lines, because
     * IaF emits nothing at all there. With one, we emit our labelled block plus
     * IaF's own gender and stage lines. Only when the entity type no longer
     * resolves — a data pack removed the mob — do we stand aside and let IaF's
     * method run, since our block cannot render a race for a type that does not
     * exist.</p>
     *
     * <p>IaF's {@code super.appendHoverText} is skipped along with the rest, but
     * that is a no-op: {@code Item#appendHoverText} has an empty body.</p>
     */
    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void pseudocore$replaceTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag,
            CallbackInfo cir) {

        DragonHornComponent horn = IafHornAccess.read(stack);
        if (horn == null) {
            // Empty horn. IaF emits nothing here, so take it over rather than
            // standing aside - which is what left the Dragon Horn as the only
            // item in the pack with a blank tooltip.
            TooltipHelper.addEmptyDragonLines(tooltip);
            cir.cancel();
            return;
        }

        CompoundTag data = horn.entityData();
        if (data == null || data.isEmpty()) {
            return;
        }

        ResourceLocation typeId = horn.entityType();
        if (typeId == null || BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).isEmpty()) {
            return;
        }

        // Our block first - identical to the Pseudocore's - then IaF's own two
        // lines, so both horns read the same at a glance.
        TooltipHelper.addLines(tooltip, typeId, data, context.registries());
        TooltipHelper.addDragonLines(tooltip, data);

        cir.cancel();
    }
}
