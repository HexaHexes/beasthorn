package org.bitwisemadness.beasthorn.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;

/**
 * NBT save/load helpers for the capture and release paths.
 *
 * <p>These mirror Ice and Fire CE's {@code EntityDataHelper} — the exact
 * utility their Dragon Horn uses — because it is the proven shape for "store a
 * whole entity in an item, put it back later". We reimplement rather than call
 * it so the addon has no runtime link to IaF CE.</p>
 */
public final class EntityDataHelper {

    private EntityDataHelper() {
    }

    /**
     * Capture: the entity's full NBT with the {@code id} key stripped, so the
     * tag can be loaded back onto a freshly created entity of the same type.
     */
    public static CompoundTag saveWithoutId(Entity entity) {
        return entity.saveWithoutId(new CompoundTag());
    }

    /**
     * Release: load the stored NBT onto an already-created entity. This is
     * called for <b>every</b> entity type, which is the single difference that
     * makes the Beast Horn generic where IaF's Dragon Horn is gated to
     * {@code DragonBaseEntity}.
     */
    public static void load(Entity entity, CompoundTag tag) {
        if (entity != null && tag != null) {
            entity.load(tag);
        }
    }

    /** Whether the tag looks like it holds a living entity worth showing stats for. */
    public static boolean hasHealth(CompoundTag tag) {
        return tag != null && tag.contains("Health");
    }

    public static float readHealth(CompoundTag tag) {
        return tag == null ? 0.0F : tag.getFloat("Health");
    }

    public static float readMaxHealth(LivingEntity entity) {
        return entity == null ? 0.0F : entity.getMaxHealth();
    }
}
