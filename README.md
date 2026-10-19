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

The horn is blacklisted by default against Ice and Fire's three dragons — `iceandfire:fire_dragon`, `iceandfire:ice_dragon`, `iceandfire:lightning_dragon` — **and 71 bosses**, covering every boss-bearing mod in the pack so a horn cannot be used to pocket an encounter.

A capture attempt on a blacklisted mob simply refuses, naming the mob.

The full list is editable in the server config, written to `config/beasthorn-server.toml` the first time a world is opened.

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
| `Race:` | Always — the mob's variant, then its entity type |
| `Owner:` | Only if the mob has an owner |
| `Health:` | Always |
| `EP:` | Only if Tensura is loaded **and** the mob has EP |

Where a mob has a **variant**, it is prepended to the race rather than shown in brackets — `Red Fang Direwolf`, `Emerald Fire Dragon`, `Dark Brown One Eyed Owl`. A species word is dropped when the variant already names the animal, so a `Direwolf` variant reads as `Direwolf` rather than `Direwolf Direwolf`. Hippogryphs are the far end of that rule: their variant already names the animal, so it stands in **for** the race entirely — `Black Hippogryph`, not `Black Hippogryph Hippogryph`.

Variant display covers Ice and Fire (dragons, sea serpents, hippogryphs, trolls, death worms), Tensura, Mobs of Mythology and Mysticism — twenty species in all. It is purely presentational: a mob whose variant cannot be read — an unknown id, or a mod that has since been removed — still stores, transports and releases correctly. It simply reads as the plain race name.

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

- **Ice and Fire CE** — enables the Dragon Horn ownership mixin, and the IaF variant names
- **Tensura** — enables the EP tooltip, Tensura ownership transfer, and the Tensura variant names
- **Mobs of Mythology** — enables the drake and sporeling variant names
- **Mysticism** — enables the okami variant names

Each is optional independently. Without one, Beast Horn loads normally and simply omits that mod's contribution — its variant names read as the plain race, nothing more.

---

## Building

```
gradlew build
```

The output jar is written to `build/libs/`.

### The `libs/` folder

A fresh clone will **not** build until you drop twelve third-party jars into a `libs/` folder. They are gitignored — this mod does not redistribute them — and the build compiles directly against them:

| Files in `libs/` | Needed for |
| --- | --- |
| `manascore-architectury-neoforge-4.0.0.2.jar`<br>`manascore-attribute-neoforge-4.0.0.2.jar`<br>`manascore-command-neoforge-4.0.0.2.jar`<br>`manascore-config-neoforge-4.0.0.2.jar`<br>`manascore-inventory-neoforge-4.0.0.2.jar`<br>`manascore-keybind-neoforge-4.0.0.2.jar`<br>`manascore-network-neoforge-4.0.0.2.jar`<br>`manascore-race-neoforge-4.0.0.2.jar`<br>`manascore-skill-neoforge-4.0.0.2.jar`<br>`manascore-storage-neoforge-4.0.0.2.jar` | the EP tooltip and Tensura ownership transfer |
| `mobs_of_mythology-neoforge-1.21.1-3.0.0.jar` | drake and sporeling variant names |
| `mysticism-neoforge-2.2.1.jar` | okami variant names |

Take them from the matching projects' own downloads. Everything else — Tensura, ManasCore's main jar, and Ice and Fire CE — resolves automatically from CurseMaven and needs nothing by hand.

Missing any of these twelve produces a Gradle compile failure, not a silent partial build.

---

## Credits

- Horn model and texture adapted from **Ice and Fire** / **Ice and Fire CE** (LGPL). A copy of the horn texture is embedded so the item still renders if Ice and Fire is not installed. Full attribution and the list of modifications are in [`NOTICE.md`](NOTICE.md).
- Capture/release approach modeled on Ice and Fire CE's Dragon Horn.
- Built on **NeoForge** and **ModDevGradle**.

## License

MIT — see [`LICENSE`](LICENSE).

The horn's model silhouette and body texture are adapted from Ice and Fire / Ice and Fire CE and remain under **LGPL-2.1**; that third-party material is not covered by this mod's MIT licence. See [`NOTICE.md`](NOTICE.md) for the full attribution and the list of modifications.
