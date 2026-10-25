package org.bitwisemadness.pseudocore.util;

import io.github.Memoires.mysticism.entity.variant.OkamiVariant;

import java.util.Optional;

/**
 * The only class in this addon that imports a Mysticism type.
 *
 * <p>Never reference this directly — always go through {@link VariantBridge},
 * which gates the call on ModList. Loading this class without Mysticism
 * installed would throw {@link NoClassDefFoundError}. Mysticism itself depends
 * on Tensura, but that is no reason to assume it is present alongside it, so it
 * gets its own gate rather than borrowing Tensura's.</p>
 */
final class MysticismAccess {

    private MysticismAccess() {
    }

    static Optional<String> okamiVariant(int id) {
        return VariantNames.lookup(OkamiVariant.values(), id, OkamiVariant::byId);
    }
}
