package org.bitwisemadness.beasthorn.util;

import net.neoforged.fml.ModList;
import org.bitwisemadness.beasthorn.BeastHorn;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Facade for every int-ordinal variant the pack's mods use.
 *
 * <p>This class deliberately contains <b>no</b> reference to any Tensura, Mobs
 * of Mythology or Mysticism type. Each method checks its owning mod through
 * {@link ModList} and only then delegates to an access class — {@link
 * TensuraAccess}, {@link MythologyAccess} or {@link MysticismAccess} — that
 * does import it. Because the JVM resolves a class's references lazily, those
 * access classes are never loaded, and so never try to resolve missing types,
 * when their mod is absent.</p>
 *
 * <p>That gate is load-bearing rather than defensive. The entity-id switch in
 * {@link VariantHelper} can only be reached by a mob of that exact type, which
 * requires the mod — except that a horn is a <b>portable, storable item</b>. A
 * horn captured while Mobs of Mythology was installed and hovered after it was
 * removed reaches the branch anyway, and without the gate that is a
 * {@link NoClassDefFoundError} on a tooltip. Capture, transport and trade
 * across mod-list changes is precisely what this addon is for.</p>
 */
public final class VariantBridge {

    private VariantBridge() {
    }

    /** Tensura's direwolf. The species word is in the name itself. */
    public static Optional<String> direwolf(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.direwolfVariant(id));
    }

    /** The slime's colour. Its {@code Type} key is a separate axis, unused. */
    public static Optional<String> slime(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.slimeColor(id));
    }

    public static Optional<String> basilisk(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.basiliskVariant(id));
    }

    public static Optional<String> boneGolem(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.boneGolemVariant(id));
    }

    public static Optional<String> cattledeer(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.cattledeerVariant(id));
    }

    public static Optional<String> hornedRabbit(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.hornedRabbitVariant(id));
    }

    public static Optional<String> houndDog(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.houndDogVariant(id));
    }

    public static Optional<String> hoverLizard(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.hoverLizardVariant(id));
    }

    public static Optional<String> landfish(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.landfishVariant(id));
    }

    public static Optional<String> leechLizard(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.leechLizardVariant(id));
    }

    public static Optional<String> lizardman(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.lizardmanVariant(id));
    }

    public static Optional<String> oneEyedOwl(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.oneEyedOwlVariant(id));
    }

    public static Optional<String> orc(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.orcVariant(id));
    }

    public static Optional<String> dragonPeacock(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.dragonPeacockVariant(id));
    }

    public static Optional<String> phantaspore(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.phantasporeVariant(id));
    }

    public static Optional<String> skeleton(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.skeletonVariant(id));
    }

    public static Optional<String> zombie(int id) {
        return guarded(TensuraBridge::isLoaded, () -> TensuraAccess.zombieVariant(id));
    }

    /**
     * The drake's skin. MoM never names these, so {@link VariantHelper} owns
     * the readable table and calls this only as the fallback for an id that
     * table does not cover.
     */
    public static Optional<String> drake(int id) {
        return guarded(() -> ModList.get().isLoaded("mobs_of_mythology"),
                () -> MythologyAccess.drakeVariant(id));
    }

    public static Optional<String> sporeling(int id) {
        return guarded(() -> ModList.get().isLoaded("mobs_of_mythology"),
                () -> MythologyAccess.sporelingVariant(id));
    }

    public static Optional<String> okami(int id) {
        return guarded(() -> ModList.get().isLoaded("mysticism"),
                () -> MysticismAccess.okamiVariant(id));
    }

    private static Optional<String> guarded(BooleanSupplier modLoaded, Supplier<Optional<String>> work) {
        if (!modLoaded.getAsBoolean()) return Optional.empty();
        try {
            return work.get();
        } catch (Throwable t) {
            // A tag from another mod build, or a resolver that has moved.
            BeastHorn.LOGGER.debug("Variant lookup failed", t);
            return Optional.empty();
        }
    }
}
