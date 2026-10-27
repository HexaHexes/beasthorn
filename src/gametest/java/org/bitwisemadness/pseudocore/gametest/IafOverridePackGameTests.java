package org.bitwisemadness.pseudocore.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;

/**
 * The override pack (#21, part 2): while the Ice and Fire integration is
 * active, IaF's Dragon Horn recipe is gone and both its dragon-horn advancement
 * ids load as Pseudocore's reworks, not IaF's originals.
 *
 * <p>"Ours, not theirs" is proven by shape, not existence: an advancement id
 * resolves to one file across the pack stack, and both IaF's originals and our
 * reworks occupy the same ids - so the assertions compare criterion names and
 * reward contents, which differ between the two, and the recipe-unlock
 * rework's rewards are checked to name our dragon recipes.</p>
 *
 * <p>Everything is written against the environment, like the rest of the dragon
 * tests: without IaF there is no pack and no IaF ids at all, and with the
 * integration switch off the recipe override deliberately restores IaF's
 * original - as-if-absent is part of the contract under test.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IafOverridePackGameTests {

    private IafOverridePackGameTests() {
    }

    @GameTest(template = "test_area")
    public static void iafDragonHornRecipeFollowsTheIntegrationSwitch(GameTestHelper helper) {
        boolean iaf = ModList.get().isLoaded("iceandfire");
        boolean integration = iaf && PseudocoreConfig.iceAndFireIntegrationEnabled();
        var recipes = helper.getLevel().getServer().getRecipeManager();

        boolean present = recipes.byKey(
                ResourceLocation.fromNamespaceAndPath("iceandfire", "dragon_horn")).isPresent();

        check(present == (iaf && !integration),
                "IaF's dragon horn recipe must exist exactly when IaF is installed and this mod's"
                        + " integration is off; with the integration on the override pack must remove it."
                        + " Found present=" + present + " iaf=" + iaf + " integration=" + integration);

        helper.succeed();
    }

    @GameTest(template = "test_area")
    public static void iafDragonHornAdvancementsLoadAsOurReworks(GameTestHelper helper) {
        if (!ModList.get().isLoaded("iceandfire")) {
            helper.succeed();
            return;
        }
        var advancements = helper.getLevel().getServer().getAdvancements();

        AdvancementHolder recipeUnlock = advancements.get(
                ResourceLocation.fromNamespaceAndPath("iceandfire", "recipes/misc/dragon_horn"));
        check(recipeUnlock != null, "the recipe-unlock advancement id must exist");
        check(recipeUnlock.value().criteria().containsKey("has_item")
                        && !recipeUnlock.value().criteria().containsKey("has_the_recipe"),
                "the loaded recipe-unlock advancement must be Pseudocore's rework, not IaF's original"
                        + " (criterion names differ)");
        check(recipeUnlock.value().rewards().recipes().contains(
                        ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, "copper_dragon_pseudocore")),
                "the rework must unlock the dragon pseudocore recipes, got "
                        + recipeUnlock.value().rewards().recipes());

        AdvancementHolder gameplay = advancements.get(
                ResourceLocation.fromNamespaceAndPath("iceandfire", "iceandfire/dragon_horn"));
        check(gameplay != null, "the gameplay advancement id must exist");
        check(gameplay.value().criteria().containsKey("obtain_dragon_pseudocore"),
                "the gameplay advancement must fire from our dragon items, not IaF's horn");
        check(gameplay.value().parent().isPresent(),
                "IaF's parent (the dragon egg advancement) must be preserved");

        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
