package org.bitwisemadness.beasthorn.util;

import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * The only class in this addon that imports Tensura or ManasCore types.
 *
 * <p>Never reference this class directly — always go through
 * {@link TensuraBridge}, which gates every call on ModList so this class is
 * only loaded when Tensura is actually installed. Loading it without Tensura
 * would throw {@link NoClassDefFoundError}.</p>
 */
final class TensuraAccess {

    private TensuraAccess() {
    }

    /**
     * Read ExistenceStorage off any LivingEntity via the ManasCore mixin. This
     * is the same access pattern used by tensura_minecolonies' working code.
     */
    static ExistenceStorage readExistence(LivingEntity entity) {
        if (entity instanceof StorageHolder holder) {
            return holder.manasCore$getStorage(ExistenceStorage.getKey());
        }
        return null;
    }

    static Optional<ResourceLocation> existenceStorageId() {
        return Optional.of(ExistenceStorage.getKey().id());
    }

    /**
     * EP is aura + magicule (see Tensura's own getEP). Read from NBT so the
     * tooltip can show it without a live entity.
     */
    static Optional<Double> readEpFromNbt(CompoundTag entityData) {
        CompoundTag exist = existenceCompound(entityData);
        if (exist == null) return Optional.empty();
        if (!exist.contains("aura") && !exist.contains("magicule")) return Optional.empty();
        double aura = exist.getDouble("aura");
        double magicule = exist.getDouble("magicule");
        return Optional.of(aura + magicule);
    }

    /**
     * Tensura writes the owner to both permanentOwner and temporaryOwner. We
     * read permanent first since it is the durable, naming-granted one.
     */
    static Optional<UUID> readOwnerFromNbt(CompoundTag entityData) {
        CompoundTag exist = existenceCompound(entityData);
        if (exist == null) return Optional.empty();
        if (exist.hasUUID("permanentOwner")) return Optional.of(exist.getUUID("permanentOwner"));
        if (exist.hasUUID("temporaryOwner")) return Optional.of(exist.getUUID("temporaryOwner"));
        return Optional.empty();
    }

    static boolean hasOwner(LivingEntity entity) {
        ExistenceStorage exist = readExistence(entity);
        if (exist == null) return false;
        return exist.getPermanentOwner() != null || exist.getTemporaryOwner() != null;
    }

    static Optional<UUID> readOwner(LivingEntity entity) {
        ExistenceStorage exist = readExistence(entity);
        if (exist == null) return Optional.empty();
        if (exist.getPermanentOwner() != null) return Optional.of(exist.getPermanentOwner());
        if (exist.getTemporaryOwner() != null) return Optional.of(exist.getTemporaryOwner());
        return Optional.empty();
    }

    static boolean transferOwnership(LivingEntity entity, UUID newOwner) {
        ExistenceStorage exist = readExistence(entity);
        if (exist == null) return false;
        exist.setPermanentOwner(newOwner);
        exist.setTemporaryOwner(newOwner);
        exist.markDirty();
        return true;
    }

    /**
     * Dig the ExistenceStorage compound out of the captured entity NBT. It
     * lives under ManasCoreStorage (written by ManasCore's MixinEntity), keyed
     * by the storage's resource id. We resolve that id from the API rather
     * than hardcoding it, so a Tensura key rename cannot silently break us.
     */
    private static CompoundTag existenceCompound(CompoundTag entityData) {
        if (!entityData.contains("ManasCoreStorage")) return null;
        CompoundTag all = entityData.getCompound("ManasCoreStorage");
        ResourceLocation id = ExistenceStorage.getKey().id();
        String key = id.toString();
        if (!all.contains(key)) return null;
        return all.getCompound(key);
    }
}
