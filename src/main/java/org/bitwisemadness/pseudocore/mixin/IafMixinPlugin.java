package org.bitwisemadness.pseudocore.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 *
 * <p>When ModList cannot answer because it has not been populated yet, this
 * fails <b>open</b>. {@code requiredMods} has already gated the config at the
 * loader level, so an indeterminate answer here still means IaF CE is present.
 * Failing closed instead made Mixin skip every class in the config in total
 * silence: the mod loaded with no error and no log line, and the Dragon Horn
 * simply kept IaF's own tooltip and rarity.</p>
 */
public final class IafMixinPlugin implements IMixinConfigPlugin {

    /**
     * Own logger rather than the mod's. This plugin is loaded during mixin
     * bootstrap, ahead of mod construction, so it must not pull the mod's main
     * class in - and a message here is what makes a skipped config visible at
     * all, given that Mixin itself logs nothing when a plugin returns false.
     */
    private static final Logger LOGGER = LogManager.getLogger("pseudocore/mixin");

    private static final String IAF_MOD_ID = "iceandfire";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        net.neoforged.fml.ModList modList = net.neoforged.fml.ModList.get();
        if (modList == null) {
            // ModList has not been populated yet, so it cannot answer the
            // question either way. Fail OPEN.
            //
            // requiredMods on the mixin config has already gated this config at
            // the loader level, so reaching this point means Ice and Fire CE was
            // present. Returning false instead - which is what this method used
            // to do - made Mixin skip every class in the config in silence: no
            // crash, no error, no log line. Both Dragon Horn mixins quietly
            // never applied, and the only visible symptom was that IaF's own
            // tooltip and rarity survived untouched.
            LOGGER.info("Pseudocore: ModList not yet available when applying {} to {} - "
                    + "assuming Ice and Fire CE is present", mixinClassName, targetClassName);
            return true;
        }

        boolean loaded = modList.isLoaded(IAF_MOD_ID);
        if (loaded) {
            LOGGER.debug("Pseudocore: applying {} to {}", mixinClassName, targetClassName);
        } else {
            LOGGER.info("Pseudocore: skipping {} - Ice and Fire CE is not loaded", mixinClassName);
        }
        return loaded;
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
