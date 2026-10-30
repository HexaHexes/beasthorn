package org.bitwisemadness.pseudocore.gametest;

import io.github.lightman314.lightmanscurrency.common.traders.item.tradedata.ItemTradeData;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.item.component.ContainedMobComponent;
import org.bitwisemadness.pseudocore.registry.ModDataComponents;
import org.bitwisemadness.pseudocore.registry.ModItems;

/**
 * Proves that a Pseudocore put up for sale through Lightman's Currency arrives at the
 * buyer still holding its mob.
 *
 * <p>This is the one interaction in the pack where a horn stops being a horn in this
 * mod's hands and becomes data in somebody else's. Lightman's persists a trade by
 * calling {@code ItemStack#save} and restores it with {@code ItemStack#parseOptional},
 * which is the only pair in Minecraft that carries a full {@code DataComponentPatch} -
 * and its stock gate compares whole component maps rather than item ids. If either of
 * those ever stopped being true, the failure would be silent and permanent: the listing
 * would show a horn, the buyer would pay, and the captured mob would simply be gone.</p>
 *
 * <p>Both tests go through Lightman's own public entry points rather than reimplementing
 * the round trip, so they keep meaning whatever Lightman's decides to do internally.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LightmansTradeGameTests {

    private static final ResourceLocation COW_TYPE = ResourceLocation.withDefaultNamespace("cow");
    private static final ResourceLocation ZOMBIE_TYPE = ResourceLocation.withDefaultNamespace("zombie");

    private static final UUID LISTED_UUID = new UUID(0x5EEDL, 0xA11CEL);
    private static final UUID OTHER_UUID = new UUID(0xB0BEL, 0xDEADL);

    private LightmansTradeGameTests() {
    }

    // ---------------------------------------------------------------------
    // The list half: trader data is written to NBT and read back constantly,
    // on every trader save and on world load.
    // ---------------------------------------------------------------------

    /**
     * A horn listed on a trade, then written to NBT and read back, must come back
     * identical - same item, same mob type, same UUID, same complete entity NBT.
     */
    @GameTest(template = "test_area")
    public static void listed_horn_keeps_its_mob(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        ItemStack listed = hornWith(COW_TYPE, LISTED_UUID, cowNbt());
        ContainedMobComponent before = componentOf(listed);

        ItemTradeData trade = ItemTradeData.create(true);
        trade.setItem(listed, 0);

        ItemTradeData reloaded =
                ItemTradeData.loadData(trade.getAsNBT(registries), () -> ItemTradeData.create(true), registries);

        ItemStack handedOver = reloaded.getItem(0);
        check(!handedOver.isEmpty(),
                "the listed horn vanished from the trade across the save/load round trip");
        check(handedOver.is(ModItems.IRON_PSEUDOCORE.get()),
                "the trade must come back holding a Pseudocore, got " + handedOver);

        ContainedMobComponent after = componentOf(handedOver);
        check(after != null,
                "the horn lost contained_mob across the round trip - the buyer would receive an "
                        + "empty horn and the captured mob would be destroyed with no error anywhere");
        check(after.entityType().equals(before.entityType()),
                "the horn must name the same mob type after the round trip, got "
                        + after.entityType() + " instead of " + before.entityType());
        check(after.entityUuid().equals(before.entityUuid()),
                "the horn must keep the same mob UUID after the round trip, so whatever is bound "
                        + "to the mob by UUID still matches, got " + after.entityUuid()
                        + " instead of " + before.entityUuid());
        check(after.entityData().equals(before.entityData()),
                "the horn must keep the mob's complete NBT after the round trip, got "
                        + after.entityData() + " instead of " + before.entityData());
        check(handedOver.getComponents().equals(listed.getComponents()),
                "every component on the horn must survive the round trip intact");

        helper.succeed();
    }

    // ---------------------------------------------------------------------
    // The buy half: what the trader will accept as stock determines what can
    // actually be handed over when a buyer pays.
    // ---------------------------------------------------------------------

    /**
     * With a horn listed, only another horn holding the very same mob may stand in as
     * stock. An empty horn and a horn holding a different mob must both be refused.
     */
    @GameTest(template = "test_area")
    public static void only_the_listed_horn_can_be_sold(GameTestHelper helper) {
        ItemTradeData trade = ItemTradeData.create(true);
        trade.setItem(hornWith(COW_TYPE, LISTED_UUID, cowNbt()), 0);

        check(trade.getEnforceNBT(0),
                "this slot must compare components by default. Lightman's ships that way, but if it "
                        + "ever stops, every horn becomes interchangeable with every other one and the "
                        + "buyer receives whatever happens to be in stock");

        ItemStack identical = hornWith(COW_TYPE, LISTED_UUID, cowNbt());
        check(trade.allowItemInStorage(identical),
                "an identical horn must be accepted as stock for the trade it was listed under");

        ItemStack empty = new ItemStack(ModItems.IRON_PSEUDOCORE.get());
        check(!trade.allowItemInStorage(empty),
                "an empty horn must not be accepted where a populated one is listed - the buyer "
                        + "would pay for a captured mob and receive a horn with nothing in it");

        ItemStack differentMob = hornWith(ZOMBIE_TYPE, OTHER_UUID, zombieNbt());
        check(!trade.allowItemInStorage(differentMob),
                "a horn holding a different mob must not be accepted either - the buyer must "
                        + "receive exactly the mob that was listed");

        helper.succeed();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static ItemStack hornWith(ResourceLocation type, UUID uuid, CompoundTag entityData) {
        ItemStack stack = new ItemStack(ModItems.IRON_PSEUDOCORE.get());
        stack.set(ModDataComponents.CONTAINED_MOB.get(), new ContainedMobComponent(type, uuid, entityData));
        return stack;
    }

    private static ContainedMobComponent componentOf(ItemStack stack) {
        ContainedMobComponent component = stack.get(ModDataComponents.CONTAINED_MOB.get());
        check(component != null, "the horn must hold a contained_mob component at all");
        return component;
    }

    private static CompoundTag cowNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:cow");
        tag.putUUID("UUID", LISTED_UUID);
        tag.putFloat("Health", 11.5F);
        tag.putString("CustomName", "{\"text\":\"Bessie\"}");
        tag.putBoolean("NoAI", true);
        return tag;
    }

    private static CompoundTag zombieNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:zombie");
        tag.putUUID("UUID", OTHER_UUID);
        tag.putFloat("Health", 20.0F);
        return tag;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
