package org.bitwisemadness.pseudocore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.bitwisemadness.pseudocore.util.DragonTints;
import org.junit.jupiter.api.Test;

/**
 * Pins the dragon tint palette (#51): the three elements, all twelve IaF
 * variants, the fallbacks, and the mode flip. The handler itself cannot be
 * game-tested ({@code RegisterColorHandlersEvent} never fires on
 * {@code runGameTestServer}), so this pure-function suite is the CI guard.
 */
public class DragonTintsTest {

    /** The three element colours, as shipped. */
    @Test
    public void elementPaletteIsPinned() {
        assertEquals(0xDB4A22, DragonTints.color("fire", null, false));
        assertEquals(0x9CD8EA, DragonTints.color("ice", null, false));
        assertEquals(0xFFE14A, DragonTints.color("lightning", null, false));
    }

    /** Element mode ignores the variant entirely. */
    @Test
    public void elementModeIgnoresVariant() {
        assertEquals(DragonTints.color("fire", null, false),
                DragonTints.color("fire", "red", false));
        assertEquals(DragonTints.color("ice", null, false),
                DragonTints.color("ice", "black", false));
    }

    /** All twelve DragonColors, pinned to their shipped variant colour. */
    @Test
    public void variantPaletteIsPinned() {
        Map<String, Integer> expected = Map.ofEntries(
                Map.entry("fire:red", 0xEE4A3A),
                Map.entry("fire:green", 0x58C248),
                Map.entry("fire:bronze", 0xC9802E),
                Map.entry("fire:gray", 0xBFC3C7),
                Map.entry("ice:blue", 0x46D8E8),
                Map.entry("ice:white", 0xF4F8FC),
                Map.entry("ice:sapphire", 0x4A6FE0),
                Map.entry("ice:silver", 0xC7CED6),
                Map.entry("lightning:electric", 0x3D7EE8),
                Map.entry("lightning:amethyst", 0xD668E8),
                Map.entry("lightning:copper", 0xE8825A),
                Map.entry("lightning:black", 0x33363D));
        for (Map.Entry<String, Integer> e : expected.entrySet()) {
            String[] parts = e.getKey().split(":");
            assertEquals(e.getValue(), DragonTints.color(parts[0], parts[1], true),
                    e.getKey() + " must tint as pinned");
        }
    }

    /** Variant lookup is case-insensitive: IaF writes the raw name as-is. */
    @Test
    public void variantLookupIsCaseInsensitive() {
        assertEquals(DragonTints.color("fire", "red", true),
                DragonTints.color("fire", "Red", true));
        assertEquals(DragonTints.color("ice", "sapphire", true),
                DragonTints.color("ice", "SAPPHIRE", true));
    }

    /** A datapack-added or unknown variant falls back to its element colour. */
    @Test
    public void unknownVariantFallsBackToElement() {
        assertEquals(0xDB4A22, DragonTints.color("fire", "chartreuse", true));
        assertEquals(0x9CD8EA, DragonTints.color("ice", "", true));
    }

    /** No variant stored: the element colour, in either mode. */
    @Test
    public void absentVariantFallsBackToElement() {
        assertEquals(0xDB4A22, DragonTints.color("fire", null, true));
        assertEquals(0xFFE14A, DragonTints.color("lightning", null, true));
    }

    /** The three dragon ids resolve; nothing else does. */
    @Test
    public void elementOfMatchesOnlyDragonIds() {
        assertEquals("fire", DragonTints.elementOf("fire_dragon"));
        assertEquals("ice", DragonTints.elementOf("ice_dragon"));
        assertEquals("lightning", DragonTints.elementOf("lightning_dragon"));
        assertNull(DragonTints.elementOf("dragon"));
        assertNull(DragonTints.elementOf("sea_serpent"));
        assertNull(DragonTints.elementOf("zombie"));
        assertNull(DragonTints.elementOf(""));
    }

    /** A non-element reaching color() is a programming error, not a fallback. */
    @Test
    public void nonElementThrows() {
        assertThrows(IllegalArgumentException.class, () -> DragonTints.color("void", null, false));
        assertThrows(IllegalArgumentException.class, () -> DragonTints.color("", "red", true));
    }
}
