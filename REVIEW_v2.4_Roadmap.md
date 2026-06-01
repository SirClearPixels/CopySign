# CopySign — Feature Review & Improvement Roadmap

**Reviewed version:** 2.3.0
**Date:** 2026-05-30
**Codebase:** ~9,200 LOC Java 21, Paper/Folia API 1.21, targets MC 1.21.7+ (1.21.11 confirmed)

---

## 1. What CopySign Does Today

CopySign is a mature sign copy/paste plugin. Players shift-click a sign to copy its text, per-line colors, and per-side glow state onto a held sign item (stored as NBT), then place the item to paste. On top of that core loop it layers a personal **Sign Library**, admin-managed **Server Templates**, and a substantial support infrastructure.

### Current feature set

| Area | Capability |
|------|-----------|
| **Core copy/paste** | Text, per-line color, per-side glow, regular + hanging signs, sneak-to-copy/paste toggles |
| **Per-player toggle** | `/cs on` / `/cs off`, persisted in `players.yml` |
| **Sign Library** | `save` / `load` / `delete` / `library` GUI; per-player limits via config or granular permissions (`library.limit.N`, `library.unlimited`) |
| **Server Templates** | `templates create/use/delete/list` + GUI; reserved names; delete confirmation with `-force` |
| **Confirmation system** | `/cs confirm` / `/cs cancel` with configurable timeout |
| **Admin** | `/cs reload` (hot-reload of config, messages, templates) |
| **Platform** | Folia + Paper + Spigot via `SchedulerUtil` abstraction; `VersionCompatibility` for per-side glow API |
| **Integrations** | WorldGuard (reflection-based soft dep), bStats (ID 26118), update checker |
| **Infra** | Cooldowns per action, sound effects, async file I/O, backup system, auto-save, sign-data cache with TTL, NBT validation, debug logging |
| **Permissions** | 35+ nodes including granular library limits and bypass nodes |

This is a feature-complete, well-architected plugin. The improvement opportunities below are about hardening, modernization, and breadth — not fixing a broken core.

---

## 2. Top Improvement Opportunities for Next Version

### Tier 1 — Highest leverage

#### 2.1 Drop the hard NBT-API dependency → native PersistentDataContainer
**Current:** Copy data is stored on items via `de.tr7zw.nbtapi.NBTItem`, making NBT-API a required runtime dependency for every server.
**Why it matters:** NBT-API is the single hardest dependency and a recurring source of breakage on Minecraft version bumps (it does internal version reflection). Paper has shipped a stable `PersistentDataContainer` (PDC) API for items since 1.14, and it fully supports the string/boolean data CopySign stores.
**Recommendation:** Migrate item storage to `ItemMeta#getPersistentDataContainer()` with namespaced keys. Keep a one-time read-fallback that reads legacy NBT-API tags and rewrites them to PDC so existing copied items don't break. This removes a dependency, eliminates a whole class of version-bump failures, and is the most future-proofing change available.
**Effort:** Medium. Touches all 6 listeners + `CopySignCommand` + `SavedSignData`. Worth a dedicated phase.

#### 2.2 Add an automated test suite (currently zero tests)
**Current:** No `src/test`, no JUnit in `pom.xml`. All 12 phase summaries describe manual verification.
**Why it matters:** 9,200 LOC across thread-safe managers, version-compat branches, and serialization with **no regression safety net**. Every MC version bump is currently validated by hand (`test-checklist.md`). For the logic that doesn't need a live server — `SavedSignData` serialization round-trips, `NBTValidationUtil`, name validation, config validation, cooldown math, limit-permission parsing — unit tests are cheap and high-value.
**Recommendation:** Add JUnit 5 + MockBukkit. Start with serialization, validation, and limit-resolution. Target the pure-logic utilities first; they're the regression-prone parts.
**Effort:** Medium, incremental.

#### 2.3 Split the monolithic `CopySignCommand` (868 lines)
**Current:** A single 868-line command class dispatches all 15 subcommands; the main `CopySign.java` is 622 lines and `ErrorHandler` is 550.
**Why it matters:** This is the most-edited file and the hardest to test or extend. Adding any new subcommand means touching the giant switch.
**Recommendation:** Extract a sub-command interface (`SubCommand` with `execute`/`tabComplete`/`permission`) and one class per command. Pairs naturally with 2.2 (each command becomes independently testable) and clears the path for new commands in 2.4+.
**Effort:** Medium, mechanical.

### Tier 2 — Strong user-facing wins

#### 2.4 Library/template management quality-of-life
- **Rename** saved signs and templates (currently delete + re-save only).
- **Search / filter** in the library GUI — players hitting the 50-sign limit have no way to find a sign by name.
- **Pagination already exists**, but add category/folder grouping or sort options (by name, by date saved).
- **Import/export** a library to a shareable file or string, so players can move designs between servers.
- **Preview improvements:** the GUI shows lore preview; consider showing front/back side indicators and glow state visually.

#### 2.5 PlaceholderAPI integration
A soft `PlaceholderAPI` hook exposing things like saved-sign count, library limit, toggle state, and template count would let server owners surface CopySign state in scoreboards/menus. Low effort given the existing soft-dependency pattern used for WorldGuard.

#### 2.6 Cross-version / cross-server copy format
The copied NBT currently embeds raw color enum names and a `"regular"`/`"hanging"` type string. Define an explicit, versioned serialization schema (a `format-version` field) so future format changes are forward/backward compatible and library files survive plugin upgrades cleanly. This is insurance for every feature that persists sign data.

### Tier 3 — Polish & ecosystem

#### 2.7 Full Adventure / MiniMessage adoption
The migration to Adventure is underway (`LegacyComponentSerializer.legacySection()`). Next step: let `messages.yml` and library/template names accept **MiniMessage** formatting (gradients, hover, click). This is the modern Paper standard and a visible quality signal.

#### 2.8 Localization shipped, not just "ready"
README claims "multi-language ready" via `messages.yml`, but only one language ships. Provide a `messages_<locale>.yml` mechanism and ship 2–3 community translations (e.g. de, es, zh) to make the claim real.

#### 2.9 Modernize the update checker
If it polls a hardcoded source, point it at the Modrinth/Hangar/SpigotMC API and respect `general.check-for-updates`. Consider notifying admins on join (with `copysign.admin`) rather than only console.

#### 2.10 Documentation & repo hygiene
- The repo root holds **20+ phase-summary / plan markdown files** (`Phase1-…`, `@CopySign…BugReport.md`, etc.). Move these to `docs/archive/` or a `dev/` folder so the root is clean for users browsing the project.
- Add a real `CHANGELOG.md` (there are scattered `RELEASE_NOTES_*.md` but no canonical changelog).
- Consider a `wiki/` or docs site for the 35+ permission nodes and config reference.

---

## 3. Hardening / Risk Areas to Verify

These are flagged for verification in the next cycle, not confirmed defects:

1. **NBT-API version coupling** — `pom.xml` pins item-nbt-api 2.15.5; a server bundling an older NBT-API can break copy/paste. (Resolved entirely by 2.1.)
2. **Reflection-based WorldGuard** — works across WG6/WG7 by method probing; should be covered by an integration smoke test when WG updates.
3. **Folia region scheduling** — the 1-tick deferred back-side write in `SignChangeListener` via `runAtLocationDelayed` is the trickiest concurrency path; a Folia regression test belongs in the manual checklist at minimum.
4. **Config migration** — `ConfigMigrator` + `config-version: 2`; confirm a clean upgrade path is exercised when adding any new config keys in 2.4.
5. **Limit-permission parsing** (`library.limit.N`) — exactly the kind of string-parsing logic that should get a unit test (what wins when a player has both `limit.10` and `limit.50`?).

---

## 4. Suggested 2.4 Scope (prioritized)

1. **PDC migration** (removes NBT-API hard dep) — *biggest future-proofing win*
2. **Test harness** (JUnit 5 + MockBukkit) covering serialization, validation, limit parsing
3. **Command refactor** (one class per subcommand)
4. **Library QoL**: rename + search/filter + import/export
5. **PlaceholderAPI** soft hook
6. **Repo cleanup**: archive dev markdown, add `CHANGELOG.md`

Items 1–3 are structural and unlock everything after them; 4–6 are user-visible wins that fit naturally once the structure is in place.

---

*This document is a planning artifact. None of the Tier-1 items are emergencies — CopySign 2.3.0 is stable and feature-rich. The theme for 2.4 is **future-proofing (kill the NBT-API dependency), provability (tests), and breadth (library QoL + integrations).***
