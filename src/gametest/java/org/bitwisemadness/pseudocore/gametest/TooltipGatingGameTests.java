package org.bitwisemadness.pseudocore.gametest;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.player.ITensuraPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.util.TooltipHelper;
import org.bitwisemadness.pseudocore.util.TensuraBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The tooltip's analysis gating: EP and the Tensura-recorded owner answer to
 * the viewer's analysis the same way Tensura Jade's panel gates them, while
 * name, race and health stay visible to everyone.
 *
 * <p>The viewer is supplied through the {@code addLines} overload that takes
 * one, because the production path resolves it from a client-only class and
 * this server has none. Everything else is the real tooltip path - the same
 * NBT digging, the same bridges, the same shared library - not a re-tread of
 * the logic under test.</p>
 *
 * <p>Every test is a no-op success without Tensura: with the mod absent there
 * is no EP line and no Tensura owner to gate in the first place, which is
 * itself the shipped behaviour.</p>
 */
@GameTestHolder(Pseudocore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TooltipGatingGameTests {

    /** The ManasCore storage id the existence compound rides under. */
    private static Optional<String> existenceId() {
        return TensuraBridge.existenceStorageId().map(ResourceLocation::toString);
    }

    private TooltipGatingGameTests() {
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /** A stored mob's tag carrying {@code aura + magicule} EP and a Tensura owner. */
    private static CompoundTag storedMob(double aura, double magicule, UUID tensuraOwner) {
        CompoundTag existence = new CompoundTag();
        existence.putDouble("aura", aura);
        existence.putDouble("magicule", magicule);
        if (tensuraOwner != null) {
            existence.putUUID("permanentOwner", tensuraOwner);
        }
        existence.putString("manascore_registry_storage_id", existenceId().orElseThrow());

        ListTag storages = new ListTag();
        storages.add(existence);
        CompoundTag root = new CompoundTag();
        root.putFloat("Health", 20.0F);
        CompoundTag storageRoot = new CompoundTag();
        storageRoot.put("manascore_registry_storage", storages);
        root.put("ManasCoreStorage", storageRoot);
        return root;
    }

    private static void setAttribute(Player player, Holder<Attribute> attribute, double value) {
        var instance = player.getAttribute(attribute);
        check(instance != null, "a player must carry " + attribute.getRegisteredName());
        instance.setBaseValue(value);
    }

    /** A viewer Tensura considers entitled to analysis at level 2. */
    private static Player entitledViewer(GameTestHelper helper) {
        Player viewer = helper.makeMockPlayer(GameType.CREATIVE);
        setAttribute(viewer, TensuraAttributes.ANALYSIS_LEVEL, 2);
        setAttribute(viewer, TensuraAttributes.ANALYSIS_DISTANCE, 64);
        // Caps above the attributes' registered minimum of 10.0: max EP reads
        // 1000 magicule + 50 aura = 1050, so reach at level 2 is 2100.
        setAttribute(viewer, TensuraAttributes.MAX_AURA, 50.0D);
        setAttribute(viewer, TensuraAttributes.MAX_MAGICULE, 1000.0D);
        ITensuraPlayer data = TensuraStorages.getPlayerDataFrom(viewer);
        check(data != null, "a viewer must carry Tensura player data");
        data.setAnalysisMode(0);
        return viewer;
    }

    /** A viewer Tensura refuses to analyse: level 0, whatever else they have. */
    private static Player unentitledViewer(GameTestHelper helper) {
        Player viewer = helper.makeMockPlayer(GameType.CREATIVE);
        setAttribute(viewer, TensuraAttributes.ANALYSIS_LEVEL, 0);
        setAttribute(viewer, TensuraAttributes.ANALYSIS_DISTANCE, 64);
        return viewer;
    }

    /** Index of the (first) tooltip line starting with {@code label}, or -1. */
    private static int indexOf(List<Component> tooltip, String label) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().startsWith(label)) {
                return i;
            }
        }
        return -1;
    }

    private static String text(List<Component> tooltip, String label) {
        int index = indexOf(tooltip, label);
        check(index >= 0, "expected a <" + label + "> line in " + texts(tooltip));
        return tooltip.get(index).getString();
    }

    private static List<String> texts(List<Component> tooltip) {
        return tooltip.stream().map(Component::getString).toList();
    }

    /**
     * The EP line answers to analysis exactly as Tensura Jade's panel does: a
     * holder without analysis sees no EP at all; a holder whose grasp falls
     * short of the stored mob's EP reads the reach figure plus "+"; and one
     * within reach reads the true value.
     */
    @GameTest(template = "test_area")
    public static void epLineHonoursAnalysisGating(GameTestHelper helper) {
        if (!ModList.get().isLoaded("tensura")) {
            helper.succeed();
            return;
        }
        Optional<String> id = existenceId();
        check(id.isPresent(), "Tensura's existence storage id must resolve");

        ResourceLocation direwolf = ResourceLocation.parse("tensura:direwolf");

        // A 100049-EP mob: far beyond the entitled viewer's reach of 2100.
        CompoundTag cloudy = storedMob(49.0D, 100000.0D, null);

        List<Component> ungatedTooltip = new ArrayList<>();
        TooltipHelper.addLines(ungatedTooltip, direwolf, cloudy,
                helper.getLevel().registryAccess(), unentitledViewer(helper));
        check(indexOf(ungatedTooltip, "EP") < 0,
                "a holder without analysis must see no EP line at all, got " + texts(ungatedTooltip));

        List<Component> blurredTooltip = new ArrayList<>();
        TooltipHelper.addLines(blurredTooltip, direwolf, cloudy,
                helper.getLevel().registryAccess(), entitledViewer(helper));
        check("EP: 2100+".equals(text(blurredTooltip, "EP")),
                "EP beyond reach must blur to the reach figure plus a plus, got "
                        + texts(blurredTooltip));

        // A 1550-EP mob sits inside that same reach, so the true figure survives.
        CompoundTag clear = storedMob(50.0D, 1500.0D, null);
        List<Component> inReachTooltip = new ArrayList<>();
        TooltipHelper.addLines(inReachTooltip, direwolf, clear,
                helper.getLevel().registryAccess(), entitledViewer(helper));
        check("EP: 1550".equals(text(inReachTooltip, "EP")),
                "EP within reach must read exactly, got " + texts(inReachTooltip));

        // Race and health are never gated, entitled viewer or not.
        check(indexOf(ungatedTooltip, "Race") >= 0,
                "the race line must stay visible to a viewer without analysis");
        check(indexOf(blurredTooltip, "Race") >= 0,
                "the race line must stay visible to an entitled viewer");
        check(text(ungatedTooltip, "Health").startsWith("Health: 20.0"),
                "health is a vanilla fact and must stay visible ungated, got " + texts(ungatedTooltip));

        helper.succeed();
    }

    /**
     * The Tensura-recorded owner is gated with the EP line; vanilla's Owner
     * tag - a tamed pet, not Tensura's information - shows to everyone.
     */
    @GameTest(template = "test_area")
    public static void tensuraOwnerIsGatedWhileVanillaOwnerIsNot(GameTestHelper helper) {
        if (!ModList.get().isLoaded("tensura")) {
            helper.succeed();
            return;
        }

        ResourceLocation cow = ResourceLocation.parse("minecraft:cow");
        UUID tensuraOwner = UUID.randomUUID();
        UUID vanillaOwner = UUID.randomUUID();

        CompoundTag tensuraOwned = storedMob(10.0D, 100.0D, tensuraOwner);
        List<Component> denied = new ArrayList<>();
        TooltipHelper.addLines(denied, cow, tensuraOwned,
                helper.getLevel().registryAccess(), unentitledViewer(helper));
        check(indexOf(denied, "Owner") < 0,
                "a Tensura-recorded owner must be withheld from a holder without analysis, got "
                        + texts(denied));

        List<Component> allowed = new ArrayList<>();
        TooltipHelper.addLines(allowed, cow, tensuraOwned,
                helper.getLevel().registryAccess(), entitledViewer(helper));
        check(text(allowed, "Owner").contains(tensuraOwner.toString().substring(0, 8)),
                "an entitled holder must see the Tensura owner, got " + texts(allowed));

        CompoundTag vanillaOwned = storedMob(10.0D, 100.0D, null);
        vanillaOwned.putUUID("Owner", vanillaOwner);
        List<Component> vanillaDenied = new ArrayList<>();
        TooltipHelper.addLines(vanillaDenied, cow, vanillaOwned,
                helper.getLevel().registryAccess(), unentitledViewer(helper));
        check(text(vanillaDenied, "Owner").contains(vanillaOwner.toString().substring(0, 8)),
                "a vanilla-tamed owner is not Tensura's information and must show to everyone, got "
                        + texts(vanillaDenied));

        helper.succeed();
    }

    /**
     * The variant-qualified Race line still works through the shared library -
     * a direwolf's stored variant reads "Red Fang Direwolf" exactly as before
     * the move, since the logic now ships in mob_info_common.
     */
    @GameTest(template = "test_area")
    public static void raceLineStillQualifiesWithTheVariant(GameTestHelper helper) {
        if (!ModList.get().isLoaded("tensura")) {
            helper.succeed();
            return;
        }

        ResourceLocation direwolf = ResourceLocation.parse("tensura:direwolf");
        check(BuiltInRegistries.ENTITY_TYPE.getOptional(direwolf).isPresent(),
                "tensura:direwolf must be registered");

        CompoundTag data = new CompoundTag();
        data.putInt("Variant", 1);

        List<Component> tooltip = new ArrayList<>();
        TooltipHelper.addLines(tooltip, direwolf, data,
                helper.getLevel().registryAccess(), entitledViewer(helper));
        String race = text(tooltip, "Race");
        // Whatever Tensura's second direwolf variant is today, the line reads
        // "<variant> <species>" - the composition the shared library owns.
        // Matched structurally rather than against rendered text: whether this
        // headless server loaded Tensura's language file decides what the
        // species half reads as, and that is not what is under test here.
        check(race.startsWith("Race: ") && race.length() > "Race: ".length() + "Direwolf".length(),
                "a stored direwolf variant must qualify the race line, got <" + race + ">");
        check(!race.endsWith("Race: Direwolf"),
                "an unqualified race line would mean the variant never arrived, got <" + race + ">");

        helper.succeed();
    }
}
