# Agent rules and conventions â€” Beast Horn

**Repo:** `HexaHexes/beasthorn` Â· **Mod ID:** `beasthorn` Â· **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 Â· **Gradle:** 9.2.1 (wrapper committed)
**Jar:** `build/libs/beasthorn-<version>.jar`

Adds a single item â€” the **Beast Horn** â€” that captures any mob with all of its NBT,
holds it, and lets you carry/sell/trade it. Inspired by Ice and Fire's Dragon Horn but
generalized to any mob. **Runs standalone** â€” every mod integration is optional.

**Dependencies**
- Required: none beyond Minecraft/NeoForge.
- Optional (each independently): Tensura (`tensura`) â€” EP tooltip + Tensura ownership
  fields; ManasCore (`manascore`) â€” supports Tensura; Ice and Fire CE (`iceandfire`) â€”
  Dragon Horn ownership mixin + IaF variant names; Mobs of Mythology; Mysticism.

**Test:** `./gradlew runGameTestServer` â€” GameTests for the Lightman's Currency trade
path. (Note: this repo uses `runGameTestServer`, not `runGametest`.)

**Gotchas**
- **Dependency jars resolve from CurseMaven** (`curse.maven:<slug>-<projectId>:<fileId>`
  coordinates in `build.gradle`) â€” this is the pattern the rest of the ecosystem is
  migrating to. This repo needs no gitignored `libs/` jars for the core build.
- **`NOTICE.md` is mandatory and must never be deleted.** The horn's model silhouette
  and body texture are adapted from Ice and Fire / Ice and Fire CE and remain under
  **LGPL-2.1**; that third-party material is not covered by this mod's licence. Keep the
  full attribution and the list of modifications.
- **No icons.** The `icon/` listing-art folder and the in-game `META-INF/icon.png` have
  been removed per project convention â€” do not re-add them unless the user explicitly asks.
- Capture is guarded regardless of gamerule: only ownerless mobs or mobs you own; not
  leashed or carrying passengers. The boss/dragon blacklist lives in
  `config/beasthorn-server.toml`.
- Ownership-transfer gamerules (`beastHornOwnershipTransfer`,
  `beastHornDragonOwnershipTransfer`) both default to `false`.
- The Ice and Fire Dragon Horn ownership mixin is a separate, opt-in integration â€” if
  Ice and Fire CE is removed the mixin simply does not load.

---

## Branch sync (do this first)

Before starting any new development in this repo:

1. `git status` â€” confirm what uncommitted work exists. Never disturb or sweep it.
2. `git pull --ff-only` â€” sync with the remote. A fast-forward is the normal case,
   because we push straight to the shared branch.
3. If the pull would **not** fast-forward (the branch has diverged), or if it would
   collide with local uncommitted work, **stop and ask** rather than merging, rebasing
   or stashing on your own.

## Scope doctrine

- **One mod + bundled datapack.** No KubeJS scripts.
- **Data-driven over code** â€” prefer JSON/datapack content to Java when both work.
- **Public API only, no mixins** in the colony-gear-style addon mods. Sanctioned
  exception: `ds-tensura-fix` (tensura-dragon-skin-rebalance), a mixin fix mod by design.

## Verification

- The in-repo GameTest run is the **only** CI gate. Never verify against a live
  CurseForge instance.
- Keep `expectedTests` in `build.gradle` in sync with the `@GameTest` methods.
- **Client-render features cannot be proven headless.** The honest scope is
  "build green + the user's in-game check" â€” say so rather than overclaiming.

## Build environment

- JDK 21 (`JAVA_HOME` -> JDK 21); the toolchain targets Java 21.
- The Gradle wrapper is committed. `./gradlew build` compiles, `./gradlew runGametest`
  (or `runGameTestServer`) runs the GameTest server.
- `libs/` (compile-only) and `testmods/` (runtime closure) are **gitignored** staging
  folders. Never commit third-party jars.
- On `run/` file-lock errors, kill any stale `java` process and retry.

## PowerShell quirks (this org is developed on Windows)

- A `git push` "error" in PowerShell is usually stderr passthrough â€” success is the
  `hash..hash  branch -> branch` line, not the absence of red text.
- `New-Object Type(args)` breaks on arithmetic â€” use `::new()` or a parenthesized
  argument.

## Git & release

- Remote is SSH: `git@github.com:HexaHexes/<name>.git`.
- Push only after tests are green **and** the user has given the go.
- Never sweep the user's uncommitted work into a commit â€” commit only what is intended.
- Commit subjects: concise, imperative.

## House style

- **No icons** unless the user explicitly asks. Icons are being removed across all repos.
- READMEs are **player-facing only**: pitch, mechanics in plain terms, in-game usage,
  required/optional dependencies, credits, license. No build internals, no class names.
- All repos are **LGPL v2.1**, copyright Bitwise Madness. `NOTICE` / attribution files
  are mandatory and must never be deleted.
- Deploy to the Tensura RPG instance **only** when the user asks.

## CI & release model

- `ci.yml` runs on pull requests: a build job and a GameTest job. Both assert a
  non-zero result â€” a run that passes vacuously (0 tests) is a failure.
- `release.yml` runs on every push to the default branch, re-runs the tests, and â€” only
  when the version was bumped â€” tags and publishes a GitHub release with a cumulative
  changelog. Alpha/nightly publishing is opt-in via the `ALPHA_RELEASES` repo variable
  and is **off** by default.
- `version-bump.yml` opens an isolated version-bump PR (only the version file changes).
  Beta/stable bumps must not carry other changes; `-alpha` bumps may bundle work.
- CurseForge publishing (`curseforge.yml`) is **deferred** until a mod has a CurseForge
  listing and the `CURSEFORGE_PROJECT_ID` / `CURSEFORGE_API_KEY` secrets. Do not add it
  speculatively.

