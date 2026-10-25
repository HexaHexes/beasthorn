package org.bitwisemadness.pseudocore.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.bitwisemadness.pseudocore.item.PseudocoreItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("pseudocore");

    public static final DeferredItem<PseudocoreItem> IRON_PSEUDOCORE = ITEMS.registerItem("iron_pseudocore",
            properties -> new PseudocoreItem(properties
                    .stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static void register() {
    }
}
