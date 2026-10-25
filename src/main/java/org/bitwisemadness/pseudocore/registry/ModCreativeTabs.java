package org.bitwisemadness.pseudocore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.bitwisemadness.pseudocore.Pseudocore;

/**
 * The mod's own creative tab.
 *
 * <p>A tab of its own rather than vanilla's Tools &amp; Utilities: the item line
 * is a set of tiers, and a single-purpose tab is where the whole set belongs
 * together. The icon is the iron tier, the one every environment has.</p>
 *
 * <p>Nothing is filtered out of the contents. Every tier is registered exactly
 * when it is craftable - the mod's own items are the only ones here, and an
 * integration being off removes both its items and its recipes together - so a
 * registered entry is always a real, obtainable item.</p>
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Pseudocore.MOD_ID);

    /** The tab holding every Pseudocore tier. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PSEUDOCORE =
            CREATIVE_MODE_TABS.register("pseudocore", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.pseudocore"))
                    .icon(() -> new ItemStack(ModItems.IRON_PSEUDOCORE.get()))
                    .displayItems((parameters, output) -> output.accept(ModItems.IRON_PSEUDOCORE.get()))
                    .build());

    private ModCreativeTabs() {
    }
}
