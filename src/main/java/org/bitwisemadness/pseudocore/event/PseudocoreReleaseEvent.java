package org.bitwisemadness.pseudocore.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Posted after a core successfully released its mob into the world.
 *
 * <p>This is the one integration point for anything that needs to react to a
 * release - pseudocore-curios stamps its core-to-mob linkage here rather than
 * mixing into three separate release paths. All three fire it after the spawn
 * succeeded and the core's storage was stripped:
 * {@code PseudocoreItem.releaseContained} (beast block-click and the addon
 * hotkey), {@code IafHornAccess.releaseDragonAt} (the addon hotkey's dragon
 * path), and the Dragon Horn mixin (Ice and Fire's own dragon block-click).</p>
 *
 * <p>Posted on {@link NeoForge#EVENT_BUS} (the game bus), so consumers use the
 * ordinary {@code @SubscribeEvent} pattern. Server-side only - every release
 * path runs server-side by construction. Not cancellable: the mob is already
 * in the world; this event observes, it does not gate.</p>
 *
 * @param stack    the released core, its storage already stripped - observers
 *                 may stamp it (the stack object is the live item stack)
 * @param released the spawned mob, alive in the world
 * @param level    where it spawned
 * @param position the spawn position
 * @param releaser who released it, or null when released without a player
 *                 context (dispenser-style)
 */
public final class PseudocoreReleaseEvent extends Event {

    private final ItemStack stack;
    private final Entity released;
    private final ServerLevel level;
    private final Vec3 position;
    /** Null when released without a player context (dispenser-style). */
    private final Player releaser;

    public PseudocoreReleaseEvent(ItemStack stack, Entity released, ServerLevel level,
                                  Vec3 position, Player releaser) {
        this.stack = stack;
        this.released = released;
        this.level = level;
        this.position = position;
        this.releaser = releaser;
    }

    public ItemStack stack() {
        return stack;
    }

    public Entity released() {
        return released;
    }

    public ServerLevel level() {
        return level;
    }

    public Vec3 position() {
        return position;
    }

    public Player releaser() {
        return releaser;
    }
}
