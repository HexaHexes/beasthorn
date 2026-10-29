package org.bitwisemadness.pseudocore.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.util.OrbColors;

/**
 * The orb-colour API (#123): {@link OrbColors#orbColorFor} answers from the
 * same ladder the live item tint delegates to - equality is structural (one
 * method, two callers), so what the tests pin are the ladder's <b>values</b>
 * and its determinism.
 *
 * <p>The original parity assertion against the {@code TINT} lambda is
 * deliberately absent: NeoForge's dist cleaner strips the client-only
 * {@code ItemColor} class on a dedicated server, which is exactly why the
 * ladder moved out of the tint handler (#123) - the gametest failure that
 * proved it is recorded on that issue. Client-side rendering is verified in
 * game, as the addon's rendering tests note too.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TintGameTests {

    private TintGameTests() {
    }

    /**
     * The fallback ladder's golden value: a variantless common mob answers
     * with its spawn-egg primary, alpha-normalised on both sides - the same
     * source {@code resolvePrimaryColor} reads, asserted through the public
     * API only.
     */
    @GameTest(template = "test_area")
    public static void orbColorForFallsBackToTheSpawnEggPrimary(GameTestHelper helper) {
        ResourceLocation cowId = ResourceLocation.withDefaultNamespace("cow");
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(cowId).orElse(null);
        check(type != null, "the cow must exist");
        SpawnEggItem egg = SpawnEggItem.byId(type);
        check(egg != null, "the cow must have a spawn egg");

        int api = OrbColors.orbColorFor(cowId, new CompoundTag());
        int expected = egg.getColor(0);
        check((api | 0xFF000000) == (expected | 0xFF000000),
                "a variantless mob must tint to its spawn-egg primary: api="
                        + Integer.toHexString(api) + " egg=" + Integer.toHexString(expected));

        // Null data is the API's documented edge: same answer as empty.
        check(OrbColors.orbColorFor(cowId, null) == api,
                "null entity data must answer as empty data");

        helper.succeed();
    }

    /**
     * The variant ladder through the public door: same inputs, same colour,
     * across two calls - the property the hash-derived branch promises (and
     * what makes a dead core's tint stable across sessions and clients).
     * The mob is a Tensura species with variant NBT, present-or-skip like
     * every other Tensura-dependent probe.
     */
    @GameTest(template = "test_area")
    public static void orbColorForIsDeterministicForVariantCarryingMobs(GameTestHelper helper) {
        ResourceLocation direwolfId = ResourceLocation.fromNamespaceAndPath("tensura", "direwolf");
        if (BuiltInRegistries.ENTITY_TYPE.getOptional(direwolfId).isEmpty()) {
            helper.succeed();
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putInt("Variant", 3);

        int first = OrbColors.orbColorFor(direwolfId, data);
        int second = OrbColors.orbColorFor(direwolfId, data);
        check(first == second,
                "the same stored mob must always tint the same: "
                        + Integer.toHexString(first) + " vs " + Integer.toHexString(second));

        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
