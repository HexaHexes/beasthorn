package org.bitwisemadness.pseudocore.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * The mob stored inside a Pseudocore.
 *
 * <p>Mirrors Ice and Fire CE's {@code DragonHornComponent} shape, which is the
 * proven record type for "an item holding a whole entity": the entity's type id,
 * its UUID, and its complete {@code saveWithoutId} NBT. Storing the full NBT is
 * what lets everything about the mob — name, health, ownership, Tensura's
 * ManasCore storage, inventory, AI state — survive capture, trade, and release.</p>
 *
 * @param entityType the registry id of the captured entity type, e.g. {@code minecraft:zombie}
 * @param entityUuid the captured entity's UUID, restored on release so things
 *                   bound to the mob by UUID (Redomesticate deeds, etc.) still match
 * @param entityData the entity's full NBT from {@code Entity#saveWithoutId}
 */
public record ContainedMobComponent(ResourceLocation entityType, UUID entityUuid, CompoundTag entityData) {

    public static final Codec<ContainedMobComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("entityType").forGetter(ContainedMobComponent::entityType),
            UUIDUtil.CODEC.fieldOf("entityUuid").forGetter(ContainedMobComponent::entityUuid),
            CompoundTag.CODEC.fieldOf("entityData").forGetter(ContainedMobComponent::entityData)
    ).apply(instance, ContainedMobComponent::new));

    /**
     * Network codec. {@link ByteBufCodecs#COMPOUND_TAG} is typed to plain
     * {@code ByteBuf}, which satisfies the {@code ? super RegistryFriendlyByteBuf}
     * bound that {@code networkSynchronized} requires.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainedMobComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, ContainedMobComponent::entityType,
                    UUIDUtil.STREAM_CODEC, ContainedMobComponent::entityUuid,
                    ByteBufCodecs.COMPOUND_TAG, ContainedMobComponent::entityData,
                    ContainedMobComponent::new
            );
}
