package org.bitwisemadness.pseudocore.registry;

import com.mojang.serialization.MapCodec;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.config.PseudocoreConfig;
import org.bitwisemadness.pseudocore.data.condition.IntegrationActiveCondition;

/**
 * The mod's datapack condition types.
 *
 * <p>One condition per integration, each answering "is the mod installed
 * <b>and</b> is that integration switched on" in {@code pseudocore-common.toml}.
 * Data files opt in with a bare
 * {@code {"type": "pseudocore:tensura_active"}} entry, so turning an integration
 * off removes its recipes, advancements and overrides exactly as if the mod were
 * not installed - which is the point of the switches.</p>
 *
 * <p>Registration runs on the mod bus from the main mod class, so the conditions
 * exist before any data pack is parsed.</p>
 */
public final class ModConditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Pseudocore.MOD_ID);

    /**
     * The shared condition <b>instances</b>, not just their codecs: datagen
     * (#116) serializes a condition by looking its codec up in
     * {@code NeoForgeRegistries.CONDITION_CODECS} by value identity, so the
     * provider must emit <i>this</i> object - a fresh
     * {@code new IntegrationActiveCondition(...)} would carry a different
     * unit codec and fail the lookup. The supplier stays lazy: it is only
     * consulted when a data pack is actually evaluated, never at
     * construction or serialization.
     */
    public static final IntegrationActiveCondition TENSURA_ACTIVE = new IntegrationActiveCondition(
            "tensura_active", () -> isLoaded("tensura") && PseudocoreConfig.tensuraIntegrationEnabled());

    /** See {@link #TENSURA_ACTIVE}: the shared instance the codecs register from. */
    public static final IntegrationActiveCondition ICE_AND_FIRE_ACTIVE = new IntegrationActiveCondition(
            "iceandfire_active", () -> isLoaded("iceandfire") && PseudocoreConfig.iceAndFireIntegrationEnabled());

    static {
        CONDITION_CODECS.register("tensura_active", () -> TENSURA_ACTIVE.codec());
        CONDITION_CODECS.register("iceandfire_active", () -> ICE_AND_FIRE_ACTIVE.codec());
    }

    private ModConditions() {
    }

    /**
     * Mod presence, consulted lazily. {@code ModList} is null only before mod
     * loading, which is earlier than any condition is ever evaluated.
     */
    private static boolean isLoaded(String modId) {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(modId);
    }
}
