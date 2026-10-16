# Beast Horn

A **NeoForge 1.21.1** addon that adds a single low-tech, fantasy item — the **Beast Horn** — which captures a mob with **all of its NBT data**, holds it safely, and lets you carry it, sell it, or trade it to another player. The mob inside keeps everything: its name, its health, its inventory, its owner, and any modded data stored on it.

Inspired by Ice and Fire's Dragon Horn, but generalized to work on **any** mob.

---

## Why it exists

Ice and Fire's Dragon Horn already does exactly what you want — stores a whole entity in an item — but it is hard-gated to dragons. Releasing a dragon also has no way to hand ownership to whoever receives it.

Rather than patch Ice and Fire (which would need re-patching on every CE update, and would put dragon storage at risk), Beast Horn ships as its **own standalone item** that reuses the same proven capture/release approach. It works alongside Ice and Fire rather than replacing it.

---

## What the horn does

### Capture

Right-click a mob to store it in the horn. The mob's **entire NBT** is written into the item — this is the hard requirement, and it is what makes the item useful for trading: nothing about the mob is lost.

**Capture is guarded** (these rules apply regardless of any gamerule setting):

- You can only capture a mob that is **ownerless**, or that **you own**. You cannot horn someone else's pet.
- A mob that is **leashed** or **carrying passengers** cannot be captured. Releasing those would strand a leash or duplicate a rider.

### Release

Use the horn on the top face of a block to release the stored mob, restored exactly as captured, at its original UUID (so anything bound to that mob by UUID — deeds and the like — still resolves).

### Trade

Because the mob lives entirely in the item's NBT, handing the horn to another player is all it takes. Whoever holds the horn, and whoever releases it, gets the same mob.

---

## Ice and Fire CE compatibility

Beast Horn **does not work with Ice and Fire's dragons**. They keep using IaF's own Dragon Horn, which stores dragon-specific data the Beast Horn does not reproduce.

The horn is blacklisted by default against:

- `iceandfire:fire_dragon`
- `iceandfire:ice_dragon`
- `iceandfire:lightning_dragon`

This list is editable in the server config.

### Dragon ownership transfer (optional mixin)

Ice and Fire CE is **optional**. When it *is* installed, Beast Horn adds a small mixin that makes IaF's own Dragon Horn transfer dragon ownership to whoever releases it — mirroring the capture guard, so you cannot re-home someone else's dragon by accident.

This behavior is off by default and is entirely separate from the rest of the mod. If Ice and Fire CE is removed, the mixin is simply not loaded and Beast Horn still works normally.

---

## Gamerules

Both ownership-transfer behaviors **default to `false`**. Out of the box the horn is purely a storage and trading item — releasing a mob never changes who owns it.

| Gamerule | Default | Effect |
| --- | --- | --- |
| `beastHornOwnershipTransfer` | `false` | When `true`, releasing a mob from a Beast Horn makes the releasee that mob's owner. |
| `beastHornDragonOwnershipTransfer` | `false` | When `true`, releasing a dragon from IaF's Dragon Horn makes the releasee that dragon's owner. |

Enable them individually:

```
/gamerule beastHornOwnershipTransfer true
/gamerule beastHornDragonOwnershipTransfer true
```

They are independent, so a server can turn one on without the other.

---

## Tooltip

A filled horn shows its contents, always labelled and always in this order:

| Line | Shown |
| --- | --- |
| `Name:` | Only if the mob has a custom name |
| `Race:` | Always — the mob's entity type |
| `Owner:` | Only if the mob has an owner |
| `Health:` | Always |
| `EP:` | Only if Tensura is loaded **and** the mob has EP |

An empty horn instead shows a hint telling you what to do with it.

---

## Appearance

The horn reuses Ice and Fire's horn model and texture, so it fits the existing art with no new asset style.

When a mob is stored, the horn's **three small orbs** tint to that mob's **spawn-egg primary colour** — vivid, and only the orbs. The horn body itself is never tinted.

This is done as a two-layer split in the model: the body is one texture layer left uncoloured, and the orbs are a second layer of pure white that the tint handler multiplies by the mob's colour. So the colour you see is exactly the egg colour — no desaturation from colour maths.

Mob types with no spawn egg get a stable colour derived from their entity id, so the same mob always tints the same way.

---

## Tensura integration

If **Tensura** is loaded, the tooltip also shows the mob's **EP** (its `aura + magicule`, read from the captured NBT).

Ownership transfer through the Beast Horn writes Tensura's own ownership fields — permanent owner, temporary owner, and the storage dirty flag — rather than guessing at NBT paths, so Tensura's systems (following, naming, subordination) see the new owner correctly.

Tensura is **optional**. Without it, the addon loads normally and simply omits the EP line and Tensura-specific ownership handling.

---

## Requirements

- **Minecraft** 1.21.1
- **NeoForge** 21.1.233 or later
- **Java** 21

### Optional

- **Ice and Fire CE** — enables the Dragon Horn ownership mixin
- **Tensura** — enables the EP tooltip and Tensura ownership transfer

---

## Building

```
gradlew build
```

The output jar is written to `build/libs/`.

---

## Credits

- Horn model and texture adapted from **Ice and Fire** / **Ice and Fire CE** (LGPL). A copy of the horn texture is embedded so the item still renders if Ice and Fire is not installed. Full attribution and the list of modifications are in [`NOTICE.md`](NOTICE.md).
- Capture/release approach modeled on Ice and Fire CE's Dragon Horn.
- Built on **NeoForge** and **ModDevGradle**.

## License

MIT.
