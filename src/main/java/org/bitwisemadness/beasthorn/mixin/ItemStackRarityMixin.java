package org.bitwisemadness.beasthorn.mixin;

import com.iafenvoy.iceandfire.item.DragonHornItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes Ice and Fire's Dragon Horn read as {@link Rarity#RARE}, so its name
 * renders aqua alongside the Beast Horn's yellow ({@link Rarity#UNCOMMON}).
 *
 * <p>Rarity is a data component in 1.21.1 — {@code ItemStack#getRarity} reads
 * {@code DataComponents.RARITY} off the stack — so there is no item-side hook
 * to override: {@code Item} has no rarity field or getter, and NeoForge's
 * {@code IItemExtension} exposes nothing for it either. IaF bakes its choice
 * into the Dragon Horn's default components at registration, so the only place
 * to intervene is the read.</p>
 *
 * <p>Redirecting the lookup rather than cancelling at the method head keeps
 * vanilla's "enchanted items bump up one tier" step intact: {@code RARE} is fed
 * into that same code path instead of replacing it, so an enchanted Dragon Horn
 * still promotes to epic. Every other item falls through to the original call
 * untouched.</p>
 *
 * <p>Ships in the IaF-gated mixin config, since the Dragon Horn does not exist
 * without Ice and Fire CE.</p>
 */
@Mixin(value = ItemStack.class, remap = true)
public abstract class ItemStackRarityMixin {

    @Redirect(
            method = "getRarity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private Object beasthorn$rareForDragonHorn(ItemStack stack, DataComponentType<?> type, Object fallback) {
        if (stack.getItem() instanceof DragonHornItem) {
            return Rarity.RARE;
        }
        return stack.getOrDefault(type, fallback);
    }
}
