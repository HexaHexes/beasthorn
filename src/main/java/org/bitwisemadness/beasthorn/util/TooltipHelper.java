package org.bitwisemadness.beasthorn.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds the shared tooltip block for a filled horn.
 *
 * <p>Every line is labelled so a reader never has to guess which value is which.
 * Order is fixed: name, race, owner, health, then EP. The always-present vanilla
 * values group at the top; the mod-conditional EP sits last so its absence when
 * Tensura is not installed leaves no gap mid-stack. Both the Beast Horn and Ice
 * and Fire's Dragon Horn run this same method, which is what keeps the two
 * horns' readouts identical rather than merely similar.</p>
 *
 * <p>A label is always grey. A value carries only the colour this mod chose for
 * it — except the mob's custom name, which brings its own styling and is passed
 * through untouched.</p>
 */
public final class TooltipHelper {

    /**
     * Tooltip code normally only runs on the client, but anything touching
     * {@code Minecraft} or {@code I18n} would hard-crash a dedicated server if
     * NeoForge ever routed a tooltip through it. Resolve once so those two
     * client-only classes are only ever entered when they exist.
     */
    private static final boolean CLIENT_DIST =
            net.neoforged.fml.loading.FMLEnvironment.dist.isClient();

    private TooltipHelper() {
    }

    public static void addLines(List<Component> tooltip, ResourceLocation typeId, CompoundTag data,
                                HolderLookup.Provider registries) {
        // Name - only if the mob carries a custom name. The name keeps whatever
        // colour codes it was given, so it reads exactly as it does overhead.
        Component customName = readCustomName(data, registries);
        if (customName != null && !customName.getString().isBlank()) {
            tooltip.add(named("Name", customName));
        }

        // Race - the mob's entity type description, preceded by its variant
        // where one exists. Prepending reads "Emerald Fire Dragon", which is
        // the shape IaF already gives hippogryphs through its own lang value;
        // that one species names the mob outright, so it replaces instead. A
        // variant that names the species on its own - a direwolf's "Mystic
        // Wind Wolf" - retires the race for the same reason.
        VariantHelper.Variant variant = VariantHelper.variantOf(typeId, data);
        String race = describeEntityType(typeId);
        if (variant != null) {
            race = variant.replacesRace() || VariantHelper.alreadyNamesMob(variant.text(), race)
                    ? variant.text()
                    : variant.text() + " " + race;
        }
        tooltip.add(line("Race", race, ChatFormatting.AQUA));

        // Owner - resolve the UUID to a player name where possible.
        resolveOwner(data).ifPresent(uuid -> tooltip.add(
                line("Owner", resolvePlayerName(uuid), ChatFormatting.GREEN)));

        // Health - read from NBT, so it reflects the mob as captured.
        if (data != null && data.contains("Health")) {
            float health = data.getFloat("Health");
            float max = data.contains("attributes") ? readMaxHealthFromNbt(data) : health;
            String text = max > 0.0F
                    ? String.format("%.1f / %.1f", health, max)
                    : String.format("%.1f", health);
            tooltip.add(line("Health", text, ChatFormatting.RED));
        }

        // EP - Tensura only, and only when the stored NBT actually has it.
        Optional<Double> ep = TensuraBridge.readEpFromNbt(data);
        ep.ifPresent(value -> tooltip.add(
                line("EP", formatEp(value), ChatFormatting.LIGHT_PURPLE)));
    }

    /**
     * Ice and Fire's own Dragon Horn lines, emitted here so the Dragon Horn
     * shows them after the shared block.
     *
     * <p>The wording is IaF's — these are its own lang keys — and the stage
     * thresholds are IaF's own AgeTicks bands, verified against CE 2.0-beta.15.
     * Reproducing them rather than letting IaF's {@code appendHoverText} run is
     * what makes the ordering above possible; it costs us coupling to those two
     * NBT keys and the AgeTicks maths.</p>
     */
    public static void addDragonLines(List<Component> tooltip, CompoundTag data) {
        if (data == null || data.isEmpty()) return;

        // Gender - a plain boolean, true meaning male.
        boolean male = data.getBoolean("Gender");
        String genderLabel = Component.translatable("dragon.gender").getString();
        String genderValue = Component.translatable(
                male ? "dragon.gender.male" : "dragon.gender.female").getString();
        tooltip.add(Component.literal(genderLabel + " " + genderValue)
                .withStyle(ChatFormatting.GRAY));

        // Stage and age in days, both derived from AgeTicks as IaF does. The
        // spaces around the day count are ours: IaF concatenates these tokens
        // jammed together, which renders "Stage 3( 50 Days)".
        int days = data.getInt("AgeTicks") / 24000;
        tooltip.add(Component.translatable("dragon.stage")
                .append(Component.literal(" " + dragonStageFor(days) + " "))
                .append(Component.translatable("dragon.days.front"))
                .append(Component.literal(days + " "))
                .append(Component.translatable("dragon.days.back"))
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * The two lines an empty Dragon Horn shows, mirroring the Beast Horn's own
     * empty tooltip except for the wording.
     *
     * <p>These replace IaF's method entirely rather than sitting alongside it,
     * because IaF emits nothing at all for an empty horn — which left the
     * Dragon Horn as the only item in the pack with a blank tooltip.</p>
     */
    public static void addEmptyDragonLines(List<Component> tooltip) {
        tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Right-click a dragon to capture it.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** IaF's AgeTicks bands: 25, 50, 75 and 100 days old. */
    private static int dragonStageFor(int days) {
        if (days >= 100) return 5;
        if (days >= 75) return 4;
        if (days >= 50) return 3;
        if (days >= 25) return 2;
        return 1;
    }

    /** A grey label, ready for a value to be appended. */
    private static MutableComponent label(String label) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY);
    }

    /** A labelled line whose value is coloured by us. */
    private static MutableComponent line(String label, String value, ChatFormatting valueColor) {
        return label(label).append(Component.literal(value).withStyle(valueColor));
    }

    /**
     * A labelled line whose value brings its own styling — the mob's custom
     * name.
     *
     * <p>The value is appended untouched, so whatever colour and text codes the
     * name carries survive into the tooltip. A name with no colour of its own
     * simply inherits grey from the label, which is the normal Minecraft
     * behaviour and reads correctly.</p>
     */
    private static MutableComponent named(String label, Component value) {
        return label(label).append(value);
    }

    /**
     * The mob's custom name as a component, so its styling is preserved.
     *
     * <p>Deliberately does <b>not</b> call {@code Component#getString}, which
     * would strip every style the name carried and leave a flat string for us
     * to re-colour wrongly.</p>
     */
    private static Component readCustomName(CompoundTag data, HolderLookup.Provider registries) {
        if (data == null || !data.contains("CustomName")) return null;
        String raw = data.getString("CustomName");
        if (raw == null || raw.isEmpty()) return null;
        try {
            Component name = Component.Serializer.fromJson(raw, registries);
            if (name != null) return name;
        } catch (Exception ignored) {
            // Not valid component JSON - show the raw text rather than nothing.
        }
        return Component.literal(raw);
    }

    private static String describeEntityType(ResourceLocation id) {
        if (id == null) return "Unknown";
        // Localized name when we are on the client; the entity's own
        // description component otherwise (its getString() degrades to the
        // translation key on a server, which is still readable).
        if (CLIENT_DIST) {
            String key = "entity." + id.getNamespace() + "." + id.getPath();
            if (I18n.exists(key)) {
                return I18n.get(key);
            }
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type != null) {
            return type.getDescription().getString();
        }
        return id.toString();
    }

    /**
     * Owner comes from whichever system actually holds it: Tensura's existence
     * storage first (the most common on this pack), then vanilla's Owner tag
     * used by tamed animals, then Redomesticate's owner tag.
     */
    private static Optional<UUID> resolveOwner(CompoundTag data) {
        if (data == null) return Optional.empty();

        Optional<UUID> tensura = TensuraBridge.readOwnerFromNbt(data);
        if (tensura.isPresent()) return tensura;

        if (data.hasUUID("Owner")) return Optional.of(data.getUUID("Owner"));

        // Redomesticate stores its owner under a namespaced key.
        if (data.contains("RedomesticateOwner")) {
            String raw = data.getString("RedomesticateOwner");
            try {
                return Optional.of(UUID.fromString(raw));
            } catch (Exception ignored) {
                // fall through
            }
        }
        return Optional.empty();
    }

    private static String resolvePlayerName(UUID uuid) {
        // Minecraft is client-only; on a server just show the short uuid.
        if (CLIENT_DIST) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                var player = mc.level.getPlayerByUUID(uuid);
                if (player != null) return player.getName().getString();
            }
            if (mc.getConnection() != null) {
                var info = mc.getConnection().getPlayerInfo(uuid);
                if (info != null) return info.getProfile().getName();
            }
        }
        // Offline or unknown - show a short uuid rather than nothing.
        return uuid.toString().substring(0, 8);
    }

    /**
     * Vanilla stores entity attributes as a ListTag of {id, base, modifiers},
     * so scan it for the max-health entry rather than reading a compound.
     * Returns 0 when absent, which the caller renders as a plain health value.
     */
    private static float readMaxHealthFromNbt(CompoundTag data) {
        net.minecraft.nbt.ListTag attributes = data.getList("attributes",
                net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < attributes.size(); i++) {
            net.minecraft.nbt.CompoundTag entry = attributes.getCompound(i);
            if (entry.contains("id") && entry.getString("id").endsWith("generic.max_health")
                    && entry.contains("base")) {
                return (float) entry.getDouble("base");
            }
        }
        return 0.0F;
    }

    /** Group EP into thousands - Tensura EP values run large. */
    private static String formatEp(double ep) {
        long rounded = Math.round(ep);
        return String.format("%,d", rounded);
    }
}
