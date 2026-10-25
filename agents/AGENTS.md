# Agent rules and conventions — Pseudocore

**Repo:** `HexaHexes/pseudocore` · **Mod ID:** `pseudocore` · **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 · **Gradle:** 9.2.1 (wrapper committed)
**Jar:** `build/libs/pseudocore-<version>.jar`

Adds a single item — the **Pseudocore** — that captures any mob with all of its NBT,
holds it, and lets you carry/sell/trade it. Inspired by Ice and Fire's Dragon Horn but
generalized to any mob. **Runs standalone** — every mod integration is optional.

**Dependencies**
- Required: none beyond Minecraft/NeoForge.
- Optional (each independently): Tensura (`tensura`) — EP tooltip + Tensura ownership
  fields; ManasCore (`manascore`) — supports Tensura; Ice and Fire CE (`iceandfire`) —
  Dragon Horn ownership mixin + IaF variant names; Mobs of Mythology; Mysticism.

**Test:** `./gradlew runGameTestServer` — GameTests for the Lightman's Currency trade
path. (Note: this repo uses `runGameTestServer`, not `runGametest`.)

**Gotchas**
- **Dependency jars resolve from CurseMaven** (`curse.maven:<slug>-<projectId>:<fileId>`
  coordinates in `build.gradle`) - this is the pattern the rest of the ecosystem runs
  on. Every dependency, core or optional, resolves from Maven; nothing is committed.
- **`NOTICE.md` is mandatory and must never be deleted.** The horn's model silhouette
  and body texture are adapted from Ice and Fire / Ice and Fire CE and remain under
  **LGPL-2.1**; that third-party material is not covered by this mod's licence. Keep the
  full attribution and the list of modifications.
- **No icons.** The `icon/` listing-art folder and the in-game `META-INF/icon.png` have
  been removed per project convention — do not re-add them unless the user explicitly asks.
- Capture is guarded regardless of gamerule: only ownerless mobs or mobs you own; not
  leashed or carrying passengers. The boss/dragon blacklist lives in
  `config/pseudocore-server.toml`.
- Ownership-transfer gamerules (`pseudocoreOwnershipTransfer`,
  `pseudocoreDragonOwnershipTransfer`) both default to `false`.
- The Ice and Fire Dragon Horn ownership mixin is a separate, opt-in integration — if
  Ice and Fire CE is removed the mixin simply does not load.

---

## Branch sync (do this first)

Before starting any new development in this repo:

1. `git status` — confirm what uncommitted work exists. Never disturb or sweep it.
2. `git pull --ff-only` — sync with the remote. A fast-forward is the normal case,
   because we push straight to the shared branch.
3. If the pull would **not** fast-forward (the branch has diverged), or if it would
   collide with local uncommitted work, **stop and ask** rather than merging, rebasing
   or stashing on your own.

## Scope doctrine

- **One mod + bundled datapack.** No KubeJS scripts.
- **Data-driven over code** — prefer JSON/datapack content to Java when both work.
- **Public API only, no mixins** in the colony-gear-style addon mods. Sanctioned
  exception: `ds-tensura-fix` (tensura-dragon-skin-rebalance), a mixin fix mod by design.

## Verification

- Verify with the in-repo GameTest run (`./gradlew runGametest`, or `runGameTestServer`).
  Repos without GameTests verify with `./gradlew build`. Never verify against a live
  CurseForge instance.
- Keep `expectedTests` in `build.gradle` in sync with the `@GameTest` methods.
- **Client-render features cannot be proven headless.** The honest scope is
  "build green + the user's in-game check" - say so rather than overclaiming.


## Build environment

- JDK 21 (`JAVA_HOME` -> JDK 21); the toolchain targets Java 21.
- The Gradle wrapper is committed. `./gradlew build` compiles, `./gradlew runGametest`
  (or `runGameTestServer`) runs the GameTest server.
- **Dependencies resolve through Maven, never local folders.** Every dependency -
  compile-only, `localRuntime`, dev-run - is a CurseMaven coordinate. ManasCore's
  module jars nest under `META-INF/jars/` inside the core artifact and are extracted
  by `extractManascoreModules` for compilation. Gitignored `libs/` and `testmods/`
  folders are retired: CI must resolve every dependency from a clean checkout.
- On `run/` file-lock errors, kill any stale `java` process and retry.

## PowerShell quirks (this org is developed on Windows)

- A `git push` "error" in PowerShell is usually stderr passthrough — success is the
  `hash..hash  branch -> branch` line, not the absence of red text.
- `New-Object Type(args)` breaks on arithmetic — use `::new()` or a parenthesized
  argument.

## Git & release

- Remote is SSH: `git@github.com:HexaHexes/<name>.git`.
- Push only after tests are green **and** the user has given the go.
- Never sweep the user's uncommitted work into a commit — commit only what is intended.
- Commit subjects: concise, imperative.
- **Version bump on finish:** always bump the version when a new feature or bugfix
  is finished.
- **One feature or bugfix per branch:** develop each on its own feature branch and
  merge it into main/master through a pull request with a **squash merge**. The PR
  body must detail the work done, following `.github/pull_request_template.md`.

## House style

- **No icons** unless the user explicitly asks. Icons are being removed across all repos.
- **No emojis, ever.** Prohibited in all code, docs, configs, and commit messages.
- READMEs are **player-facing only**: pitch, mechanics in plain terms, in-game usage,
  required/optional dependencies, credits, license. No build internals, no class names.
- All repos are **LGPL v2.1**, copyright Bitwise Madness. `NOTICE` / attribution files
  are mandatory and must never be deleted.
- Deploy to the Tensura RPG instance **only** when the user asks.

## Naming

- **Java base package:** `org.bitwisemadness.<project-name>` - repo name lowercased,
  separators stripped (e.g. `ds-tensura-fix` -> `org.bitwisemadness.dstensurafix`).
  The Maven `group` in `build.gradle` matches (`org.bitwisemadness`).
- Keep repo name, folder name, `mod_id`, and the Java package aligned; the rename
  checklist in the template's `README.md` walks the whole set.
- **The template (`neoforge-mod-template`) is canonical for org-wide rules.** When a
  rule is added or changed in any repo's `agents/AGENTS.md`, mirror the change into
  the template so new repos inherit it.

## CI & release model

- `ci.yml` runs three checks that back the branch protection rulesets: **junit**
  (build + the JUnit suite, asserted non-vacuous), **gametest** (`runGameTestServer`,
  graded on the `All N required tests passed` log line) and **version-bump** (a beta
  or stable bump must change the version file and nothing else; an `-alpha` bump may
  bundle work).
- `release.yml` is **not yet enabled for the gradle mod repos** - it needs a CI-built jar
  to attach. The datapack repo is the exception: its "build" is a zip, so it has a working
  `release.yml` that tags and publishes on a version bump.
- `version-bump.yml` opens an isolated version-bump PR (only the version file changes).
- CurseForge publishing (`curseforge.yml`) is **deferred** until a mod has a CurseForge
  listing and the `CURSEFORGE_PROJECT_ID` / `CURSEFORGE_API_KEY` secrets. Do not add it
  speculatively.
- Where a release workflow exists, alpha/nightly publishing is opt-in via the
  `ALPHA_RELEASES` repo variable and is **off** by default.

