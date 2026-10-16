package org.bitwisemadness.beasthorn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.bitwisemadness.beasthorn.config.BeastHornConfig;
import org.bitwisemadness.beasthorn.item.component.ContainedMobComponent;
import org.bitwisemadness.beasthorn.registry.ModDataComponents;
import org.bitwisemadness.beasthorn.registry.ModGameRules;
import org.bitwisemadness.beasthorn.util.EntityDataHelper;
import org.bitwisemadness.beasthorn.util.TensuraBridge;
import org.bitwisemadness.beasthorn.util.TooltipHelper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Beast Horn: captures any mob with its full NBT and releases it later.
 *
 * <p>This is a generalized Ice and Fire Dragon Horn. IaF's version gates both
 * capture and release to {@code DragonBaseEntity}; the only reason the Beast
 * Horn is generic is that it drops both of those gates. Capture is guarded so
 * you cannot steal someone's pet; release is where ownership may transfer, and
 * only when a server admin has enabled the gamerule.</p>
 */
public class BeastHornItem extends Item {

    public BeastHornItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // Capture
    // ------------------------------------------------------------------

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Never capture players.
        if (target instanceof Player) {
            return InteractionResult.PASS;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (BeastHornConfig.COMMON.isBlacklisted(id)) {
            player.displayClientMessage(
                    Component.literal("The " + id.getPath() + " is too powerful to be captured by this horn.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.PASS;
        }

        // Ownership guard: allow if the mob has no owner, or the clicker is the
        // owner. This is the anti-theft rule and is intentionally not gameruled.
        if (!canCapture(player, target)) {
            player.displayClientMessage(
                    Component.literal("This creature is owned by another player.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.PASS;
        }

        // Refuse mobs that are leashed or carrying passengers - releasing those
        // would strand a leash or duplicate/passenger-lock a rider.
        if (isLeashed(target) || !target.getPassengers().isEmpty()) {
            player.displayClientMessage(
                    Component.literal("This creature cannot be captured right now.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.PASS;
        }

        CompoundTag data = EntityDataHelper.saveWithoutId(target);
        UUID uuid = target.getUUID();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());

        stack.set(ModDataComponents.CONTAINED_MOB.get(),
                new ContainedMobComponent(typeId, uuid, data));

        // Remove the mob from the world only after the data is safely stored.
        target.discard();

        return InteractionResult.SUCCESS;
    }

    /**
     * Capture is allowed when the mob is ownerless, or when this player is its
     * owner. Checked across vanilla taming, Tensura ownership, and
     * Redomesticate's ownership layer.
     */
    private boolean canCapture(Player player, LivingEntity target) {
        // Vanilla tamed animals.
        if (target instanceof TamableAnimal tamable) {
            UUID owner = tamable.getOwnerUUID();
            if (owner != null && !owner.equals(player.getUUID())) {
                return false;
            }
        }

        // Tensura ownership - its owner is authoritative when present.
        if (TensuraBridge.isLoaded()) {
            Optional<UUID> tensuraOwner = readTensuraOwner(target);
            if (tensuraOwner.isPresent() && !tensuraOwner.get().equals(player.getUUID())) {
                return false;
            }
        }

        return true;
    }

    private static Optional<UUID> readTensuraOwner(LivingEntity target) {
        return TensuraBridge.readOwner(target);
    }

    private static boolean isLeashed(LivingEntity target) {
        // Leash state is exposed through Leashable, which Mob implements.
        return target instanceof net.minecraft.world.entity.Leashable leashable
                && leashable.isLeashed();
    }

    // ------------------------------------------------------------------
    // Release
    // ------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        ContainedMobComponent comp = stack.get(ModDataComponents.CONTAINED_MOB.get());
        if (comp == null) {
            return InteractionResult.PASS;
        }

        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(comp.entityType()).orElse(null);
        if (type == null) {
            return InteractionResult.PASS;
        }

        Entity entity = type.create(serverLevel);
        if (entity == null) {
            return InteractionResult.PASS;
        }

        // Restore the mob's full state. Unlike IaF's Dragon Horn this is done
        // for every entity type - that single missing gate is what makes the
        // Beast Horn generic.
        EntityDataHelper.load(entity, comp.entityData());

        // Restore the original UUID so anything bound to this mob by UUID
        // (Redomesticate deeds and the like) still resolves.
        UUID stored = comp.entityUuid();
        if (stored != null) {
            entity.setUUID(stored);
        }

        entity.moveTo(context.getClickedPos().getCenter(), 0.0F, 0.0F);

        // Ownership transfer - only if the server opted in AND the releaser is
        // not already the owner.
        Player player = context.getPlayer();
        boolean transfer = player != null
                && ModGameRules.isBeastHornTransferEnabled(serverLevel)
                && entity instanceof LivingEntity;

        // Set before addFreshEntity so the spawn packet already carries it, and
        // re-asserted below afterwards: TamableAnimal caches its owner in a
        // field refreshed by reassignOwner(), and a pre-spawn setOwnerUUID
        // cannot resolve the UUID to an online player yet. Doing both makes
        // getControllingPassenger() (i.e. riding) work on the very tick the
        // mob appears rather than one tick later.
        if (transfer) {
            transferOwnership((LivingEntity) entity, player);
        }

        if (serverLevel.addFreshEntity(entity)) {
            stack.remove(ModDataComponents.CONTAINED_MOB.get());
            if (transfer) {
                transferOwnership((LivingEntity) entity, player);
            }
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Point the released mob's ownership at the releasee.
     *
     * <p>Tensura's owner is written first because it is the authoritative one
     * on this pack, then vanilla's, matching the two-call pattern already used
     * successfully in the field. TamableAnimal caches its owner in a field
     * refreshed by reassignOwner(), so vanilla is set after Tensura in case the
     * cache needs the UUID in place first.</p>
     */
    private void transferOwnership(LivingEntity entity, Player player) {
        UUID newOwner = player.getUUID();

        if (TensuraBridge.isLoaded()) {
            TensuraBridge.transferOwnership(entity, newOwner);
        }

        if (entity instanceof TamableAnimal tamable) {
            tamable.setOwnerUUID(newOwner);
        }
    }

    // ------------------------------------------------------------------
    // Tooltip
    // ------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ContainedMobComponent comp = stack.get(ModDataComponents.CONTAINED_MOB.get());
        if (comp == null) {
            tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Right-click a mob to capture it.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        TooltipHelper.addLines(tooltip, comp, context.registries());
    }
}
