package org.bitwisemadness.beasthorn.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.bitwisemadness.beasthorn.item.BeastHornItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("beasthorn");

    public static final DeferredItem<BeastHornItem> BEAST_HORN = ITEMS.registerItem("beast_horn",
            properties -> new BeastHornItem(properties
                    .stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static void register() {
    }
}
