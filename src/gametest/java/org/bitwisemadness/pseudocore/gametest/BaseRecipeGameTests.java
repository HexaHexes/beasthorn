package org.bitwisemadness.pseudocore.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;
import org.bitwisemadness.pseudocore.registry.ModItems;

import java.util.List;

/**
 * The iron tier is the tier that exists in every environment, and it is the one
 * that shows the dual-state recipe grammar: a magic-crystal recipe while Tensura's
 * integration is on, a gem-tag recipe while it is off, never both and never
 * neither.
 *
 * <p>The assertions are written against the environment rather than against a
 * fixed expectation, so they hold in a pack with Tensura, in a pack without it,
 * and with the integration switched off. What is fixed is the invariant: exactly
 * one of the two recipes is loaded, and the loaded one really accepts four iron
 * ingots around its centre ingredient. That last check is what a JSON-only test
 * cannot give - it proves the pattern, the tag reference and the result id fit
 * together, so the recipe is craftable rather than merely present.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseRecipeGameTests {

    private static final ResourceLocation CRYSTAL_RECIPE =
            ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, "iron_pseudocore");
    private static final ResourceLocation GEM_RECIPE =
            ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, "iron_pseudocore_gem");
    private static final ResourceLocation TENSURA_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath("tensura", "low_quality_magic_crystal");

    private BaseRecipeGameTests() {
    }

    @GameTest(template = "test_area")
    public static void ironTierHasExactlyItsCurrentRecipe(GameTestHelper helper) {
        boolean tensura = ModList.get().isLoaded("tensura") && PseudocoreConfig.tensuraIntegrationEnabled();
        var recipeManager = helper.getLevel().getServer().getRecipeManager();

        check(recipeManager.byKey(CRYSTAL_RECIPE).isPresent() == tensura,
                "the magic-crystal recipe " + CRYSTAL_RECIPE + " must be loaded exactly when Tensura's"
                        + " integration is active; found it " + recipeManager.byKey(CRYSTAL_RECIPE).isPresent()
                        + " with Tensura active=" + tensura);
        check(recipeManager.byKey(GEM_RECIPE).isPresent() == !tensura,
                "the gem recipe " + GEM_RECIPE + " must be loaded exactly when Tensura's integration is"
                        + " inactive, so the iron tier is never uncraftable and never double-craftable");

        ResourceLocation activeId = tensura ? CRYSTAL_RECIPE : GEM_RECIPE;
        RecipeHolder<?> holder = recipeManager.byKey(activeId).orElseThrow(() ->
                new GameTestAssertException("expected " + activeId + " to be loaded"));
        check(holder.value() instanceof CraftingRecipe,
                activeId + " must be a shaped crafting recipe, got " + holder.value().getClass().getName());

        Level level = helper.getLevel();
        Item centre = tensura ? BuiltInRegistries.ITEM.get(TENSURA_CRYSTAL) : Items.EMERALD;
        check(centre != Items.AIR,
                "the centre ingredient for the active variant did not resolve: " + TENSURA_CRYSTAL);

        CraftingInput input = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
                new ItemStack(Items.IRON_INGOT), new ItemStack(centre), new ItemStack(Items.IRON_INGOT),
                ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY));

        check(((CraftingRecipe) holder.value()).matches(input, level),
                "the active iron recipe must accept four iron ingots around " + centre.getDescriptionId()
                        + " - the c:ingots/iron tag and the pattern must actually fit together");

        check(holder.value().getResultItem(level.registryAccess()).is(ModItems.IRON_PSEUDOCORE.get()),
                "the loaded iron recipe must craft the iron pseudocore, got "
                        + holder.value().getResultItem(level.registryAccess()));

        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
