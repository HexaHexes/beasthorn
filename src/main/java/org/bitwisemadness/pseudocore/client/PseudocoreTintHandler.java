package org.bitwisemadness.pseudocore.client;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;
import org.bitwisemadness.pseudocore.item.component.ContainedMobComponent;
import org.bitwisemadness.pseudocore.registry.ModDataComponents;
import org.bitwisemadness.pseudocore.util.DragonTints;
import org.bitwisemadness.pseudocore.util.IafHornAccess;
import org.bitwisemadness.pseudocore.util.VariantHelper;

import java.util.Map;
import java.util.UUID;

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
 * client-side from the component's stored entity type. Dragons are the
 * exception (#51): their orbs tint by element, or by the dragon's own colour
 * when {@code dragonTintMode} says so - see {@link DragonTints}. No extra NBT
 * is stored, so the tint can never drift out of sync with the mob inside.</p>
 *
 * <p>Empty horns are the exception on the other side (#53): with no contained
 * mob the orb layer is multiplied by plain white, so the dull empty-state art
 * renders exactly as drawn - tinting it would double-dim and muddy it. The
 * model itself also swaps to the empty art via the {@code pseudocore:filled}
 * predicate, so both halves of the state agree.</p>
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
     * translucent value here would render the body invisible.
     *
     * <p>Provenance differs by tier (#71). The Tensura metals are sampled from
     * Tensura's own {@code <metal>_ingot.png} textures: the mean of the opaque
     * pixels that carry hue - luminance between 40 and 235, excluding the dark
     * outline and the white highlight - which is the colour a glance reads as
     * that metal. So orichalcum is the ingot's yellow-green rather than an
     * orange, adamantite its dark green rather than a blue-grey, and
     * hihiirokane its pale silver-cyan rather than a red. The vanilla tiers
     * keep their curated values - they were not part of the mismatch - and
     * iron stays opaque white, which is the deliberate baseline that renders
     * the body art exactly as drawn.</p>
     */
    private static final Map<String, Integer> BODY_COLORS = Map.ofEntries(
            Map.entry("copper_pseudocore", 0xFFE88C5A),
            Map.entry("silver_pseudocore", 0xFF969DA4),
            Map.entry("gold_pseudocore", 0xFFF6D75C),
            Map.entry("netherite_pseudocore", 0xFF6A5F63),
            Map.entry("low_magisteel_pseudocore", 0xFFB1B3B1),
            Map.entry("high_magisteel_pseudocore", 0xFFB7BDB9),
            Map.entry("pure_magisteel_pseudocore", 0xFFB1BBC5),
            Map.entry("mithril_pseudocore", 0xFFA5B5E0),
            Map.entry("orichalcum_pseudocore", 0xFFB8C935),
            Map.entry("adamantite_pseudocore", 0xFF426F59),
            Map.entry("hihiirokane_pseudocore", 0xFF9397AE),
            Map.entry("copper_dragon_pseudocore", 0xFFE88C5A),
            Map.entry("iron_dragon_pseudocore", 0xFFFFFFFF),
            Map.entry("silver_dragon_pseudocore", 0xFF969DA4),
            Map.entry("gold_dragon_pseudocore", 0xFFF6D75C),
            Map.entry("netherite_dragon_pseudocore", 0xFF6A5F63));

    /** Opaque body tint for a tier; anything unmapped stays white (untouched). */
    private static int bodyColor(ItemStack stack) {
        return BODY_COLORS.getOrDefault(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), 0xFFFFFFFF);
    }

    /**
     * Curated orb colours for the variants whose names are literally colours:
     * Tensura's slime dyes and the direwolf fangs. Keyed by item path plus the
     * lowercased variant text; every other variant derives a stable colour from
     * its name instead.
     */
    private static final Map<String, Integer> CURATED_ORB_COLORS = Map.ofEntries(
            Map.entry("slime:red", 0xFFB02E26),
            Map.entry("slime:orange", 0xFFF9801D),
            Map.entry("slime:yellow", 0xFFFED83D),
            Map.entry("slime:lime", 0xFF80C71F),
            Map.entry("slime:green", 0xFF5E7C16),
            Map.entry("slime:cyan", 0xFF169C9C),
            Map.entry("slime:light blue", 0xFF3AB3DA),
            Map.entry("slime:blue", 0xFF3C44AA),
            Map.entry("slime:purple", 0xFF8932B8),
            Map.entry("slime:magenta", 0xFFC74EBD),
            Map.entry("slime:pink", 0xFFF38BAA),
            Map.entry("slime:brown", 0xFF835432),
            Map.entry("slime:gray", 0xFF474F52),
            Map.entry("slime:light gray", 0xFF9D9D97),
            Map.entry("slime:white", 0xFFF9FFFE),
            Map.entry("direwolf:red fang", 0xFFB02E26),
            Map.entry("direwolf:green fang", 0xFF5E7C16),
            Map.entry("direwolf:blue fang", 0xFF3C44AA),
            Map.entry("direwolf:brown fang", 0xFF835432),
            Map.entry("direwolf:purple fang", 0xFF8932B8),
            Map.entry("direwolf:black fang", 0xFF1D1D21));

    /**
     * The orb colour of a stored mob, resolved in order: dragons first (#51 -
     * tinted by element or curated variant colour, per {@code dragonTintMode},
     * never by the generic hash below); then a curated colour when the mob's
     * variant is one whose name is literally a colour; otherwise a stable
     * colour derived from the variant name itself, so two variants of one
     * species read differently while staying deterministic across sessions and
     * clients; and for mobs with no readable variant, the species' spawn-egg
     * primary, with the stable id-derived colour as the last resort.
     */
    private static int orbColor(ContainedMobComponent comp) {
        ResourceLocation type = comp.entityType();

        // Dragons: the stored NBT always carries Variant (#51's evidence), but
        // the raw string-typed read (not VariantHelper#variantOf, which
        // localizes) is what maps onto IaF's DragonColor names. A missing or
        // non-string Variant simply falls through to the element colour.
        String dragonElement = DragonTints.elementOf(type.getPath());
        if (dragonElement != null) {
            return DragonTints.color(dragonElement,
                    VariantHelper.stringVariant(comp.entityData()),
                    PseudocoreConfig.dragonTintVariantMode());
        }

        VariantHelper.Variant variant = VariantHelper.variantOf(type, comp.entityData());
        if (variant != null) {
            String key = type.getPath() + ":" + variant.text().toLowerCase(java.util.Locale.ROOT);
            Integer curated = CURATED_ORB_COLORS.get(key);
            return curated != null ? curated : stableColorFrom(key) | 0xFF000000;
        }

        return resolvePrimaryColor(type);
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
        if (comp != null) {
            return orbColor(comp) | 0xFF000000;
        }
        // A dragon core stores its capture in Ice and Fire's horn component
        // rather than in CONTAINED_MOB (#65), so without this read every dragon
        // tinted white and the element/variant modes below were unreachable. The
        // raw pieces are shaped into the same record the beast line already
        // produces, so orbColor needs no second branch.
        ResourceLocation dragonType = IafHornAccess.storedDragonType(stack);
        if (dragonType != null) {
            CompoundTag dragonData = IafHornAccess.storedDragonData(stack);
            if (dragonData == null) {
                dragonData = new CompoundTag();
            }
            // The UUID is unused by orbColor - the colour is derived from the
            // element and the stored Variant string - and IaF's component
            // carries one, so it is threaded through rather than invented.
            UUID dragonUuid = IafHornAccess.storedDragonUuid(stack);
            return orbColor(new ContainedMobComponent(dragonType,
                    dragonUuid == null ? new UUID(0L, 0L) : dragonUuid, dragonData)) | 0xFF000000;
        }
        // An empty horn is not tinted (#53): the dull empty-state art must
        // render exactly as drawn - multiplying it by a colour would double-dim
        // and muddy it. Filled horns keep the tint paths above untouched.
        return 0xFFFFFFFF;
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

    private static int stableColorFrom(ResourceLocation id) {
        return stableColorFrom(id.toString());
    }

    /**
     * Deterministic fallback colour. Uses the key's own hash so it is stable
     * across sessions and across clients, then forces high saturation and value
     * so an egg-less mob still reads as vivid as one with an egg. HSB is
     * converted by hand rather than via AWT, to keep the client free of an
     * unnecessary desktop-library dependency.
     */
    private static int stableColorFrom(String key) {
        float hue = Math.floorMod(key.hashCode(), 360) / 360.0F;
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
