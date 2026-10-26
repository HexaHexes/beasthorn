package org.bitwisemadness.pseudocore.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.registry.ModItems;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The tab is the only way to reach the item line without a recipe, so it is
 * checked as a set rather than as "an item exists".
 *
 * <p>Contents are built here explicitly: the game only builds a tab when the
 * creative screen opens, which never happens on a dedicated server. Building them
 * in the test is what makes the tab's contents assertable at all - and it also
 * exercises the generator itself, so a tab that throws while being populated
 * fails the suite rather than the first player who opens it.</p>
 *
 * <p>The "nothing but our items" assertion is the load-bearing one: it is what
 * keeps every future tier on this tab and off everyone else's.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeTabGameTests {

    private CreativeTabGameTests() {
    }

    @GameTest(template = "test_area")
    public static void ourTabHoldsOurItemsAndNothingElse(GameTestHelper helper) {
        ResourceLocation tabId = ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, "pseudocore");
        check(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(tabId),
                "no creative tab is registered as " + tabId + ", so every pseudocore would be"
                        + " reachable only through the search tab");

        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(tabId);

        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                helper.getLevel().enabledFeatures(), false, helper.getLevel().registryAccess()));

        var items = tab.getDisplayItems();
        var shown = items.stream().map(ItemStack::getItem).collect(Collectors.toSet());
        var expected = Set.copyOf(ModItems.all());

        check(shown.equals(expected),
                "the tab must hold exactly the registered tiers, in this environment " + expected
                        + ", found: " + shown);
        check(items.size() == expected.size(),
                "each tier must appear once, found " + items.size() + " entries: " + items);
        check(tab.getIconItem().is(ModItems.IRON_PSEUDOCORE.get()),
                "the tab icon must be the iron pseudocore, the tier every environment has, got "
                        + tab.getIconItem());

        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
