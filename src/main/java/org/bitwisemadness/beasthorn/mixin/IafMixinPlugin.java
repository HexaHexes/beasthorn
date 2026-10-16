package org.bitwisemadness.beasthorn.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Belt-and-braces guard for the Ice and Fire CE mixin config.
 *
 * <p>The config already declares {@code requiredMods=["iceandfire"]} in
 * neoforge.mods.toml, which is the primary mechanism that keeps it from loading
 * without IaF CE. This plugin re-checks independently, because a mixin config
 * that applies against a missing target class fails at mixin apply time with a
 * hard crash rather than a clean skip — and the cost of the duplicate check is
 * one ModList lookup.</p>
 *
 * <p>Uses {@link net.neoforged.fml.ModList} only (never the loading-time list),
 * so it is safe at the point FML evaluates configs.</p>
 */
public final class IafMixinPlugin implements IMixinConfigPlugin {

    private static final String IAF_MOD_ID = "iceandfire";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // ModList is available when FML processes mixin configs; guard anyway in
        // case an early loader path calls this before it is populated.
        net.neoforged.fml.ModList modList = net.neoforged.fml.ModList.get();
        if (modList == null) {
            return false;
        }
        return modList.isLoaded(IAF_MOD_ID);
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String targetClassName2, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String targetClassName2, IMixinInfo mixinInfo) {
    }
}
