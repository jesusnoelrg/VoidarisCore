# VoidarisCore - Rules

- **Edited by:** jesusnoelrg
- **Date:** 11/14/2026

Voidaris core plugin. Paper 26.3 (build 248), Java 25, Gradle Kotlin DSL.

## Commands
- **Compile:** `./gradlew compileJava -q` (use `build` only when a jar is needed).
- **Jar:** `./gradlew build -q` → `build/libs/`.
- **Test server:** `./gradlew runServer` (creates `run/`; never read it, only `tail -n 50 run/logs/latest.log`).
- A `Stop` hook compiles automatically at the end of each turn and reports only the errors.

## Change Classification (Declare BEFORE coding)
- **New:** New functionality.
- **Improvement:** Optimization of existing functionality.
- **Fix:** Bug fix (Requires root cause analysis + before/after evidence: `runServer` log, console output or in-game reproduction steps. There is no automated test suite yet; do not set one up without asking).
- **Build:** Infrastructure/configuration.
- **Non-code:** Documentation/file relocation (Does not require validation via tests).

## Communication and Language
- **User interaction:** ALWAYS respond to `jesusnoelrg` in SPANISH.
- **Code and repository:** Commits, branches, PRs, code, identifiers, and documentation MUST be 100% in English (no "Spanglish"). Do not translate the user's Spanish-language documentation without their permission.

## Strict Guidelines
- **Approval required:** NEVER install dependencies or submit a PR to the `main` branch without first consulting `jesusnoelrg`.
- **Modules:** `club.voidaris.core.<module>` — one package per module (spawn, tpa, homes, chat, etc.).
- **Each module:** an on/off toggle under `modules:` in `config.yml`, its settings in its own file (`teleports.yml`, `homes.yml`, `warps.yml`, `kits.yml`, `chat-format.yml`, `announces.yml`, or a new one registered in `ConfigManager`), messages in `messages.yml`, and permissions in `plugin.yml`.
- **Runtime data** (homes, warp locations, cooldowns) goes in `plugins/VoidarisCore/data/`, never in the default config files.
- **Player-facing messages (Spanish):** Use MiniMessage with the prefix `<dark_purple><bold>Voidaris</bold> <dark_gray><bold>»</bold>`<white>{message}.
- **Plugin permissions:** `voidaris.<module>.<action>`.
- No hardcoded text: everything must reside in `messages.yml`.
- Core integrations: LuckPerms API, PlaceholderAPI, Vault (economy).
- **Root cause:** No error is considered resolved without evidence of its root cause; mere hypotheses are not accepted.
- **Coding style:** Use modern Java-style indentation. Comment ONLY complex logic (the "why"), never what the code itself does (the "what"). Avoid over-engineering.
- **Scope management:** Log any errors or ideas that fall outside the current scope in `docs/BACKLOG.md` using the `>>` append operator—no need to read the entire file.
- **Sub-agents:** Use them EXCLUSIVELY for exploring the source code (read-only) or for performing isolated parallel checks, in order to protect the main context.

## Paper API
- The 26.x API may differ from what you are familiar with. When in doubt, verify the signature using `javap -cp <jar> <class>` rather than reading through entire JARs or source files.

## Server context
Analysis and decisions: `docs/` (do not read these unless the task requires it).

## Git and PR workflow
- **Branches:** Must follow the format `(feat|fix|chore|build|docs|ci|refactor|test)/snake_case_name` and target the `dev` branch.
- **Commits:** Must follow the format `prefix(simplified_file): description in lowercase` (e.g., `docs(prd): add users table`).
- **PRs (Pull Requests):** Consult `.github/pull_request_template.md` just before running `gh pr create`. The title must follow the format: `prefix(module): description in lowercase`. Use bold text for the applicable classification and strikethrough (`~~`) for those that do not apply. Include the `Root cause` and `fix` sections ONLY in the case of fixes.