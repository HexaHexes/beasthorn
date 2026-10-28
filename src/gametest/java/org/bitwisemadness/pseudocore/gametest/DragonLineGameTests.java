package org.bitwisemadness.pseudocore.gametest;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;
import org.bitwisemadness.pseudocore.item.DragonPseudocoreItem;
import org.bitwisemadness.pseudocore.registry.ModDataComponents;
import org.bitwisemadness.pseudocore.registry.ModItems;
import org.bitwisemadness.pseudocore.util.IafHornAccess;
import org.bitwisemadness.pseudocore.util.TooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The dragon line: five tiers of Ice and Fire horn, one recipe state at a time,
 * config sections defaulting to the stage ladder, and a stage cap that actually
 * refuses a grown dragon.
 *
 * <p>The stage test grows a fire dragon to 75 days - stage 4 by IaF's own
 * banding, 75 days being where stage 4 starts - and requires the copper tier
 * (cap 1) to refuse it while storing nothing. The <b>capture</b> side (the netherite tier
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
    /**
     * The default EP caps, one per metal in {@link #DRAGON_METALS} order:
     * 1.5x that stage's end-of-row EP from Tensura's progression, with the
     * netherite tier - the line's last, its cap of caps - uncapped at 0.
     */
    private static final double[] DEFAULT_DRAGON_MAX_EP = {150_000.0, 300_000.0, 525_000.0, 825_000.0, 0.0};
    private static final Pattern DIGITS = Pattern.compile("\\d+");

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
            double expectedMaxEP = DEFAULT_DRAGON_MAX_EP[i];
            check(section.maxEP.get() == expectedMaxEP,
                    key + ".maxEP must default to " + expectedMaxEP
                            + " (1.5x its stage's end-of-row EP, 0 for the uncapped netherite tier), got "
                            + section.maxEP.get());
            check(section.allowTamedMobs.get(), key + " must default to allowing tamed dragons");
            check(section.allowTensuraNamed.get(), key + " must default to allowing named dragons");

            PseudocoreConfig.DragonTierLimits limits = PseudocoreConfig.COMMON.dragonTierLimits(key);
            check(limits.maxStage() == STAGE_LADDER[i] && limits.maxEP() == expectedMaxEP
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

    /**
     * The click layer (#37): capture is driven from
     * {@code PlayerInteractEvent.EntityInteract}, because vanilla asks
     * {@code entity.interact()} first and a tamed dragon - the most rideable mob
     * in the pack - swallowed every dragon pseudocore click before the tier
     * gates could run. This drives the real event through the real bus.
     *
     * <p>Control first: the beast line's wiring - the pattern the dragon line
     * borrows - must capture an unowned cow and come back cancelled with the mob
     * stored. Then the dragon line: a mock holder can never satisfy IaF's
     * own-the-dragon guard (the stage test documents why), so the assertable
     * contract is what IaF's own bytecode dictates - every decline it makes
     * returns CONSUME (the client prediction, the non-dragon target and the
     * not-your-dragon case all share that path). The capped tier's refusal is
     * OURS: a PASS that leaves the event uncanceled so the dragon keeps its
     * vanilla click. The uncapped tier's hand-off is IaF's: the event comes
     * back cancelled with CONSUME and nothing stored - the wiring ran our
     * gates, reached IaF's guard, and was declined silently, which is what
     * holding a horn-shaped item does to a dragon you do not own.</p>
     */
    @GameTest(template = "test_area")
    public static void captureClicksReachTheItemThroughTheEvent(GameTestHelper helper) {
        if (!ModItems.dragonTiersAvailable()) {
            helper.succeed();
            return;
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);

        var cow = helper.spawn(net.minecraft.world.entity.EntityType.COW, 1, 1, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.IRON_PSEUDOCORE.get()));
        var beastEvent = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow);
        NeoForge.EVENT_BUS.post(beastEvent);
        check(beastEvent.isCanceled(),
                "control failed: the beast line's click must capture the cow and cancel the event");
        check(beastEvent.getCancellationResult() == InteractionResult.SUCCESS,
                "control failed: the cancelled click must report SUCCESS, got "
                        + beastEvent.getCancellationResult());
        check(player.getMainHandItem().has(ModDataComponents.CONTAINED_MOB.get()),
                "control failed: the capture must have stored the cow");

        @SuppressWarnings("unchecked")
        var fireDragon = (net.minecraft.world.entity.EntityType<DragonBaseEntity>) BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.fromNamespaceAndPath("iceandfire", "fire_dragon"));
        DragonBaseEntity dragon = helper.spawn(fireDragon, 1, 1, 1);
        dragon.setAgeInDays(75);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DRAGON_TIERS.get(0).get()));
        var cappedEvent = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, dragon);
        NeoForge.EVENT_BUS.post(cappedEvent);
        check(!cappedEvent.isCanceled(),
                "a stage-capped refusal must pass the click back so the dragon stays ridable");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DRAGON_TIERS.get(4).get()));
        var uncappedEvent = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, dragon);
        NeoForge.EVENT_BUS.post(uncappedEvent);
        check(uncappedEvent.isCanceled()
                        && uncappedEvent.getCancellationResult() == InteractionResult.CONSUME,
                "the uncapped tier must hand off to IaF's capture path, whose every decline returns"
                        + " CONSUME - got canceled=" + uncappedEvent.isCanceled()
                        + " result=" + uncappedEvent.getCancellationResult());
        check(IafHornAccess.read(player.getMainHandItem()) == null,
                "a capture stopped by IaF's guard must not store the dragon");

        helper.succeed();
    }

    /**
     * The tooltip's stage line is the living dragon's stage (#48): it must
     * equal {@code getDragonStage()} for every growth cap this environment can
     * configure, never a copy of the stock 25-day bands. Three worlds run
     * over one tamed 98-day dragon - the untouched default, a lowered cap
     * (the clamp path: both sides must stop at day 64), and a raised cap with
     * tensura-iaf-growth absent, where Ice and Fire's config grows but its
     * bands do not, so the tooltip must not stretch either.
     *
     * <p>The reported pack - cap 512 WITH the addon - is pinned numerically
     * in {@code DragonStagesTest}, because no test environment loads the
     * addon; the equality asserted here is the property that test's inputs
     * feed. The config is restored in a finally block, so a failed leg cannot
     * leak a moved cap into later tests.</p>
     */
    @GameTest(template = "test_area")
    public static void tooltipStageMatchesTheLivingDragon(GameTestHelper helper) {
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
        dragon.setAgeInDays(98);
        check(dragon.isTame(),
                "the dragon must be tame so the tamed clamp path is the one under test");

        IntegerEntry ageCap = IafCommonConfig.INSTANCE.dragon.maxTamedDragonAge;
        int original = ageCap.getValue();
        try {
            for (int cap : new int[] { original, 64, 512 }) {
                ageCap.setValue(cap);

                CompoundTag data = new CompoundTag();
                data.putInt("AgeTicks", 98 * 24000);
                List<Component> tooltip = new ArrayList<>();
                TooltipHelper.addDragonLines(tooltip, data);

                int shown = parseStageLine(tooltip);
                int live = dragon.getDragonStage();
                check(shown == live, "growth cap " + cap + ": the tooltip must render stage " + live
                        + " (what the living dragon reports), got " + shown);
            }
        } finally {
            ageCap.setValue(original);
        }

        helper.succeed();
    }

    /** The stage line is addDragonLines' second line; its first number is the stage. */
    private static int parseStageLine(List<Component> tooltip) {
        check(tooltip.size() >= 2,
                "the dragon block must render a gender line and a stage line, got " + tooltip.size());
        Matcher matcher = DIGITS.matcher(tooltip.get(1).getString());
        check(matcher.find(), "the stage line must carry a stage number: " + tooltip.get(1).getString());
        return Integer.parseInt(matcher.group());
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
