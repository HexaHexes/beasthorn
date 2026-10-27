package org.bitwisemadness.pseudocore.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.bitwisemadness.pseudocore.Pseudocore;

/**
 * Registers the mod's Dragon Horn override pack.
 *
 * <p>The pack lives under {@code resources/overrides/} - outside the mod's own
 * {@code data/} root - so its files reach the game only through this registration
 * and the mod's own datapack never leaks them. It exists for one purpose: while
 * the Ice and Fire integration is active, IaF's Dragon Horn steps aside for the
 * dragon pseudocore line.</p>
 *
 * <p>Mechanics, verified against the 1.21.1 sources: a data id resolves to the
 * <b>single topmost file</b> across the pack stack ({@code listMatchingResources}
 * yields one resource per id), and {@code AddPackFindersEvent} runs after every
 * mod's own pack is registered - so the pack registered here always sits last,
 * which for a TOP pack means its files always win. The three files it carries:</p>
 *
 * <ul>
 * <li>{@code iceandfire:recipe/dragon_horn} - IaF's own recipe content, gated
 * {@code not(pseudocore:iceandfire_active)}. Integration on: the condition fails,
 * the id resolves to this file, and the recipe is gone - disabled, not replaced.
 * Integration off: this file still owns the id, and it loads IaF's original
 * content, exactly as if this mod were absent.</li>
 * <li>{@code iceandfire:recipes/misc/dragon_horn} - the recipe-unlock
 * advancement, reworked to trigger on dragon bones and unlock the ten dragon
 * pseudocore recipes. Ungated, because the dragon items exist and stay craftable
 * in both switch states (their recipes guard on mod presence, not the switch).</li>
 * <li>{@code iceandfire:iceandfire/dragon_horn} - the gameplay advancement,
 * reworked to fire when any dragon pseudocore is obtained. Icon, title,
 * description and parent stay IaF's, so the advancement reads exactly as IaF
 * wrote it - only what triggers it changes.</li>
 * </ul>
 *
 * <p>Registered only while Ice and Fire CE is installed: without it the recipe
 * override would name an item that does not exist, and there is nothing to
 * override.</p>
 */
@EventBusSubscriber(modid = Pseudocore.MOD_ID)
public final class PseudocorePackEvents {

    private PseudocorePackEvents() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA
                || !ModList.get().isLoaded("iceandfire")) {
            return;
        }
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, "overrides"),
                PackType.SERVER_DATA,
                Component.literal("Pseudocore: Dragon Horn overrides"),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP);
    }
}
