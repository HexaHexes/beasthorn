package org.bitwisemadness.beasthorn.util;

import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Shared lookup for the int-ordinal variant enums that several mods use.
 *
 * <p>Every species in this family exposes the same shape — a {@code byId(int)}
 * resolver over an array indexed by the stored id — so the bounds handling
 * belongs in exactly one place. It must, because that array is indexed by the
 * id itself: an out-of-range tag throws
 * {@link ArrayIndexOutOfBoundsException} rather than returning null, and a
 * horn is a portable item, so a tag written by a newer mod build is reachable.
 * </p>
 *
 * <p>This class deliberately imports no mod type. Its only bounds are
 * {@code Enum} and {@code Optional}, which is what lets it sit in the middle of
 * three different mods' access classes without dragging any of them in.</p>
 */
final class VariantNames {

    private VariantNames() {
    }

    /**
     * The enum constant {@code byId} names, or empty when the id is outside the
     * enum's own table.
     *
     * @param table the enum's {@code values()}, used only for its length
     * @param id    the id read from NBT, which may be any int at all
     * @param byId  the enum's own resolver
     * @return the constant's {@link Enum#name()}, or empty
     */
    static <T extends Enum<T>> Optional<String> lookup(T[] table, int id, IntFunction<T> byId) {
        if (id < 0 || id >= table.length) return Optional.empty();
        T value = byId.apply(id);
        return value == null ? Optional.empty() : Optional.of(value.name());
    }
}
