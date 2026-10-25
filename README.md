# Pseudocore

A single low-tech fantasy item line -- the **Pseudocore** -- that captures a mob with
all of its data, holds it safely, and lets you carry it, sell it, or trade it to
another player. The mob inside keeps everything: its name, its health, its inventory,
its owner, and any modded data stored on it.

## Inspiration

This mod is directly inspired by two mods:

- **Ice and Fire CE** - Ice and Fire's Dragon Horn is the model for the whole concept:
  a vessel item that captures a mob with all of its data, shows it in a labelled
  tooltip, and hands ownership off when it is released. The capture guards, the
  tooltip layout, and the ownership-transfer idea all follow that design, and the
  item's model silhouette is adapted from Ice and Fire's horn (see [`NOTICE.md`](NOTICE.md)).
- **Tensura: Reincarnated** - Tensura contributed the progression layer: EP (energy
  points) shown for captured mobs, named variants, magic crystals as the craft
  ingredients of the tier recipes, and the metal tiers that the pseudocore line is
  built on.

Both are optional at runtime: Pseudocore runs standalone and only layers their
content in when the corresponding mod is present.

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
| Owner | only if the mob has an owner |
| Health | always |
| EP | only if Tensura is loaded and the mob has EP |

Variants read naturally -- *Red Fang Direwolf*, *Emerald Fire Dragon*,
*Black Hippogryph* -- covering Ice and Fire, Tensura, Mobs of Mythology and Mysticism.
An empty pseudocore shows a hint instead.

## Appearance

The item keeps a horn silhouette adapted from Ice and Fire, so it fits the existing
art. When a mob is stored, the item's **three small orbs** tint to that mob's
spawn-egg color -- vivid, and only the orbs. Mob types with no spawn egg get a
stable color derived from their id.

## Ice and Fire dragons stay on Ice and Fire's horn

Pseudocore **does not** capture Ice and Fire's dragons -- they keep using IaF's own
Dragon Horn. The item is blacklisted by default against the three IaF dragons, 62
bosses, eight Tensura and Mysticism encounter mobs (Tensura's orc ranks, goblin,
lizardman and three daemon ranks, plus Mysticism's okami) and villagers/colonists --
79 ids in all -- so it can't be used to pocket an encounter. A capture attempt on a
blacklisted mob simply refuses, naming the mob. The full list is editable in
`config/pseudocore-server.toml`.

## Optional ownership transfer (off by default)

Releasing a mob never changes who owns it, unless a server opts in:

| Gamerule | Default | Effect |
|---|---|---|
| `pseudocoreOwnershipTransfer` | off | Releasing a mob from a Pseudocore makes the releasee its owner |
| `pseudocoreDragonOwnershipTransfer` | off | Releasing a dragon from IaF's Dragon Horn makes the releasee its owner |

```
/gamerule pseudocoreOwnershipTransfer true
/gamerule pseudocoreDragonOwnershipTransfer true
```

## Getting it in-game

Craft a pseudocore (see its recipe in-game) and use it as above -- no configuration.

## Dependencies

| | Mod | Why |
|---|---|---|
| **Required** | Minecraft 1.21.1 + NeoForge 21.1+ | Platform |
| Optional | [Ice and Fire CE](https://www.curseforge.com/minecraft/mc-mods/iceandfire-ce) | Dragon Horn ownership hand-off, and Ice and Fire variant names |
| Optional | Tensura: Reincarnated | EP tooltip, Tensura ownership fields, Tensura variant names |
| Optional | Mobs of Mythology | drake and sporeling variant names |
| Optional | Mysticism | okami variant names |

**Pseudocore runs standalone** -- every integration above is independent. Without
one, the item still works and just omits that mod's names or extras.

## Credits

- Capture/release approach and the horn silhouette modeled on **Ice and Fire** /
  **Ice and Fire CE** (LGPL). A copy of the horn texture is embedded so the item
  still renders without Ice and Fire. Full attribution and the list of modifications
  are in [`NOTICE.md`](NOTICE.md).
- EP display, tier progression, and metal/crystal theming inspired by
  **Tensura: Reincarnated**.

## License

GNU Lesser General Public License v2.1 -- see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.

The horn's model silhouette and body texture are adapted from Ice and Fire / Ice and
Fire CE and remain under **LGPL-2.1**; that third-party material is covered by
[`NOTICE.md`](NOTICE.md), not by this mod's licence alone.
