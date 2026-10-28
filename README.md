# Pseudocore

A single low-tech fantasy item line -- the **Pseudocore** -- that captures a mob with
all of its data, holds it safely, and lets you carry it, sell it, or trade it to
another player. The mob inside keeps everything: its name, its health, its inventory,
its owner, and any modded data stored on it.

The line comes in **twelve tiers**, each with a higher capture limit than the last,
plus a separate **five-tier dragon line** for Ice and Fire's dragons.

## Inspiration

This mod draws on three sources:

- **Ice and Fire CE** - Ice and Fire's Dragon Horn is the model for the whole concept:
  a vessel item that captures a mob with all of its data, shows it in a labelled
  tooltip, and hands ownership off when it is released. The capture guards, the
  tooltip layout, and the ownership-transfer idea all follow that design, and the
  item's model silhouette is adapted from Ice and Fire's horn (see [`NOTICE.md`](NOTICE.md)).
- **Tensura: Reincarnated** - Tensura contributed the progression layer: EP (energy
  points) shown for captured mobs, named variants, magic crystals as the craft
  ingredients of the tier recipes, and the metal tiers that the pseudocore line is
  built on.
- **The Pseudocores of the Tensura anime and manga** - the item's design follows
  theirs, and they are where the mod's name comes from.

The two mods are optional at runtime: Pseudocore runs standalone and only layers
their content in when the corresponding mod is present.

## What a pseudocore does

**Capture** -- right-click a mob to store it. The mob's entire data is written into
the item, so nothing about it is lost -- which is what makes it safe to trade.
Capture is guarded:

- You can only capture a mob that is **ownerless**, or that **you own** -- never
  someone else's pet.
- A mob that is **leashed** or **carrying a rider** cannot be captured.

**Release** -- use the item on the top of a block to release the mob exactly as
captured, at its original identity (so anything bound to that mob by ID -- deeds and
the like -- still resolves).

**Trade** -- because the mob lives entirely in the item, handing it to another player
is all it takes. Whoever holds it, and whoever releases it, gets the same mob.

## What's inside (tooltip)

A filled pseudocore always shows its contents, in this order:

| Line | Shown |
|---|---|
| Name | only if the mob has a custom name |
| Race | always -- the mob's variant, then its type |
| Owner | only if the mob has an owner (a Tensura-recorded owner needs Tensura analysis - see below) |
| Health | always |
| EP | only if Tensura is loaded, the mob has EP, and you are entitled to Tensura's analysis |

Variants read naturally -- *Red Fang Direwolf*, *Emerald Fire Dragon*,
*Black Hippogryph* -- covering Ice and Fire, Tensura, Mobs of Mythology and Mysticism.
An empty pseudocore shows a hint instead, together with the capture caps its tier
enforces (see the tier tables below).

While Tensura is installed, the EP line and the Tensura-recorded owner follow
Tensura's own **analysis rules**: a holder whose analysis cannot reach a mob's
EP reads it blurred the same way Tensura's own overlay blurs it, and a holder
with no analysis at all sees neither line. Vanilla-tamed owners are never
hidden, and name, race and health are always shown. This can be switched off in
`config/pseudocore-common.toml` (`respectAnalysisGating`).

## Appearance

The item's look follows the Pseudocores of Tensura's anime and manga: a vessel
with a cluster of coloured orbs, while the model silhouette stays adapted from
Ice and Fire's horn. When a mob is stored, the item's **three small orbs** tint to
that mob's spawn-egg color -- vivid, and only the orbs. Mob types with no spawn
egg get a stable color derived from their id. Each tier's body also carries its
metal's colour -- the iron tier's texture as drawn, the rest tinted from it.

## The tier line

Every beast-tier pseudocore captures the same way; what separates the tiers is
**how much EP** the mob inside may have. EP is read from Tensura, so the caps only
matter while Tensura is installed and the integration is on.

| Tier | Max EP | Tier | Max EP |
|---|---|---|---|
| Copper | 1,500 | Low Magisteel | 150,000 |
| Iron | 7,500 | High Magisteel | 225,000 |
| Silver | 15,000 | Pure Magisteel | 300,000 |
| Gold | 37,500 | Mithril | 450,000 |
| Netherite | 112,500 | Orichalcum | 750,000 |
| | | Adamantite | 1,500,000 |
| | | Hihi'irokane | uncapped |

Every cap is configurable per tier in `config/pseudocore-server.toml`
(`[pseudocore.tiers.<tier>]`), including whether the tier may capture tamed or
Tensura-named mobs. A tier that refuses a capture says so, naming the mob.

Each tier has **two recipes**, shaped around the tier's ingots (tags like
`c:ingots/copper`, so any mod's metal works): one uses the Tensura magic crystal of
the matching quality and exists only while Tensura's integration is active; the
other uses any gem from `c:gems` and exists only while that integration is off. One
of the two is always craftable, whichever way the pack is built.

## Ice and Fire's dragons: the dragon line

Pseudocore **does** capture Ice and Fire's dragons -- but never with a beast
pseudocore. They are blacklisted there, alongside 62 bosses, eight Tensura and
Mysticism encounter mobs and villagers/colonists (79 ids in all, editable in
`config/pseudocore-server.toml`), so a beast core can't be used to pocket an
encounter. The capture attempt simply refuses, naming the mob.

Dragons are captured with the **dragon pseudocores**, a five-tier line of their own
that speaks IaF's own capture language: the same ownership guard (you capture a
dragon **you own**, never someone else's), and a second, **growth-stage** cap so a
starter core can't pocket an ancient wyrm.

| Tier | Max stage | Max EP | Tier | Max stage | Max EP |
|---|---|---|---|---|---|
| Copper Dragon | 1 | 150,000 | Gold Dragon | 4 | 825,000 |
| Iron Dragon | 2 | 300,000 | Netherite Dragon | 5 | uncapped |
| Silver Dragon | 3 | 525,000 | | | |

Dragon tiers also take the same tamed/named switches, in
`[pseudocore.dragonTiers.<tier>]` of the same file. Stage is derived the way Ice
and Fire derives it for the living dragon - its own age bands over the pack's
configured growth schedule, so a 512-day growth clock reads stage 1 where stock
128-day growth reads stage 4. Release works like the beast line, at the dragon's
original identity and ownership.

Dragon pseudocore recipes are built from `c:bones/dragon`, the tier's ingots and a
magic crystal (or a gem, as above) -- and only exist while Ice and Fire is
installed; the dragon line is the one part of the mod that needs a mod to be
craftable at all. Ice and Fire's own **Dragon Horn recipe is disabled** by the
bundled override data pack, and Ice and Fire's dragon-horn advancements are
reworked to point at the dragon pseudocores instead. The horn item itself is
untouched -- other datapacks can still enable or re-add it however they like.
The dragon tiers sit in the mod's own creative tab.

## Optional ownership transfer (off by default)

Releasing a mob never changes who owns it, unless a server opts in:

| Gamerule | Default | Effect |
|---|---|---|
| `pseudocoreOwnershipTransfer` | off | Releasing a mob from a Pseudocore makes the releasee its owner |
| `pseudocoreDragonOwnershipTransfer` | off | Releasing a dragon from a dragon pseudocore (or IaF's Dragon Horn) makes the releasee its owner |

```
/gamerule pseudocoreOwnershipTransfer true
/gamerule pseudocoreDragonOwnershipTransfer true
```

## Turning an integration off

Ice and Fire CE and Tensura can each be switched off **without uninstalling them**,
in `config/pseudocore-common.toml`:

| Setting | Default | Effect when off |
|---|---|---|
| `tensuraIntegration` | on | No EP line, no Tensura ownership read or transfer, no Tensura variant names, and the crystal recipes give way to the gem ones |
| `iceAndFireIntegration` | on | The dragon pseudocores stop capturing, the Dragon Horn keeps IaF's own tooltip and rarity, its release no longer transfers ownership, and its recipe is no longer overridden (the dragon recipes are keyed to Ice and Fire being installed, so they remain) |
| `respectAnalysisGating` | on | The EP line and the Tensura-recorded owner show to everyone, with no blur; without it they follow Tensura's analysis rules |

The switch is honoured in code and in data packs alike, so it also works on a
server whose modpack still has the mod installed. The entity blacklist lives in
its own file, `config/pseudocore-server.toml`.

## Getting it in-game

Craft a pseudocore (see its recipe in-game) and use it as above -- no configuration.
Every tier also sits in its own **Pseudocore** creative tab.

## Dependencies

| | Mod | Why |
|---|---|---|
| **Required** | Minecraft 1.21.1 + NeoForge 21.1+ | Platform |
| **Required** | Mob Info Common | Shared variant-name and analysis-gating logic (ships with this mod's packs) |
| Optional | [Ice and Fire CE](https://www.curseforge.com/minecraft/mc-mods/iceandfire-ce) | The dragon line and its capture rules, Dragon Horn ownership hand-off, and Ice and Fire variant names |
| Optional | Tensura: Reincarnated | EP tooltip, Tensura ownership fields, Tensura variant names, and the analysis rules the tooltip gates by |

Variant names for **Mobs of Mythology** (drake, sporeling) and **Mysticism**
(okami) come through Mob Info Common, so neither mod needs to be installed for
Pseudocore to load -- each is read only when present.

**Pseudocore runs standalone** -- every integration above is independent. Without
one, the item still works and just omits that mod's names or extras.

## Credits

- Capture/release approach and the horn silhouette modeled on **Ice and Fire** /
  **Ice and Fire CE** (LGPL). A copy of the horn texture is embedded so the item
  still renders without Ice and Fire. Full attribution and the list of modifications
  are in [`NOTICE.md`](NOTICE.md).
- EP display, tier progression, and metal/crystal theming inspired by
  **Tensura: Reincarnated**.
- Item design -- the vessel and its coloured orbs -- inspired by the Pseudocores of
  the **Tensura** anime and manga, which is also where the mod's name comes from.

## License

GNU Lesser General Public License v2.1 -- see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.

The horn's model silhouette and body texture are adapted from Ice and Fire / Ice and
Fire CE and remain under **LGPL-2.1**; that third-party material is covered by
[`NOTICE.md`](NOTICE.md), not by this mod's licence alone.

## AI disclosure

Parts of this project were written with the assistance of AI coding tools.
AI-assisted code, data and documentation ship only after human review by the
maintainers.
