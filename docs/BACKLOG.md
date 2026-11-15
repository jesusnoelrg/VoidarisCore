- **Config validation:** warn on missing or invalid keys when loading the YAML files, and use `config-version` to migrate old files.- **runServer build drift:** `runServer` downloads the latest Paper 26.3 build (252) instead of the build the API targets (248); consider pinning it with `serverJar`/`paperBuild`.

- **Disabled module commands:** Brigadier commands cannot be unregistered at runtime, so commands of a disabled module stay registered; they should check `ModuleManager#isEnabled` and reply with `general.module-disabled`.
- **runServer integrations:** the test server has no LuckPerms or PlaceholderAPI, so group overrides and placeholders cannot be tested there; consider `downloadPlugins` in `runServer`.
- **Storage migrations untested against MariaDB:** the runner was only validated up to the connection attempt (no MariaDB available while building feat/database_storage); run a V1 migration against a real instance before the first schema ships.
- **Redis cache:** add a Lettuce-backed `Cache` implementation if Voidaris becomes a multi-server network.
- **Test server database.yml:** `run/plugins/VoidarisCore/database.yml` was generated with `enabled: true` during testing; set it back to false or delete it.
