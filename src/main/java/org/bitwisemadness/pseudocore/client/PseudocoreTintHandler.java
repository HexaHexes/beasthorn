package org.bitwisemadness.pseudocore.client;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.bitwisemadness.pseudocore.item.component.ContainedMobComponent;
import org.bitwisemadness.pseudocore.registry.ModDataComponents;

import java.util.Map;

/**
 * Tints the three orbs in the Pseudocore texture to the captured mob's colour.
 *
 * <p>The model is a two-layer spawn-egg style split: {@code layer0} is only the
 * orb pixels in pure white (tintIndex 0) and {@code layer1} is the horn body
 * (tintIndex 1). Because {@code item/generated} assigns each layer a tint index
 * equal to its layer number, multiplying the white orbs by a colour yields that
 * colour exactly — no colour maths, no desaturation, so the orbs read vivid.
 * The body layer is tinted per tier; opaque white for the iron tier leaves it
 * untouched.</p>
 *
 * <p>The colour is the mob's spawn egg <b>primary</b> colour, resolved
 * client-side from the component's stored entity type. No extra NBT is stored,
 * so the tint can never drift out of sync with the mob inside.</p>
 */
public final class PseudocoreTintHandler {

    /** Fallback for mobs with no spawn egg: a bone-ish neutral. */
    private static final int FALLBACK = 0xD9C7A3;

    /**
     * The body tint of each tier, keyed by item id path. These are
     * <b>multipliers</b> on the body texture: the iron tier is opaque white,
     * which leaves the texture as drawn, and every other tier multiplies it by
     * its metal's colour, the way dyed armour works. Every value is opaque -
     * ItemRenderer reads the alpha raw (see the note in {@link #TINT}), so a
     * translucent value here would render the body invisible. Palette proposed
     * in the tinting issue and open to taste.
     */
    private static final Map<String, Integer> BODY_COLORS = Map.ofEntries(
            Map.entry("copper_pseudocore", 0xFFE88C5A),
            Map.entry("silver_pseudocore", 0xFFDDE4EA),
            Map.entry("gold_pseudocore", 0xFFF6D75C),
            Map.entry("netherite_pseudocore", 0xFF6A5F63),
            Map.entry("low_magisteel_pseudocore", 0xFF9FA8B0),
            Map.entry("high_magisteel_pseudocore", 0xFFC4CFD8),
            Map.entry("pure_magisteel_pseudocore", 0xFFE9F1F6),
            Map.entry("mithril_pseudocore", 0xFFC7D9EE),
            Map.entry("orichalcum_pseudocore", 0xFFE5A44E),
            Map.entry("adamantite_pseudocore", 0xFF59627A),
            Map.entry("hihiirokane_pseudocore", 0xFFE0513D));

    /** Opaque body tint for a tier; anything unmapped stays white (untouched). */
    private static int bodyColor(ItemStack stack) {
        return BODY_COLORS.getOrDefault(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), 0xFFFFFFFF);
    }

    private PseudocoreTintHandler() {
    }

    /**
     * Colour for tint index 0 (the orbs) vs index 1 (the horn body).
     *
     * <p>{@code item/generated} gives each layer a tint index equal to its layer
     * number, so layer0 = orbs = index 0 and layer1 = body = index 1.</p>
     */
    public static final ItemColor TINT = (stack, tintIndex) -> {
        // ItemRenderer reads the alpha straight off this int and hands it to the
        // vertex with no normalisation at all, so it must be opaque. A bare RGB
        // value like 0xFFFFFF has alpha 0, which renders the quad invisible -
        // that is exactly why the horn used to disappear completely when empty.
        // layer1 is the body, tinted per metal - see bodyColor.
        if (tintIndex == 1) {
            return bodyColor(stack);
        }
        ContainedMobComponent comp = stack.get(ModDataComponents.CONTAINED_MOB.get());
        int rgb = comp == null ? FALLBACK : resolvePrimaryColor(comp.entityType());
        return rgb | 0xFF000000;
    };

    /**
     * The mob's spawn egg primary colour. {@code SpawnEggItem.byId} is populated
     * in the SpawnEggItem constructor, so it resolves for deferred and modded
     * eggs (Tensura's included) by the time rendering happens.
     */
    static int resolvePrimaryColor(ResourceLocation entityTypeId) {
        if (entityTypeId == null) return FALLBACK;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
        if (type == null) return FALLBACK;

        SpawnEggItem egg = SpawnEggItem.byId(type);
        if (egg == null) {
            // No spawn egg: derive a stable hue from the id so the same mob
            // always tints the same, rather than being permanently colourless.
            return stableColorFrom(entityTypeId);
        }
        return egg.getColor(0); // 0 = primary
    }

    /**
     * Deterministic fallback colour. Uses the id's own hash so it is stable
     * across sessions and across clients, then forces high saturation and value
     * so an egg-less mob still reads as vivid as one with an egg. HSB is
     * converted by hand rather than via AWT, to keep the client free of an
     * unnecessary desktop-library dependency.
     */
    private static int stableColorFrom(ResourceLocation id) {
        float hue = Math.floorMod(id.hashCode(), 360) / 360.0F;
        return hsbToRgb(hue, 0.75F, 0.95F) & 0xFFFFFF;
    }

    private static int hsbToRgb(float h, float s, float v) {
        if (s == 0.0F) {
            int grey = (int) (v * 255.0F + 0.5F);
            return (grey << 16) | (grey << 8) | grey;
        }
        float scaled = h * 6.0F;
        int sector = (int) Math.floor(scaled);
        float frac = scaled - sector;
        float p = v * (1.0F - s);
        float q = v * (1.0F - s * frac);
        float t = v * (1.0F - s * (1.0F - frac));
        float r, g, b;
        switch (sector % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        int ri = (int) (r * 255.0F + 0.5F);
        int gi = (int) (g * 255.0F + 0.5F);
        int bi = (int) (b * 255.0F + 0.5F);
        return (ri << 16) | (gi << 8) | bi;
    }
}
