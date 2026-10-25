package org.bitwisemadness.pseudocore.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;

/**
 * Proves the integration switches reach the <b>data packs</b>, not just the code.
 *
 * <p>Two probe recipes ship in this source set only - harmless shapeless recipes
 * that turn a stick into a stick - each gated behind one integration condition.
 * Whether each one survives data pack loading is asserted against the same answer
 * the code side gives ({@code ModList} plus the common-config switch), so the test
 * is meaningful whether or not Tensura and Ice and Fire happen to be installed in
 * the run: with them installed the recipe must be present, with them absent or
 * switched off it must have been dropped.</p>
 *
 * <p>That equality is the whole point of registering the conditions in the first
 * place. A recipe silently surviving a disabled integration, or silently
 * vanishing while it is enabled, is exactly the failure this catches - and it is
 * invisible in any single test run, because neither the loader nor the mod logs
 * anything when a condition decides.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IntegrationConditionGameTests {

    private static final String TENSURA = "tensura";
    private static final String ICE_AND_FIRE = "iceandfire";

    private IntegrationConditionGameTests() {
    }

    @GameTest(template = "test_area")
    public static void tensuraConditionGatesItsDataFile(GameTestHelper helper) {
        checkRegistered("tensura_active");

        assertProbeMatches(
                helper,
                "integration_condition_probe_tensura",
                ModList.get().isLoaded(TENSURA) && PseudocoreConfig.tensuraIntegrationEnabled(),
                "Tensura");

        helper.succeed();
    }

    @GameTest(template = "test_area")
    public static void iceAndFireConditionGatesItsDataFile(GameTestHelper helper) {
        checkRegistered("iceandfire_active");

        assertProbeMatches(
                helper,
                "integration_condition_probe_iceandfire",
                ModList.get().isLoaded(ICE_AND_FIRE) && PseudocoreConfig.iceAndFireIntegrationEnabled(),
                "Ice and Fire CE");

        helper.succeed();
    }

    /**
     * The condition id must exist in the condition registry at all - otherwise
     * every data file using it fails to load, which reads as "the mod's recipes
     * are broken" rather than as a missing registration.
     */
    private static void checkRegistered(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, path);
        check(NeoForgeRegistries.CONDITION_SERIALIZERS.containsKey(id),
                "no condition is registered as " + id
                        + ", so every data file gated on it would fail to load");
    }

    private static void assertProbeMatches(
            GameTestHelper helper, String recipePath, boolean expectedPresent, String modName) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, recipePath);

        boolean present = helper.getLevel().getServer().getRecipeManager().byKey(recipeId).isPresent();

        check(present == expectedPresent,
                "the probe recipe " + recipeId + " is " + (present ? "loaded" : "missing")
                        + " while " + modName + " integration is "
                        + (expectedPresent ? "active" : "inactive")
                        + " - the integration switch has to add and remove data files, not"
                        + " merely grey them out in code");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
