package org.bitwisemadness.pseudocore.registry;

import com.mojang.serialization.MapCodec;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
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

    /** True while Tensura is installed and its integration is switched on. */
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<? extends ICondition>> TENSURA_ACTIVE =
            register("tensura_active", "tensura", PseudocoreConfig::tensuraIntegrationEnabled);

    /** True while Ice and Fire CE is installed and its integration is switched on. */
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<? extends ICondition>>
            ICE_AND_FIRE_ACTIVE =
            register("iceandfire_active", "iceandfire", PseudocoreConfig::iceAndFireIntegrationEnabled);

    private ModConditions() {
    }

    private static DeferredHolder<MapCodec<? extends ICondition>, MapCodec<? extends ICondition>> register(
            String name, String modId, java.util.function.BooleanSupplier enabled) {
        return CONDITION_CODECS.register(name, () -> new IntegrationActiveCondition(name,
                () -> isLoaded(modId) && enabled.getAsBoolean()).codec());
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
