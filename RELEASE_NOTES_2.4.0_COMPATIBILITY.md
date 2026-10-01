# 2.4.0 compatibility candidate

These changes extend the existing, unpublished 2.4 work; they do not replace its PDC migration, library rename/search, or MiniMessage features.

- Compile production code against the Spigot API and package Adventure dependencies needed on Spigot.
- Preserve native Adventure messages, inventory titles, names, lore, and sign events on Paper/Folia, with Bukkit string fallbacks on Spigot.
- Reflect Folia scheduler calls without linking Spigot to Folia-only classes; keep entity work on entity schedulers and block work on region schedulers.
- Run save-completion player callbacks on the player's scheduler, guard toggle YAML access, and avoid reading a player's name from async save work.
- Guard delayed sign updates against a removed/replaced sign and route sound/metrics work back to the player scheduler.
- Ignore cancelled placement/edit events.
- Preserve automatic recognition of new regular, wall, hanging, and wall-hanging signs, including Poplar, without a fixed wood list. An explicit administrator allowlist continues to restrict sign types.
- Merge the 12 GitHub README commits into the existing local development history without discarding either side.

This candidate compiles against Spigot APIs 26.1, 26.1.1, 26.1.2, 26.2, and 26.3. Java 25 is required by 26.x servers; the plugin keeps Java 21 bytecode for older servers. The full automated suite and separate Spigot fallback tests are reported in the task compatibility report. Runtime API probes do not establish full player-driven gameplay coverage. Folia 26.3 has no official downloadable build at the time of testing.
