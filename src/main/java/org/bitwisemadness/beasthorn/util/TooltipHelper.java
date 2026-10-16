package org.bitwisemadness.beasthorn.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.bitwisemadness.beasthorn.item.component.ContainedMobComponent;

import java.util.Optional;
import java.util.UUID;

/**
 * Builds the tooltip for a filled Beast Horn.
 *
 * <p>Every line is labelled so a reader never has to guess which value is which.
 * Order is fixed: name, race, owner, health, then EP. The always-present vanilla
 * values group at the top; the mod-conditional EP sits last so its absence when
 * Tensura is not installed leaves no gap mid-stack.</p>
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

    public static void addLines(java.util.List<Component> tooltip, ContainedMobComponent comp,
                                net.minecraft.core.HolderLookup.Provider registries) {
        if (comp == null) return;

        CompoundTag data = comp.entityData();

        // Name - only if the mob carries a custom name.
        String customName = readCustomName(data, registries);
        if (customName != null && !customName.isBlank()) {
            tooltip.add(line("Name", customName, ChatFormatting.YELLOW));
        }

        // Race - the mob's entity type description.
        tooltip.add(line("Race", describeEntityType(comp.entityType()), ChatFormatting.AQUA));

        // Owner - resolve the UUID to a player name where possible.
        Optional<UUID> owner = resolveOwner(data);
        owner.ifPresent(uuid -> tooltip.add(
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

    private static MutableComponent line(String label, String value, ChatFormatting valueColor) {
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(valueColor));
    }

    private static String readCustomName(CompoundTag data, net.minecraft.core.HolderLookup.Provider registries) {
        if (data == null || !data.contains("CustomName")) return null;
        try {
            Component name = Component.Serializer.fromJson(data.getString("CustomName"), registries);
            return name == null ? null : name.getString();
        } catch (Exception e) {
            return data.getString("CustomName");
        }
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
