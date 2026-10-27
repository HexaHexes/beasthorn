package org.bitwisemadness.pseudocore.gametest;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;
import org.bitwisemadness.pseudocore.item.DragonPseudocoreItem;
import org.bitwisemadness.pseudocore.registry.ModItems;
import org.bitwisemadness.pseudocore.util.IafHornAccess;

import java.util.List;

/**
 * The dragon line: five tiers of Ice and Fire horn, one recipe state at a time,
 * config sections defaulting to the stage ladder, and a stage cap that actually
 * refuses a grown dragon.
 *
 * <p>The stage test grows a fire dragon to 75 days - stage 4 by IaF's own
 * {@code getAgeInDays() / 25 + 1} - and requires the copper tier (cap 1) to
 * refuse it while storing nothing. The <b>capture</b> side (the netherite tier
 * accepting the same dragon) cannot be driven from a game test at all: vanilla's
 * {@code isOwnedBy} compares the holder against the owner <i>resolved from the
 * level's player list</i>, and the harness's mock player can never appear there
 * - even IaF's own horn refuses a tamed dragon held by a mock player, which a
 * baseline attempt established directly. That side stays covered by IaF's own
 * logic in live play; the refusal gate fires in our override before IaF's
 * ownership check is ever reached, which is exactly what is asserted here.</p>
 *
 * <p>Every dragon test is a no-op success without Ice and Fire, matching the
 * line's registration: absent mod, absent items, nothing to assert.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DragonLineGameTests {

    private static final List<String> DRAGON_METALS =
            List.of("copper", "iron", "silver", "gold", "netherite");
    private static final int[] STAGE_LADDER = {1, 2, 3, 4, 5};

    private DragonLineGameTests() {
    }

    @GameTest(template = "test_area")
    public static void dragonTiersHaveExactlyTheirCurrentRecipe(GameTestHelper helper) {
        if (!ModItems.dragonTiersAvailable()) {
            helper.succeed();
            return;
        }
        Level level = helper.getLevel();
        boolean tensura = ModList.get().isLoaded("tensura") && PseudocoreConfig.tensuraIntegrationEnabled();
        RecipeManager recipes = level.getServer().getRecipeManager();

        for (int i = 0; i < DRAGON_METALS.size(); i++) {
            String metal = DRAGON_METALS.get(i);
            Item result = ModItems.DRAGON_TIERS.get(i).get();
            ResourceLocation crystalId = recipeId(metal, "");
            ResourceLocation gemId = recipeId(metal, "_gem");

            check(recipes.byKey(crystalId).isPresent() == tensura,
                    "the magic-crystal recipe " + crystalId + " must be loaded exactly when Tensura's"
                            + " integration is active and IaF is installed");
            check(recipes.byKey(gemId).isPresent() == !tensura,
                    "the gem recipe " + gemId + " must be loaded exactly when Tensura's integration is"
                            + " inactive, so the " + metal + " dragon tier is never uncraftable");

            ResourceLocation activeId = tensura ? crystalId : gemId;
            RecipeHolder<?> holder = recipes.byKey(activeId).orElseThrow(() ->
                    new GameTestAssertException("expected " + activeId + " to be loaded"));
            check(holder.value() instanceof CraftingRecipe,
                    activeId + " must be a shaped crafting recipe");

            Item ingot = firstOfTag("c", "ingots/" + metal);
            Item bone = firstOfTag("c", "bones/dragon");
            Item centre = tensura ? itemOrAir("tensura:high_quality_magic_crystal") : firstOfTag("c", "gems");
            check(ingot != Items.AIR && bone != Items.AIR && centre != Items.AIR,
                    "the ring ingredients must resolve: ingot=" + ingot + " bone=" + bone
                            + " centre=" + centre);

            CraftingInput input = CraftingInput.of(3, 3, List.of(
                    new ItemStack(bone), new ItemStack(ingot), new ItemStack(bone),
                    new ItemStack(ingot), new ItemStack(centre), new ItemStack(ingot),
                    new ItemStack(bone), new ItemStack(ingot), new ItemStack(bone)));

            check(((CraftingRecipe) holder.value()).matches(input, level),
                    activeId + " must accept the full dragon-bone ring around " + centre);
            check(holder.value().getResultItem(level.registryAccess()).is(result),
                    activeId + " must craft " + result + ", got "
                            + holder.value().getResultItem(level.registryAccess()));
        }

        helper.succeed();
    }

    @GameTest(template = "test_area")
    public static void dragonTierSectionsDefaultToTheStageLadder(GameTestHelper helper) {
        for (int i = 0; i < DRAGON_METALS.size(); i++) {
            String key = DRAGON_METALS.get(i);
            PseudocoreConfig.DragonTierSection section = PseudocoreConfig.COMMON.dragonTier(key).orElse(null);
            check(section != null, "no dragon config section for tier " + key);
            check(section.maxStage.get() == STAGE_LADDER[i],
                    key + ".maxStage must default to " + STAGE_LADDER[i] + ", got " + section.maxStage.get());
            check(section.maxEP.get() == 0.0D, key + ".maxEP must default to 0 (no cap)");
            check(section.allowTamedMobs.get(), key + " must default to allowing tamed dragons");
            check(section.allowTensuraNamed.get(), key + " must default to allowing named dragons");

            PseudocoreConfig.DragonTierLimits limits = PseudocoreConfig.COMMON.dragonTierLimits(key);
            check(limits.maxStage() == STAGE_LADDER[i] && limits.maxEP() == 0.0D
                    && limits.allowTamedMobs() && limits.allowTensuraNamed(),
                    "the resolved limits for " + key + " must match the section's defaults");
        }

        check(PseudocoreConfig.dragonTierKey(ModItems.IRON_PSEUDOCORE.get()).isEmpty(),
                "beast items are not dragon tiers");

        helper.succeed();
    }

    @GameTest(template = "test_area")
    public static void stageCapRefusesGrownDragons(GameTestHelper helper) {
        if (!ModItems.dragonTiersAvailable()) {
            helper.succeed();
            return;
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        @SuppressWarnings("unchecked")
        var fireDragon = (net.minecraft.world.entity.EntityType<DragonBaseEntity>) BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.fromNamespaceAndPath("iceandfire", "fire_dragon"));
        DragonBaseEntity dragon = helper.spawn(fireDragon, 1, 1, 1);
        dragon.tame(player);
        dragon.setAgeInDays(75);

        int stage = dragon.getDragonStage();
        check(stage >= 3, "a 75-day dragon must read as grown (stage >= 3), got stage " + stage);

        DragonPseudocoreItem copper = (DragonPseudocoreItem) ModItems.DRAGON_TIERS.get(0).get();
        ItemStack copperStack = new ItemStack(copper);
        InteractionResult refused = copper.interactLivingEntity(copperStack, player, dragon, InteractionHand.MAIN_HAND);
        check(refused == InteractionResult.PASS,
                "the copper tier (stage cap 1) must refuse a stage-" + stage + " dragon, got " + refused);
        check(IafHornAccess.read(copperStack) == null,
                "a refused capture must not store the dragon");

        helper.succeed();
    }

    private static ResourceLocation recipeId(String metal, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, metal + "_dragon_pseudocore" + suffix);
    }

    private static Item itemOrAir(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(Items.AIR);
    }

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
}
