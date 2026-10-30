package org.bitwisemadness.pseudocore.util;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.component.DragonHornComponent;
import java.util.UUID;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.bitwisemadness.pseudocore.item.DragonPseudocoreItem;
import org.bitwisemadness.pseudocore.registry.ModGameRules;

/**
 * Reads and releases Ice and Fire's Dragon Horn storage.
 *
 * <p>Never reference IaF types outside this class - every entry point lives
 * here: the mixin paths (capture relaxation, tooltip, stage), and now
 * {@link #releaseDragonAt}, which the Curios addon's release keybind calls
 * (#91). That call only ever happens while a dragon core exists in a slot,
 * which means Ice and Fire is installed, so nothing here loads without IaF.</p>
 *
 * <p>Works by scanning the stack's components for a {@link DragonHornComponent}
 * value rather than looking IaF's {@code DRAGON_HORN} field up by name. The
 * field approach was tried first and failed silently: its static type differs
 * across CE builds (an Architectury {@code RegistrySupplier} in 2.0-beta.15, a
 * NeoForge {@code DeferredHolder} in 2.1.4), so a direct reference cannot be
 * compiled safely, and a reflective lookup funnels every possible failure into
 * one swallowed exception that produced no diagnostic at all.</p>
 *
 * <p>Scanning for the value type sidesteps all of it: no field, no descriptor,
 * no version coupling. If a Dragon Horn holds a dragon, one of its components
 * <i>is</i> a {@code DragonHornComponent}, and matching on the value is exact
 * rather than name-based.</p>
 */
public final class IafHornAccess {

    private IafHornAccess() {
    }

    /**
     * The stored dragon, or null when this stack is an empty Dragon Horn.
     *
     * <p>Because this mixin only ever runs on IaF's {@code DragonHornItem}, a
     * null return unambiguously means "no dragon stored" rather than "not a
     * Dragon Horn", so callers can treat it as the empty case.</p>
     */
    public static DragonHornComponent read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        // ItemStack.getComponents() is a DataComponentMap, which is Iterable over
        // its TypedDataComponent entries - the full set, not just the patch.
        for (TypedDataComponent<?> component : stack.getComponents()) {
            if (component.value() instanceof DragonHornComponent horn) {
                return horn;
            }
        }
        return null;
    }

    /**
     * The raw stored type id of a captured dragon, or null when the stack holds
     * no dragon.
     *
     * <p>The one IaF-aware read the client needs for #65. The beast line keeps
     * its captured mob in {@code pseudocore:contained_mob}, but a dragon core
     * extends IaF's horn and stores it in IaF's own component instead, so every
     * "is this core filled?" question has to ask both stores or it answers
     * "empty" for every dragon.</p>
     *
     * <p>Callers get the raw {@link ResourceLocation} rather than a
     * {@link DragonHornComponent}, so no client class has to import an IaF type
     * to use this - that is the whole point of concentrating the read here.</p>
     */
    public static ResourceLocation storedDragonType(ItemStack stack) {
        DragonHornComponent horn = read(stack);
        return horn == null ? null : horn.entityType();
    }

    /**
     * The raw stored entity data of a captured dragon, or null when the stack
     * holds no dragon.
     *
     * <p>Second half of the same #65 read: the orb colour is computed from the
     * frozen NBT (element plus the {@code Variant} string), so a caller that
     * has the type id needs the tag alongside it. Returned as IaF writes it,
     * with no normalisation - the beast line's component carries the same shape
     * under {@code entityData()}.</p>
     */
    public static CompoundTag storedDragonData(ItemStack stack) {
        DragonHornComponent horn = read(stack);
        return horn == null ? null : horn.entityData();
    }

    /**
     * The stored dragon's own UUID, or null when the stack holds no dragon.
     *
     * <p>Only needed to satisfy the shared record's shape - the orb colour is
     * derived from the element and the stored {@code Variant} string, never from
     * the UUID - but the component carries it and the release path restores it,
     * so it is read from the same single lookup rather than faked.</p>
     */
    public static UUID storedDragonUuid(ItemStack stack) {
        DragonHornComponent horn = read(stack);
        return horn == null ? null : horn.entityUuid();
    }

    /**
     * Release this stack's stored dragon at a position (#91, addon Phase 1):
     * {@code DragonHornItem.useOn} replicated from the 9128508 bytecode, minus
     * the block context - resolve the type, create the entity, load the frozen
     * NBT through {@code DragonBaseEntity.load}, restore the stored UUID,
     * position it, spawn it, and strip the horn component on success.
     *
     * <p>Ownership mirrors {@code DragonHornItemMixin}'s injection exactly -
     * the gamerule-gated {@code setTame} + {@code tame} sequence, run before
     * the spawn and skipped when the releasee already owns the dragon - so
     * releasing from a Curios slot (#91) behaves like releasing against a
     * block. That logic lives here rather than in the mixin because the mixin
     * only wraps IaF's own {@code useOn}.</p>
     *
     * <p>The strip <b>must not</b> name {@code IafDataComponents.DRAGON_HORN}:
     * the pack runs CE builds whose static field type differs (architectury's
     * RegistrySupplier vs NeoForge's DeferredHolder - see this class's javadoc),
     * so a compiled direct reference can fail at runtime. Instead the stack is
     * scanned for the component value and removed through its own
     * {@code TypedDataComponent.type()}, the same value-scan {@link #read} uses.</p>
     *
     * @param pos     where the dragon appears (block click: centre of the
     *                clicked column, one block up; the addon: the player)
     * @param yRot    facing; IaF's block path uses 180 + the clicker's facing
     * @param releaser who released it, or null - ownership transfer only for a
     *                real player under the gamerule
     * @return true when a dragon was released and the storage was stripped
     */
    public static boolean releaseDragonAt(ItemStack stack, ServerLevel level,
                                          Vec3 pos, float yRot, Player releaser) {
        DragonHornComponent comp = read(stack);
        if (comp == null) {
            return false;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(comp.entityType()).orElse(null);
        if (type == null) {
            return false;
        }

        Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }

        if (entity instanceof DragonBaseEntity dragon) {
            dragon.load(comp.entityData());
        }

        UUID stored = comp.entityUuid();
        if (stored != null) {
            entity.setUUID(stored);
        }

        entity.absMoveTo(pos.x, pos.y, pos.z, yRot, 0.0F);

        // Ownership before the spawn, exactly as the mixin injects it into
        // IaF's own useOn - proven in the field on the block-click path.
        if (releaser != null
                && ModGameRules.isDragonHornTransferEnabled(level)
                && entity instanceof TamableAnimal tame
                && !tame.isOwnedBy(releaser)) {
            tame.setTame(true, true);
            tame.tame(releaser);
        }

        if (level.addFreshEntity(entity)) {
            stripDragonHorn(stack);
            // The same release observers as the beast line (#106) - the Curios
            // addon stamps its linkage here instead of mixing into us.
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new org.bitwisemadness.pseudocore.event.PseudocoreReleaseEvent(
                            stack, entity, level, pos, releaser));
            return true;
        }
        return false;
    }

    /**
     * Remove the horn component found by value scan, never by field reference -
     * see {@link #releaseDragonAt} for the cross-build type mismatch that makes
     * a compiled {@code IafDataComponents.DRAGON_HORN} reference unsafe.
     */
    private static void stripDragonHorn(ItemStack stack) {
        for (TypedDataComponent<?> component : stack.getComponents()) {
            if (component.value() instanceof DragonHornComponent) {
                stack.remove(component.type());
                return;
            }
        }
    }

    /**
     * Capture a dragon into this stack through the tier's own gates (#109,
     * addon Phase 1 recall): the stack's item is resolved to its tier and that
     * instance's real {@code interactLivingEntity} runs - the very chain the
     * right-click takes - so the gates cannot fork from the click path.
     *
     * <p><b>The stack is held in the main hand for the duration of the call.</b>
     * IaF's horn path discards the stack parameter and re-reads
     * {@code player.getItemInHand(hand)} - proven against the pinned 9128508
     * bytecode and a live probe: with an empty hand the call returns PASS and
     * stores nothing, with the core held it proceeds to the gates and IaF's
     * ownership guard. Every real click has the item in hand; a recall passes
     * the stack that lives in the <i>Curios</i> slot, so this method arranges
     * that state itself and puts the hand back afterwards (same-instance, so
     * the click path - already holding it - is a no-op round trip).</p>
     *
     * <p>The same contract as {@link #releaseDragonAt}: callers only reach
     * here while a dragon core is in play, which means Ice and Fire is
     * installed, so nothing here loads without it. The signature stays
     * vanilla-typed end to end so the Curios addon compiles it without IaF on
     * its own classpath - it cannot even name {@link DragonPseudocoreItem},
     * whose superclass is IaF's horn item (#109).</p>
     *
     * <p>Returns exactly what the chain returns: our tier gates refuse with
     * {@code PASS} (after their actionbar message), IaF's own ownership guard
     * declines with {@code CONSUME}, and a stored dragon comes back as
     * {@code SUCCESS} - the refusal shapes are pinned by
     * {@code DragonLineGameTests}.</p>
     *
     * @param player who is capturing - drives the gate messages and limits
     * @param target the dragon being captured
     * @param stack  the dragon core whose storage receives it
     * @return the tier chain's interaction result
     */
    public static InteractionResult tryCaptureDragon(Player player, LivingEntity target, ItemStack stack) {
        if (!(stack.getItem() instanceof DragonPseudocoreItem core)) {
            return InteractionResult.PASS;
        }
        ItemStack previous = player.getItemInHand(InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        try {
            return core.interactLivingEntity(stack, player, target, InteractionHand.MAIN_HAND);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
    }

    /** The growth addon whose stretch formula {@link DragonStages} mirrors. */
    private static final String GROWTH_ADDON_MOD_ID = "tensura_iaf_growth";

    /**
     * The stage Ice and Fire would report for a stored dragon's frozen age.
     *
     * <p>The configured {@code maxTamedDragonAge} is read live - a client
     * reads its own copy of IaF's common config, the same contract every
     * common-config consumer in this pack accepts - and the growth addon is
     * detected by mod id. Both formulas and their evidence live in
     * {@link DragonStages}; this method only gathers the two inputs.</p>
     *
     * <p>Both entry points run under the mixin, which never loads without Ice
     * and Fire, so the config read below cannot be reached in a world where
     * {@code IafCommonConfig} is absent.</p>
     */
    public static int stageForAgeTicks(int ageTicks) {
        return DragonStages.stageFor(ageTicks / 24000,
                IafCommonConfig.INSTANCE.dragon.maxTamedDragonAge.getValue(),
                ModList.get().isLoaded(GROWTH_ADDON_MOD_ID));
    }
}
