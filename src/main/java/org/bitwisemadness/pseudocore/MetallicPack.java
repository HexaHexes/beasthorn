package org.bitwisemadness.pseudocore;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Registers the optional "Metallic Bodies" resource pack (#56) that ships
 * inside this mod's jar and overrides only the two body textures with more
 * metallic renders of the same art.
 *
 * <p>{@code AddPackFindersEvent} is an {@code IModBusEvent}, so the mod bus is
 * selected automatically (the same detection that routes the tint-handler
 * registration). The listener runs on both sides; the pack type check makes a
 * dedicated server no-op without needing a client-only class.</p>
 *
 * <p>The pack is registered <b>not required</b>: it appears on the Resource
 * Packs screen and the player toggles it. Minecraft keeps the selected set in
 * {@code options.txt}, so an optional pack starts deselected - "required"
 * packs are the only default-on kind and cannot be switched off, which would
 * not be optional.</p>
 *
 * <p>The id's path ({@code metallic}) doubles as the folder name inside the
 * jar: {@code addPackFinders} resolves it through {@code IModFile#findResource}
 * and opens it with {@code PathPackResources}, so the layout is simply
 * {@code metallic/pack.mcmeta}, {@code metallic/pack.png} and
 * {@code metallic/assets/...} at the jar root - deliberately outside
 * {@code assets/}, so the mod's own resource loading never sees the
 * overrides.</p>
 */
@EventBusSubscriber(modid = Pseudocore.MOD_ID)
public final class MetallicPack {

    /** The pack folder in the jar, and its id path. */
    private static final String PACK_PATH = "metallic";

    private MetallicPack() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, PACK_PATH),
                PackType.CLIENT_RESOURCES,
                Component.translatable("pack.pseudocore.metallic.name"),
                PackSource.BUILT_IN,
                false,
                Pack.Position.TOP);
    }
}
