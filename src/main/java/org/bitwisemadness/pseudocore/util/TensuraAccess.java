package org.bitwisemadness.pseudocore.util;

import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.manascore.storage.api.StorageKey;
import io.github.manasmods.tensura.entity.variant.BasiliskVariant;
import io.github.manasmods.tensura.entity.variant.BoneGolemVariant;
import io.github.manasmods.tensura.entity.variant.CattledeerVariant;
import io.github.manasmods.tensura.entity.variant.DirewolfVariant;
import io.github.manasmods.tensura.entity.variant.HornedRabbitVariant;
import io.github.manasmods.tensura.entity.variant.HoundDogVariant;
import io.github.manasmods.tensura.entity.variant.HoverLizardVariant;
import io.github.manasmods.tensura.entity.variant.LandfishVariant;
import io.github.manasmods.tensura.entity.variant.LeechLizardVariant;
import io.github.manasmods.tensura.entity.variant.LizardmanVariant;
import io.github.manasmods.tensura.entity.variant.OneEyedOwlVariant;
import io.github.manasmods.tensura.entity.variant.OrcVariant;
import io.github.manasmods.tensura.entity.variant.PeacockVariant;
import io.github.manasmods.tensura.entity.variant.PhantasporeVariant;
import io.github.manasmods.tensura.entity.variant.SkeletonVariant;
import io.github.manasmods.tensura.entity.variant.SlimeColor;
import io.github.manasmods.tensura.entity.variant.ZombieVariant;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

    /** ManasCore's per-entity storage root, written by its MixinEntity. */
    private static final String STORAGE_ROOT = "ManasCoreStorage";
    /**
     * Inside that root, storages live in a <b>list</b> keyed by this name — not
     * in a compound keyed by storage id, which is what an earlier version of
     * this class assumed and why EP never showed.
     */
    private static final String STORAGE_LIST = "manascore_registry_storage";
    /** Each list element names the storage it belongs to with this string. */
    private static final String STORAGE_ID = "manascore_registry_storage_id";

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

    /**
     * Live EP - Tensura's own {@code getEP()}, aura plus magicule. The live
     * storage rather than NBT, since the caller has the entity in hand.
     */
    static Optional<Double> readEp(LivingEntity entity) {
        ExistenceStorage exist = readExistence(entity);
        if (exist == null) return Optional.empty();
        return Optional.of(exist.getEP());
    }

    /**
     * Whether Tensura's naming system has given this mob a name - not a vanilla
     * custom name. {@code ExistenceStorage#getName} is what Tensura's own naming
     * flow writes.
     */
    static boolean isNamed(LivingEntity entity) {
        ExistenceStorage exist = readExistence(entity);
        if (exist == null) return false;
        String name = exist.getName();
        return name != null && !name.isBlank();
    }

    static Optional<UUID> readOwner(LivingEntity entity) {
        ExistenceStorage exist = readExistence(entity);
        if (exist == null) return Optional.empty();
        if (exist.getPermanentOwner() != null) return Optional.of(exist.getPermanentOwner());
        if (exist.getTemporaryOwner() != null) return Optional.of(exist.getTemporaryOwner());
        return Optional.empty();
    }

    /**
     * Variant names for the seventeen Tensura species that store an int
     * ordinal under the {@code Variant} key.
     *
     * <p>All of these go through {@link Enum#name()} rather than the
     * {@code getName()} some of them also expose. For most species the two are
     * the same word in different cases, but Phantaspore's {@code getName()}
     * returns a <b>texture path</b> ({@code phantaspore_red}), which would
     * reach the tooltip as "Phantaspore Red Phantaspore". The constant name is
     * the display name everywhere here.</p>
     *
     * <p>Bounds are handled by {@link VariantNames} because {@code byId}
     * indexes its array directly and would throw on a tag from another mod
     * build.</p>
     */
    static Optional<String> direwolfVariant(int id) {
        return VariantNames.lookup(DirewolfVariant.values(), id, DirewolfVariant::byId);
    }

    static Optional<String> slimeColor(int id) {
        return VariantNames.lookup(SlimeColor.values(), id, SlimeColor::byId);
    }

    static Optional<String> basiliskVariant(int id) {
        return VariantNames.lookup(BasiliskVariant.values(), id, BasiliskVariant::byId);
    }

    static Optional<String> boneGolemVariant(int id) {
        return VariantNames.lookup(BoneGolemVariant.values(), id, BoneGolemVariant::byId);
    }

    static Optional<String> cattledeerVariant(int id) {
        return VariantNames.lookup(CattledeerVariant.values(), id, CattledeerVariant::byId);
    }

    static Optional<String> hornedRabbitVariant(int id) {
        return VariantNames.lookup(HornedRabbitVariant.values(), id, HornedRabbitVariant::byId);
    }

    static Optional<String> houndDogVariant(int id) {
        return VariantNames.lookup(HoundDogVariant.values(), id, HoundDogVariant::byId);
    }

    static Optional<String> hoverLizardVariant(int id) {
        return VariantNames.lookup(HoverLizardVariant.values(), id, HoverLizardVariant::byId);
    }

    static Optional<String> landfishVariant(int id) {
        return VariantNames.lookup(LandfishVariant.values(), id, LandfishVariant::byId);
    }

    static Optional<String> leechLizardVariant(int id) {
        return VariantNames.lookup(LeechLizardVariant.values(), id, LeechLizardVariant::byId);
    }

    static Optional<String> lizardmanVariant(int id) {
        return VariantNames.lookup(LizardmanVariant.values(), id, LizardmanVariant::byId);
    }

    static Optional<String> oneEyedOwlVariant(int id) {
        return VariantNames.lookup(OneEyedOwlVariant.values(), id, OneEyedOwlVariant::byId);
    }

    static Optional<String> orcVariant(int id) {
        return VariantNames.lookup(OrcVariant.values(), id, OrcVariant::byId);
    }

    static Optional<String> dragonPeacockVariant(int id) {
        return VariantNames.lookup(PeacockVariant.values(), id, PeacockVariant::byId);
    }

    static Optional<String> phantasporeVariant(int id) {
        return VariantNames.lookup(PhantasporeVariant.values(), id, PhantasporeVariant::byId);
    }

    static Optional<String> skeletonVariant(int id) {
        return VariantNames.lookup(SkeletonVariant.values(), id, SkeletonVariant::byId);
    }

    static Optional<String> zombieVariant(int id) {
        return VariantNames.lookup(ZombieVariant.values(), id, ZombieVariant::byId);
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
     * Dig the ExistenceStorage compound out of the captured entity NBT.
     *
     * <p>ManasCore nests storages two levels deep and holds them in a
     * <b>list</b>, not a compound keyed by storage id. {@code MixinEntity}
     * writes the {@code ManasCoreStorage} compound; {@code CombinedStorage#toNBT}
     * puts a {@code manascore_registry_storage} ListTag inside it; each element
     * then carries a {@code manascore_registry_storage_id} string alongside the
     * storage's own fields. {@code ExistenceStorage#save} writes aura, magicule
     * and both owner UUIDs directly into that element, so they are read straight
     * off the matched entry rather than from anything nested further.</p>
     */
    private static CompoundTag existenceCompound(CompoundTag entityData) {
        if (!entityData.contains(STORAGE_ROOT, Tag.TAG_COMPOUND)) return null;
        CompoundTag all = entityData.getCompound(STORAGE_ROOT);
        if (!all.contains(STORAGE_LIST, Tag.TAG_LIST)) return null;

        // Resolved from the API rather than hardcoded, so a Tensura key rename
        // cannot silently break us. Assigned from a registration event, so it
        // can still be null this early - guard rather than lean on the bridge.
        StorageKey<ExistenceStorage> key = ExistenceStorage.getKey();
        if (key == null) return null;
        String wanted = key.id().toString();

        ListTag list = all.getList(STORAGE_LIST, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (wanted.equals(entry.getString(STORAGE_ID))) return entry;
        }
        return null;
    }
}
