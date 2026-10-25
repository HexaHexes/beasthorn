package org.bitwisemadness.pseudocore.registry;

import net.minecraft.world.level.GameRules;

/**
 * The two ownership-transfer gamerules. Both default to {@code false}.
 *
 * <p>Defaulting off is deliberate and is a safety property, not a
 * convenience: with both rules off the Pseudocore is purely a storage and
 * trading item. Releasing a mob leaves its owner untouched, so a horn that
 * was stolen or bought cannot re-home someone else's pet. A server admin has
 * to explicitly opt in with {@code /gamerule} before any ownership moves.</p>
 *
 * <p>Registration must happen inside {@code FMLCommonSetupEvent#enqueueWork}
 * because {@link GameRules#register} mutates a static map that vanilla reads
 * during game construction.</p>
 */
public final class ModGameRules {

    /**
     * When true, releasing a mob from a Pseudocore makes the releasee that
     * mob's owner. Independent of the dragon rule so a server can enable one
     * without the other.
     */
    public static GameRules.Key<GameRules.BooleanValue> PSEUDOCORE_OWNERSHIP_TRANSFER;

    /**
     * When true, releasing a dragon from Ice and Fire CE's own Dragon Horn
     * makes the releasee that dragon's owner. Applied via a mixin that only
     * loads when Ice and Fire CE is present.
     */
    public static GameRules.Key<GameRules.BooleanValue> DRAGON_HORN_OWNERSHIP_TRANSFER;

    private ModGameRules() {
    }

    /** Called from {@code FMLCommonSetupEvent#enqueueWork}. */
    public static void register() {
        PSEUDOCORE_OWNERSHIP_TRANSFER = GameRules.register(
                "pseudocoreOwnershipTransfer", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false));
        DRAGON_HORN_OWNERSHIP_TRANSFER = GameRules.register(
                "pseudocoreDragonOwnershipTransfer", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false));
    }

    /** Null-safe read, since the keys are only assigned during common setup. */
    public static boolean isPseudocoreTransferEnabled(net.minecraft.world.level.Level level) {
        return PSEUDOCORE_OWNERSHIP_TRANSFER != null
                && level.getGameRules().getBoolean(PSEUDOCORE_OWNERSHIP_TRANSFER);
    }

    /** Null-safe read, since the keys are only assigned during common setup. */
    public static boolean isDragonHornTransferEnabled(net.minecraft.world.level.Level level) {
        return DRAGON_HORN_OWNERSHIP_TRANSFER != null
                && level.getGameRules().getBoolean(DRAGON_HORN_OWNERSHIP_TRANSFER);
    }
}
