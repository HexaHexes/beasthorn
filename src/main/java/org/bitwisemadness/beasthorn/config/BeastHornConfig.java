package org.bitwisemadness.beasthorn.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Server config. The main tunable is the entity blacklist, which is what keeps
 * Ice and Fire's dragons on their own Dragon Horn instead of the Beast Horn.
 */
public final class BeastHornConfig {

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    private BeastHornConfig() {
    }

    public static final class Common {
        public final ModConfigSpec.ConfigValue<List<? extends String>> blacklistedEntities;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("beastHorn");
            blacklistedEntities = builder
                    .comment(
                            "Entity types that cannot be captured by the Beast Horn.",
                            "Ice and Fire's three dragon types are listed by default so they",
                            "keep using IaF's own Dragon Horn, which stores dragon-specific",
                            "data the Beast Horn does not reproduce. Add or remove ids here",
                            "to change which mobs are exempt.")
                    .defineListAllowEmpty("blacklistedEntities", List.of(
                            "iceandfire:fire_dragon",
                            "iceandfire:ice_dragon",
                            "iceandfire:lightning_dragon"
                    ), () -> "", entry -> entry instanceof String s && s.contains(":"));
            builder.pop();
        }

        public boolean isBlacklisted(net.minecraft.resources.ResourceLocation id) {
            for (String entry : blacklistedEntities.get()) {
                if (entry.equals(id.toString())) {
                    return true;
                }
            }
            return false;
        }
    }
}
