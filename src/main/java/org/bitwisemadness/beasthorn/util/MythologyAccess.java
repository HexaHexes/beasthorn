package org.bitwisemadness.beasthorn.util;

import net.pixeldreamstudios.mobs_of_mythology.entity.variant.DrakeVariant;
import net.pixeldreamstudios.mobs_of_mythology.entity.variant.SporelingVariant;

import java.util.Optional;

/**
 * The only class in this addon that imports a Mobs of Mythology type.
 *
 * <p>Never reference this directly — always go through {@link VariantBridge},
 * which gates the call on ModList. Loading this class without Mobs of Mythology
 * installed would throw {@link NoClassDefFoundError}. That gate is not
 * belt-and-braces: the entity-id switch in {@code VariantHelper} is normally
 * unreachable without the mod, but a horn is a portable item, so one captured
 * under Mobs of Mythology and hovered after its removal would otherwise reach
 * here and crash.</p>
 */
final class MythologyAccess {

    private MythologyAccess() {
    }

    /**
     * The drake's skin ordinal.
     *
     * <p>MoM never names these — the enum is bare {@code DRAKE_1..DRAKE_7} with
     * a {@code byId} and nothing else, and its lang file has no keys for them.
     * The readable names are supplied by {@link VariantHelper}, which owns the
     * mapping because it is presentation rather than data. Note the ids are
     * <b>zero-based</b>: {@code DRAKE_1} is id 0.</p>
     */
    static Optional<String> drakeVariant(int id) {
        return VariantNames.lookup(DrakeVariant.values(), id, DrakeVariant::byId);
    }

    static Optional<String> sporelingVariant(int id) {
        return VariantNames.lookup(SporelingVariant.values(), id, SporelingVariant::byId);
    }
}
