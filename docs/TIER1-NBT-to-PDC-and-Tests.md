# Tier 1 — NBT-API Removal + Test Harness (v2.4.0)

Status: **implemented, all tests green, jar builds.** Target version bumped to `2.4.0-SNAPSHOT`.

---

## 1. Dropped the hard NBT-API dependency → native PersistentDataContainer

### What changed
Item copy/paste data is now stored using Bukkit's native
`PersistentDataContainer` (PDC) instead of the third-party NBT-API plugin.
`plugin.yml` changed from `depend: [ NBTAPI ]` to `softdepend: [ WorldGuard, NBTAPI ]`,
so **server owners no longer need to install NBT-API** — the single biggest
barrier-to-entry and version-bump fragility is gone.

### New classes (the single storage seam)
| Class | Role |
|-------|------|
| `Util/SignItemData` | Immutable value object for the copied-sign payload (front, back, per-side color, per-side glow, type). Pure — no Bukkit coupling. |
| `Util/SignItemStorage` | The **one** place that maps `SignItemData` ↔ an item's PDC. `read` / `write` / `has` / `clear` / `stripLegacy` / `migrateIfLegacy`. All 7 former `new NBTItem(...)` sites now call this. |
| `Util/LegacyNbtBridge` | Isolated, classloader-safe NBT-API access (guarded by `isAvailable()` + `catch (Throwable)`). Reads/strips legacy tags only. |
| `Listeners/SignMigrationListener` | On `InventoryOpenEvent`, rewrites any legacy copied-sign items in the container to PDC. |

### Call sites migrated (7)
`SignCopyListener`, `SignPlaceListener`, `SignLibraryGUIListener`,
`ServerTemplateGUIListener`, `SignLibraryManager`, `ServerTemplateManager`,
`CopySignCommand` (clear / save / load / template-create / template-load).

PDC details: lowercase namespaced keys (`copied_sign_front`, …); booleans stored as
`PersistentDataType.BYTE`; colors omitted when `null` (no-color copies round-trip
correctly).

### Backward compatibility (one-version bridge — per your decision)
- **Libraries & server templates are 100% safe** — they persist as YAML
  (`SavedSignData`), never touched NBT-API.
- **Already-copied working items** keep working and are migrated. Two distinct
  mechanisms — important not to conflate them:
  - *Read fallback (does NOT persist):* `SignItemStorage.read` falls back to legacy tags
    via `LegacyNbtBridge` so an old item still **pastes/saves correctly** — but reading
    alone does not rewrite the item to PDC.
  - *Persisting migration:* `migrateIfLegacy` actually rewrites the item to PDC. It runs
    in `SignMigrationListener` on **container open** (chests/barrels/shulkers) and on
    **player join** (scans the player's own inventory, covering hotbar/backpack items
    that never fire an inventory-open event).
- **Residual risk (documented in README):** an item that is *never* held by a joining
  player and *never* in an opened container during the bridge release won't be persisted
  to PDC; if NBT-API is later removed, that specific item loses its copy data
  (recoverable by re-copy). Effectively this only hits items in chests that are never
  reopened — unavoidable without scanning every unloaded chest in the world.

### Bonus correctness improvements made along the way
- **Per-side glow now preserved on library save & template create.** The old code used
  the deprecated single-glow `SavedSignData` constructor (collapsing both sides);
  reading through `SignItemData` lets us keep true per-side glow.
- **`/copysign clear` now also strips legacy NBT-API tags**, so clearing an old copied
  item actually wipes it (PDC-clear alone would leave legacy tags readable via the
  fallback).
- **`ErrorHandler` logger made null-safe at class init** — it previously called
  `CopySign.getInstance().getLogger()` in a static initializer, which NPEs outside a
  running server and blocked unit testing. Now falls back to a standalone logger.

---

## 2. Automated test harness (previously zero tests)

Added JUnit 5 + Mockito (`test` scope) and the Surefire plugin. **27 tests, all green.**

| Test class | Covers |
|-----------|--------|
| `SignItemDataTest` (6) | Value-object semantics: null handling, color presence, combined vs per-side glow, `isHanging`. Pure. |
| `SavedSignDataTest` (7) | YAML round-trip; per-side + legacy single-glow decode; interior blank-line preservation; default fill. Pins the **persisted format** that must survive upgrades. |
| `NBTValidationUtilTest` (7) | 32KB cap, line-count/length limits, null/empty handling. |
| `PermissionsLibraryLimitTest` (7) | `library.limit.N` precedence via mocked `Player` — unlimited > admin > highest numeric limit > config default (the exact "both limit.10 and limit.50 → 50" case). |

Tests are deliberately Bukkit-free (pure logic + `YamlConfiguration` + Mockito) to avoid
the uneven PDC support in mock-server frameworks; the PDC binding itself is validated by
the build plus in-game smoke testing.

### Latent issue surfaced by the tests (flagged, not fixed)
`SavedSignData.loadFromConfigurationSection` uses `split("\n")` without a `-1` limit, so
**trailing blank lines are dropped on load** (a sign with blank lines 3–4 loads as 2
lines). Interior blanks are fine. Pinned in `documentsTrailingBlankLineCollapseOnLoad`.
Candidate Tier-2 fix: `split("\n", -1)` on load.

---

## 3. On the third ask — splitting `CopySignCommand`

Recommendation: **defer to its own pass, do it last.** The NBT work already removed the
duplicated key-handling blocks from `CopySignCommand` (clear/save/load/template now go
through `SignItemStorage`), capturing a chunk of the file-split's value. The remaining
win — one class per subcommand for independent testability — is real but is a large,
orthogonal mechanical refactor; interleaving it with this change would only add churn.
Sequence: **migrate → test → then split.**

---

## Verification
- `mvn clean package` → BUILD SUCCESS, `CopySign-2.4.0-SNAPSHOT.jar` produced.
- `mvn test` → 27 passed, 0 failed.
- `grep de.tr7zw src/main` → only `LegacyNbtBridge` (NBT-API fully isolated).

### Still needs in-game smoke testing (PDC can't be unit-tested under a mock server)
1. Copy a sign → place it → text/color/glow apply.
2. `save` to library → `load` → place.
3. Template `create` → `use` → place.
4. `clear` removes data and restores the default item name.
5. **Upgrade path:** with NBT-API installed, place/open a chest holding a 2.3.0-copied
   item and confirm it still pastes (migration).
