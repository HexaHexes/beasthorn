package org.bitwisemadness.pseudocore.util;

import java.util.Map;

/**
 * Orb colours for captured Ice and Fire dragons (#51).
 *
 * <p>Before this class a stored dragon's orb colour was a hash of its
 * variant's <b>localized</b> text, so a fire dragon tinted lime green and an
 * ice dragon mud brown. A dragon's colour is two facts we already have: its
 * element (the entity type) and, when present, its {@code Variant} - one of
 * the twelve {@code DragonColor}s IaF registers, each carrying an official
 * {@code ChatFormatting} hue that IaF itself uses to colour dragon egg, scale
 * and scale-armor names.</p>
 *
 * <p>Two modes, chosen by the {@code dragonTintMode} client config: element
 * mode tints every dragon of one element alike; variant mode uses the curated
 * per-variant hue and falls back to the element colour for a variant it does
 * not know (the DragonColor registry is datapack-driven, so names are
 * open-ended).</p>
 *
 * <p>Deliberately imports <b>no</b> foreign type at all, mirroring the
 * shared library's {@code VariantHelper} and this package's
 * {@link DragonStages}: the twelve IaF colours are
 * recorded here as data, so this class is pure and junit-testable, and the
 * mod compiles and runs with Ice and Fire absent.</p>
 *
 * <p>IaF source values (the {@code ChatFormatting} each DragonColor carries):
 * FIRE red - DARK_RED, green - DARK_GREEN, bronze - GOLD, gray - GRAY;
 * ICE blue - AQUA, white - WHITE, sapphire - BLUE, silver - DARK_GRAY;
 * LIGHTNING electric - DARK_BLUE, amethyst - LIGHT_PURPLE, copper - GOLD,
 * black - DARK_GRAY. The palette below lifts each hue's brightness and
 * saturation so white orb pixels read vivid (as the rest of the tint palette
 * does), which also splits IaF's own collisions - bronze vs copper (both
 * GOLD upstream) and silver vs black (both DARK_GRAY upstream).</p>
 */
public final class DragonTints {

    /** Element mode: every dragon of one element tints alike. */
    public static final String MODE_ELEMENT = "element";
    /** Variant mode: the twelve DragonColor hues, element as fallback. */
    public static final String MODE_VARIANT = "variant";

    /**
     * The element palette: one vivid colour per dragon type, chosen to read
     * instantly (fire hot orange-red, ice pale glacier, lightning charged
     * yellow) rather than to match any single in-game shade - a fire dragon
     * can legitimately be green or gray, so the element colour is a symbol,
     * not a sample.
     */
    private static final Map<String, Integer> ELEMENT_COLORS = Map.of(
            "fire", 0xDB4A22,
            "ice", 0x9CD8EA,
            "lightning", 0xFFE14A);

    /**
     * The twelve variant colours, keyed by the raw stored {@code Variant}
     * string lowercased (IaF writes {@code DragonColor#getName()}). The IaF
     * source hue is noted per entry; see the class javadoc for the lifting
     * rule and the two upstream collisions this splits.
     */
    private static final Map<String, Integer> VARIANT_COLORS = Map.ofEntries(
            // FIRE (DARK_RED, DARK_GREEN, GOLD, GRAY)
            Map.entry("red", 0xEE4A3A),
            Map.entry("green", 0x58C248),
            Map.entry("bronze", 0xC9802E),
            Map.entry("gray", 0xBFC3C7),
            // ICE (AQUA, WHITE, BLUE, DARK_GRAY)
            Map.entry("blue", 0x46D8E8),
            Map.entry("white", 0xF4F8FC),
            Map.entry("sapphire", 0x4A6FE0),
            Map.entry("silver", 0xC7CED6),
            // LIGHTNING (DARK_BLUE, LIGHT_PURPLE, GOLD, DARK_GRAY)
            Map.entry("electric", 0x3D7EE8),
            Map.entry("amethyst", 0xD668E8),
            Map.entry("copper", 0xE8825A),
            Map.entry("black", 0x33363D));

    private DragonTints() {
    }

    /**
     * The dragon element of an entity type id path, or {@code null} when the
     * id is not one of Ice and Fire's three dragons. Matched on the path
     * alone, exactly as the shared {@code VariantHelper} keys its dragon
     * handling, so a renamed namespace does not silently disable the tint.
     */
    public static String elementOf(String entityPath) {
        return switch (entityPath) {
            case "fire_dragon" -> "fire";
            case "ice_dragon" -> "ice";
            case "lightning_dragon" -> "lightning";
            default -> null;
        };
    }

    /**
     * The orb colour for a dragon, as opaque RGB.
     *
     * @param element    one of {@code fire}, {@code ice}, {@code lightning}
     * @param rawVariant the stored {@code Variant} string, or null when the
     *                   dragon carries none (or it is not a string)
     * @param variantMode true for {@link #MODE_VARIANT}, false for
     *                   {@link #MODE_ELEMENT}
     */
    public static int color(String element, String rawVariant, boolean variantMode) {
        Integer elementColor = ELEMENT_COLORS.get(element);
        if (elementColor == null) {
            throw new IllegalArgumentException("not a dragon element: " + element);
        }
        if (!variantMode || rawVariant == null) {
            return elementColor;
        }
        return VARIANT_COLORS.getOrDefault(rawVariant.toLowerCase(java.util.Locale.ROOT), elementColor);
    }
}
