# Beast Horn

A single low-tech fantasy item — the **Beast Horn** — that captures a mob with all of
its data, holds it safely, and lets you carry it, sell it, or trade it to another player.
The mob inside keeps everything: its name, its health, its inventory, its owner, and any
modded data stored on it.

Inspired by Ice and Fire's Dragon Horn, but generalized to work on **any** mob.

## What the horn does

**Capture** — right-click a mob to store it. The mob's entire data is written into the
item, so nothing about it is lost — which is what makes it safe to trade. Capture is
guarded:

- You can only capture a mob that is **ownerless**, or that **you own** — never someone
  else's pet.
- A mob that is **leashed** or **carrying a rider** cannot be captured.

**Release** — use the horn on the top of a block to release the mob exactly as captured,
at its original identity (so anything bound to that mob by ID — deeds and the like —
still resolves).

**Trade** — because the mob lives entirely in the item, handing the horn to another
player is all it takes. Whoever holds it, and whoever releases it, gets the same mob.

## What's inside (tooltip)

A filled horn always shows its contents, in this order:

| Line | Shown |
|---|---|
| Name | only if the mob has a custom name |
| Race | always — the mob's variant, then its type |
| Owner | only if the mob has an owner |
| Health | always |
| EP | only if Tensura is loaded and the mob has EP |

Variants read naturally — *Red Fang Direwolf*, *Emerald Fire Dragon*,
*Black Hippogryph* — covering Ice and Fire, Tensura, Mobs of Mythology and Mysticism.
An empty horn shows a hint instead.

## Appearance

The horn reuses Ice and Fire's horn look, so it fits the existing art. When a mob is
stored, the horn's **three small orbs** tint to that mob's spawn-egg color — vivid, and
only the orbs. Mob types with no spawn egg get a stable color derived from their id.

## Ice and Fire dragons stay on Ice and Fire's horn

Beast Horn **does not** capture Ice and Fire's dragons — they keep using IaF's own Dragon
Horn. The horn is blacklisted by default against the three IaF dragons **and 71 bosses**,
so it can't be used to pocket an encounter. A capture attempt on a blacklisted mob simply
refuses, naming the mob. The full list is editable in `config/beasthorn-server.toml`.

## Optional ownership transfer (off by default)

Releasing a mob never changes who owns it, unless a server opts in:

| Gamerule | Default | Effect |
|---|---|---|
| `beastHornOwnershipTransfer` | off | Releasing a mob from a Beast Horn makes the releasee its owner |
| `beastHornDragonOwnershipTransfer` | off | Releasing a dragon from IaF's Dragon Horn makes the releasee its owner |

```
/gamerule beastHornOwnershipTransfer true
/gamerule beastHornDragonOwnershipTransfer true
```

## Getting it in-game

Craft the horn (see its recipe in-game) and use it as above — no configuration.

## Dependencies

| | Mod | Why |
|---|---|---|
| **Required** | Minecraft 1.21.1 + NeoForge 21.1+ | Platform |
| Optional | [Ice and Fire CE](https://www.curseforge.com/minecraft/mc-mods/iceandfire-ce) | Dragon Horn ownership hand-off, and Ice and Fire variant names |
| Optional | Tensura: Reincarnated | EP tooltip, Tensura ownership fields, Tensura variant names |
| Optional | Mobs of Mythology | drake and sporeling variant names |
| Optional | Mysticism | okami variant names |

**Beast Horn runs standalone** — every integration above is independent. Without one,
the horn still works and just omits that mod's names or extras.

## Credits

- Horn model and texture adapted from **Ice and Fire** / **Ice and Fire CE** (LGPL). A
  copy of the horn texture is embedded so the item still renders without Ice and Fire.
  Full attribution and the list of modifications are in [`NOTICE.md`](NOTICE.md).
- Capture/release approach modeled on Ice and Fire CE's Dragon Horn.

## License

GNU Lesser General Public License v2.1 — see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.

The horn's model silhouette and body texture are adapted from Ice and Fire / Ice and Fire
CE and remain under **LGPL-2.1**; that third-party material is covered by
[`NOTICE.md`](NOTICE.md), not by this mod's licence alone.
