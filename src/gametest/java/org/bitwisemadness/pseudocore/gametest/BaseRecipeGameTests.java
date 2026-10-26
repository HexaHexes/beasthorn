package org.bitwisemadness.pseudocore.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;
import org.bitwisemadness.pseudocore.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Every registered beast tier must have exactly one recipe at a time: the
 * magic-crystal recipe while Tensura's integration is on, the gem-tag recipe while
 * it is off, never both and never neither - the rule the whole tier line shares.
 *
 * <p>The assertions are written against the environment rather than against a
 * fixed expectation, so they hold with Tensura, without it, and with the
 * integration switched off. What is fixed is the invariant. Each check also
 * proves the loaded recipe is <b>craftable</b> rather than merely present: four
 * ingots are read straight out of {@code c:ingots/<metal>} - so silver needs no
 * special case, whichever mod supplies it - put around the centre the active
 * variant calls for, and the recipe has to match them and return the tier's item.
 * A typo in a tag, a pattern or a result id fails here instead of at a crafting
 * table.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseRecipeGameTests {

    private static final ResourceLocation LOW_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath("tensura", "low_quality_magic_crystal");
    private static final ResourceLocation MEDIUM_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath("tensura", "medium_quality_magic_crystal");
    private static final ResourceLocation HIGH_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath("tensura", "high_quality_magic_crystal");

    private BaseRecipeGameTests() {
    }

    @GameTest(template = "test_area")
    public static void everyTierHasExactlyItsCurrentRecipe(GameTestHelper helper) {
        Level level = helper.getLevel();
        boolean tensura = ModList.get().isLoaded("tensura") && PseudocoreConfig.tensuraIntegrationEnabled();
        RecipeManager recipes = level.getServer().getRecipeManager();

        for (Tier tier : registeredTiers()) {
            ResourceLocation crystalId = recipeId(tier.name(), "");
            ResourceLocation gemId = recipeId(tier.name(), "_gem");

            check(recipes.byKey(crystalId).isPresent() == tensura,
                    "the magic-crystal recipe " + crystalId + " must be loaded exactly when Tensura's"
                            + " integration is active; found it " + recipes.byKey(crystalId).isPresent()
                            + " with Tensura active=" + tensura);
            check(recipes.byKey(gemId).isPresent() == !tensura,
                    "the gem recipe " + gemId + " must be loaded exactly when Tensura's integration is"
                            + " inactive, so " + tier.name() + " is never uncraftable and never"
                            + " double-craftable");

            ResourceLocation activeId = tensura ? crystalId : gemId;
            RecipeHolder<?> holder = recipes.byKey(activeId).orElseThrow(() ->
                    new GameTestAssertException("expected " + activeId + " to be loaded"));

            check(holder.value() instanceof CraftingRecipe,
                    activeId + " must be a shaped crafting recipe, got "
                            + holder.value().getClass().getName());

            Item ingot = firstOfTag("c", "ingots/" + tier.name());
            check(ingot != Items.AIR,
                    "no item in c:ingots/" + tier.name() + " - the " + tier.name()
                            + " tier is registered but nothing can craft its ingot sides");

            Item centre = tensura ? itemOrAir(tier.crystal()) : firstOfTag("c", "gems");
            check(centre != Items.AIR, "the centre ingredient for the active variant did not resolve");

            CraftingInput input = CraftingInput.of(3, 3, List.of(
                    ItemStack.EMPTY, new ItemStack(ingot), ItemStack.EMPTY,
                    new ItemStack(ingot), new ItemStack(centre), new ItemStack(ingot),
                    ItemStack.EMPTY, new ItemStack(ingot), ItemStack.EMPTY));

            check(((CraftingRecipe) holder.value()).matches(input, level),
                    activeId + " must accept four " + ingot + " around " + centre
                            + " - the tag, the pattern and the centre must actually fit together");

            check(holder.value().getResultItem(level.registryAccess()).is(tier.result()),
                    activeId + " must craft " + tier.result() + ", got "
                            + holder.value().getResultItem(level.registryAccess()));
        }

        helper.succeed();
    }

    /** Every tier that is registered in this environment, in ascending order. */
    private static List<Tier> registeredTiers() {
        List<Tier> tiers = new ArrayList<>(12);
        tiers.add(new Tier("copper", LOW_CRYSTAL, ModItems.COPPER_PSEUDOCORE.get()));
        tiers.add(new Tier("iron", LOW_CRYSTAL, ModItems.IRON_PSEUDOCORE.get()));
        if (ModItems.silverAvailable()) {
            tiers.add(new Tier("silver", LOW_CRYSTAL, ModItems.SILVER_PSEUDOCORE.get()));
        }
        tiers.add(new Tier("gold", LOW_CRYSTAL, ModItems.GOLD_PSEUDOCORE.get()));
        tiers.add(new Tier("netherite", MEDIUM_CRYSTAL, ModItems.NETHERITE_PSEUDOCORE.get()));
        if (ModItems.tensuraTiersAvailable()) {
            String[] metals = { "low_magisteel", "high_magisteel", "pure_magisteel",
                    "mithril", "orichalcum", "adamantite", "hihiirokane" };
            ResourceLocation[] crystals = { MEDIUM_CRYSTAL, MEDIUM_CRYSTAL, MEDIUM_CRYSTAL,
                    HIGH_CRYSTAL, HIGH_CRYSTAL, HIGH_CRYSTAL, HIGH_CRYSTAL };
            for (int i = 0; i < metals.length; i++) {
                tiers.add(new Tier(metals[i], crystals[i], ModItems.TENSURA_TIERS.get(i).get()));
            }
        }
        return tiers;
    }

    private static ResourceLocation recipeId(String tier, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, tier + "_pseudocore" + suffix);
    }

    private static Item itemOrAir(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
    }

    /** The first item of a conventional tag, or air when the tag is empty. */
    private static Item firstOfTag(String namespace, String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(namespace, path));
        return BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(holders -> holders.stream().findFirst().map(Holder::value))
                .orElse(Items.AIR);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private record Tier(String name, ResourceLocation crystal, Item result) {
    }
}
