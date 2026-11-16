- **Config validation:** warn on missing or invalid keys when loading the YAML files, and use `config-version` to migrate old files.- **runServer build drift:** `runServer` downloads the latest Paper 26.3 build (252) instead of the build the API targets (248); consider pinning it with `serverJar`/`paperBuild`.

- **Disabled module commands:** Brigadier commands cannot be unregistered at runtime, so commands of a disabled module stay registered; they should check `ModuleManager#isEnabled` and reply with `general.module-disabled`.
- **runServer integrations:** the test server downloads LuckPerms but not PlaceholderAPI, so placeholders cannot be tested there; add it to `downloadPlugins` in `runServer`.
- **Storage migrations untested against MariaDB:** the runner was only validated up to the connection attempt (no MariaDB available while building feat/database_storage); run a V1 migration against a real instance before the first schema ships.
- **Redis cache:** add a Lettuce-backed `Cache` implementation if Voidaris becomes a multi-server network.
- **Test server database.yml:** `run/plugins/VoidarisCore/database.yml` was generated with `enabled: true` during testing; set it back to false or delete it.
- **Cosmetics SQL untested in-game:** the `cosmetic_selections` SELECT/UPSERT were not exercised by a real player join (no client could join the test server, which also lacks LuckPerms/PlaceholderAPI for `%luckperms_prefix%`).
- **`/warps` alias:** README used to list `/warps`; `/warp` without arguments lists warps now, add the alias if players expect it.
