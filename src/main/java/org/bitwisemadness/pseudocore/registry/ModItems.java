package org.bitwisemadness.pseudocore.registry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.bitwisemadness.pseudocore.item.DragonPseudocoreItem;
import org.bitwisemadness.pseudocore.item.PseudocoreItem;

/**
 * The beast line: one item per metal tier, in ascending tier order.
 *
 * <p>Iron, copper, gold and netherite are unconditional - they come from vanilla
 * metals, so the line is complete on a bare install. Silver registers only when a
 * mod that ships silver is installed ({@link #SILVER_PSEUDOCORE} is nullable and
 * {@link #all()} skips it), and the seven Tensura metal tiers ({@link
 * #TENSURA_TIERS}) only when Tensura is - without one of those mods there would be
 * no way to make the item, which would sit in the creative tab as a dead
 * entry.</p>
 *
 * <p>The dragon line ({@link #DRAGON_TIERS}) registers only with Ice and Fire CE,
 * for the same reason: without it there are no dragons to capture.</p>
 *
 * <p>The integration switches do <b>not</b> gate registration, deliberately:
 * these metals are materials the mods add to the world, not integration content.
 * With a switch off the ingot still exists, and the tier stays craftable - silver
 * through the tag contribution in {@code data/c/tags/item/ingots/silver.json}, the
 * Tensura metals through their gem-variant recipes. Registration only follows mod
 * presence.</p>
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("pseudocore");

    public static final DeferredItem<PseudocoreItem> COPPER_PSEUDOCORE = register("copper_pseudocore");
    public static final DeferredItem<PseudocoreItem> IRON_PSEUDOCORE = register("iron_pseudocore");
    public static final DeferredItem<PseudocoreItem> GOLD_PSEUDOCORE = register("gold_pseudocore");
    public static final DeferredItem<PseudocoreItem> NETHERITE_PSEUDOCORE = register("netherite_pseudocore");

    /**
     * The silver tier, present only when Ice and Fire CE or Tensura is installed.
     * Null otherwise - always query {@link #silverAvailable()} or iterate
     * {@link #all()} instead of touching this directly.
     */
    public static final DeferredItem<PseudocoreItem> SILVER_PSEUDOCORE =
            silverSourcePresent() ? register("silver_pseudocore") : null;

    /**
     * The Tensura metal tiers - low, high and pure magisteel, mithril, orichalcum,
     * adamantite and hihi'irokane, in ascending order. Empty unless Tensura is
     * installed: without it these metals do not exist and the items would be dead
     * entries. The integration switch does not gate registration, for the same
     * reason it does not gate silver's - the metals exist in the world for as long
     * as Tensura does, and it is the recipes, not the items, that follow the
     * switch.
     */
    public static final List<DeferredItem<PseudocoreItem>> TENSURA_TIERS = registerTensuraTiers();

    /**
     * The dragon tiers - one per metal, in ascending order, for capturing Ice
     * and Fire CE dragons, plus the three dragonsteel tiers (#112), one per
     * dragon element, appended after the metals so every existing index keeps
     * its meaning. Empty unless IaF is installed: without it there are no
     * dragons to capture and the items would be dead entries. As with the
     * other mod-supplied tiers, registration follows mod presence (configs do
     * not exist yet when it is decided); the integration switch gates
     * behaviour, and the recipes use the mod-presence condition so a
     * registered tier stays craftable.
     *
     * <p>Dragon pseudocores extend IaF's own DragonHornItem, so loading the item
     * class requires IaF. That is safe here for the same reason the Tensura
     * access classes are safe: this list is empty without IaF, so the supplier
     * that constructs the items never runs, and a generic signature erases, so
     * the class itself is never loaded.</p>
     */
    public static final List<DeferredItem<DragonPseudocoreItem>> DRAGON_TIERS = registerDragonTiers();

    /** Every registered tier, in ascending order. */
    private static final List<DeferredItem<PseudocoreItem>> TIERS = buildTiers();

    private ModItems() {
    }

    /** The items of every registered tier: the beast line, then the dragon line. */
    public static List<Item> all() {
        List<Item> items = new ArrayList<>(TIERS.size() + DRAGON_TIERS.size());
        for (DeferredItem<PseudocoreItem> tier : TIERS) {
            items.add(tier.get());
        }
        for (DeferredItem<DragonPseudocoreItem> tier : DRAGON_TIERS) {
            items.add(tier.get());
        }
        return items;
    }

    /** Whether the silver tier exists in this environment. */
    public static boolean silverAvailable() {
        return SILVER_PSEUDOCORE != null;
    }

    /** Whether the Tensura metal tiers exist in this environment. */
    public static boolean tensuraTiersAvailable() {
        return !TENSURA_TIERS.isEmpty();
    }

    /** Whether the dragon tiers exist in this environment. */
    public static boolean dragonTiersAvailable() {
        return !DRAGON_TIERS.isEmpty();
    }

    private static List<DeferredItem<PseudocoreItem>> buildTiers() {
        List<DeferredItem<PseudocoreItem>> tiers = new ArrayList<>(12);
        tiers.add(COPPER_PSEUDOCORE);
        tiers.add(IRON_PSEUDOCORE);
        if (SILVER_PSEUDOCORE != null) {
            tiers.add(SILVER_PSEUDOCORE);
        }
        tiers.add(GOLD_PSEUDOCORE);
        tiers.add(NETHERITE_PSEUDOCORE);
        tiers.addAll(TENSURA_TIERS);
        return List.copyOf(tiers);
    }

    private static List<DeferredItem<PseudocoreItem>> registerTensuraTiers() {
        if (!tensuraPresent()) {
            return List.of();
        }
        List<DeferredItem<PseudocoreItem>> tiers = new ArrayList<>(7);
        tiers.add(register("low_magisteel_pseudocore"));
        tiers.add(register("high_magisteel_pseudocore"));
        tiers.add(register("pure_magisteel_pseudocore"));
        tiers.add(register("mithril_pseudocore"));
        tiers.add(register("orichalcum_pseudocore"));
        tiers.add(register("adamantite_pseudocore"));
        tiers.add(register("hihiirokane_pseudocore"));
        return List.copyOf(tiers);
    }

    private static boolean tensuraPresent() {
        ModList modList = ModList.get();
        return modList == null || modList.isLoaded("tensura");
    }

    private static List<DeferredItem<DragonPseudocoreItem>> registerDragonTiers() {
        if (!iceAndFirePresent()) {
            return List.of();
        }
        List<DeferredItem<DragonPseudocoreItem>> tiers = new ArrayList<>(8);
        for (String metal : List.of("copper", "iron", "silver", "gold", "netherite")) {
            tiers.add(ITEMS.register(metal + "_dragon_pseudocore", () -> new DragonPseudocoreItem()));
        }
        // The dragonsteel line (#112): one tier per dragon element, each
        // element-gated by default through its config section.
        for (String element : List.of("fire", "ice", "lightning")) {
            tiers.add(ITEMS.register(element + "_dragonsteel_pseudocore", () -> new DragonPseudocoreItem()));
        }
        return List.copyOf(tiers);
    }

    private static boolean iceAndFirePresent() {
        ModList modList = ModList.get();
        return modList == null || modList.isLoaded("iceandfire");
    }

    private static DeferredItem<PseudocoreItem> register(String name) {
        return ITEMS.registerItem(name,
                properties -> new PseudocoreItem(properties.stacksTo(1).rarity(Rarity.UNCOMMON)));
    }

    /**
     * Mod presence, read during mod construction - the moment a decision about
     * registration has to be made, which is earlier than any config exists.
     * A null mod list cannot happen this late; if it somehow did, registering the
     * tier is the safer failure, since an extra obtainable item beats a missing
     * one.
     */
    private static boolean silverSourcePresent() {
        ModList modList = ModList.get();
        return modList == null
                || modList.isLoaded("iceandfire")
                || modList.isLoaded("tensura");
    }

    public static void register() {
    }
}
