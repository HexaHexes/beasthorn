package org.bitwisemadness.pseudocore.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Server config. The main tunable is the entity blacklist, which is what keeps
 * Ice and Fire's dragons on their own Dragon Horn instead of the Pseudocore.
 */
public final class PseudocoreConfig {

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    private PseudocoreConfig() {
    }

    public static final class Common {
        public final ModConfigSpec.ConfigValue<List<? extends String>> blacklistedEntities;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("pseudocore");
            blacklistedEntities = builder
                    .comment(
                            "Entity types that cannot be captured by the Pseudocore.",
                            "",
                            "Ice and Fire's three dragon types are listed so they keep using IaF's",
                            "own Dragon Horn, which stores dragon-specific data this horn does not",
                            "reproduce. Named encounter mobs follow: Tensura's orc, goblin,",
                            "lizardman and three daemon ranks and Mysticism's okami are capturable",
                            "in their own right but story-relevant, so they are exempt too. Bosses",
                            "follow, so raid and world bosses stay fought rather than pocketed. The",
                            "villager family closes out the list: these mobs carry profession, home,",
                            "trades and colony jobs that a release into a new dimension would strand",
                            "them without.",
                            "",
                            "Adding or removing ids here changes which mobs are exempt.",
                            "Note this list only seeds a new config file - an existing one is",
                            "left exactly as you saved it.")
                    .defineListAllowEmpty("blacklistedEntities", List.of(
                            // Ice and Fire dragons - these belong to IaF's own horn.
                            "iceandfire:fire_dragon",
                            "iceandfire:ice_dragon",
                            "iceandfire:lightning_dragon",

                            // Named encounter mobs. Not bosses (Tensura's Orc Disaster is in
                            // the boss group below), but story-relevant enough that pocketing
                            // them would skip the encounter: Tensura's orc ranks, goblin,
                            // lizardman and three daemon ranks, and Mysticism's spirit wolf.
                            "tensura:orc",
                            "tensura:orc_lord",
                            "tensura:goblin",
                            "tensura:lizardman",
                            "tensura:arch_daemon",
                            "tensura:greater_daemon",
                            "tensura:lesser_daemon",
                            "mysticism:okami",

                            // Bosses. Identified by carrying a ServerBossEvent, which is the
                            // actual marker of a Minecraft boss rather than a name convention.
                            // That test alone is not enough: L Ender's Cataclysm and Mowzie's
                            // Mobs route their bars through a helper class instead, so their
                            // mobs never reference the event themselves. Those came from Boss
                            // Checklist's catalog, and every id below was checked against one
                            // source or the other.
                            "iceandfire:dread_queen",
                            "tensura:gazel_dwargo",
                            "tensura:hinata_sakaguchi",
                            "tensura:akash",
                            "tensura:charybdis",
                            "tensura:elemental_colossus",
                            "tensura:ifrit",
                            "tensura:orc_disaster",
                            "tensura:supermassive_slime",
                            "tensura:sylphide",
                            "tensura:undine",
                            "tensura:war_gnome",

                            // Tensura's add-ons.
                            "tensura_neb:carrion",
                            "tensura_neb:luminous_valentine",
                            "tensura_neb:rimuru_ogre_fight",
                            "foxboss:milim",
                            "mysticism:memoires",
                            "nightmareutils:otherworlder",
                            "nightmareutils:shinro_nakuba_maximum",

                            // L Ender's Cataclysm.
                            "cataclysm:netherite_monstrosity",
                            "cataclysm:ender_guardian",
                            "cataclysm:the_harbinger",
                            "cataclysm:ancient_remnant",
                            "cataclysm:the_leviathan",
                            "cataclysm:scylla",
                            "cataclysm:maledictus",
                            "cataclysm:ignis",
                            "cataclysm:amethyst_crab",
                            "cataclysm:coralssus",
                            "cataclysm:ender_golem",
                            "cataclysm:wadjet",
                            "cataclysm:ignited_revenant",
                            "cataclysm:clawdian",
                            "cataclysm:the_prowler",
                            "cataclysm:aptrgangr",
                            "cataclysm:kobolediator",

                            "mowziesmobs:ferrous_wroughtnaut",
                            "mowziesmobs:umvuthi",
                            "mowziesmobs:frostmaw",
                            "mowziesmobs:sculptor",

                            "block_factorys_bosses:sandworm",
                            "block_factorys_bosses:yeti",
                            "block_factorys_bosses:kraken",
                            "block_factorys_bosses:infernal_dragon",
                            "block_factorys_bosses:underworld_knight",

                            // Born in Chaos spells its horse-less form "withouta".
                            "born_in_chaos_v1:lord_pumpkinhead",
                            "born_in_chaos_v1:lord_pumpkinhead_head",
                            "born_in_chaos_v1:lord_pumpkinhead_withouta_horse",

                            "aquamirae:eel",
                            "aquamirae:captain_cornelia",
                            "darkdoppelganger:dark_doppelganger",
                            "illagerinvasion:invoker",
                            "irons_spellbooks:dead_king",
                            "irons_spellbooks:fire_boss",
                            "mythsandlegends:black_charro",
                            "mythsandlegends:cogwork_construct",
                            "mythsandlegends:warborn_aegis",
                            "netherman:azazel",
                            "netherman:azazel_human",

                            // Vanilla's own bosses, which are world-altering to pocket.
                            "minecraft:ender_dragon",
                            "minecraft:wither",
                            "minecraft:elder_guardian",

                            // Villagers, their guards, ribbits and MineColonies colonists.
                            "minecraft:villager",
                            "guardvillagers:guard",
                            "ribbits:ribbit",
                            "wandering_ribbit:wandering_ribbit",
                            "guardribbits:guard_ribbit",
                            "minecolonies:citizen"
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
