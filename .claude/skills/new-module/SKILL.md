---
name: new-module
description: Checklist and mandatory structure for creating a new module within the project. Use this skill whenever a user requests the creation of a module to ensure consistency (packages, `Module` class, configuration, `plugin.yml`, `README`) without needing to read existing code.
---
# Checklist: New Module

## Summary
A strict guide for initializing a new module in the project. It ensures that all required components are implemented consistently, saving tokens by eliminating the need to read existing modules to replicate their structure.

## When to Use
- When a user asks to "create a new module" or add a new, self-contained feature.
- When initializing the base structure for a new piece of functionality.

## Steps
Ensure that you strictly follow this checklist when creating a module:

1. **Create the package:** Generate the appropriate base directory or package.
2. **Create the `Module` class:** Implement the module's main class.
3. **Add to `config`:** Add the module toggle under `modules:` in `config.yml`, and put its settings in its own file in `src/main/resources/` (reuse an existing one if it fits, otherwise create it and register it in `club.voidaris.core.config.ConfigManager`).
4. **Define messages:** Specify and add the messages that the module will send to players within the language or message files.
5. **Define permissions in `plugin.yml`:** Register the necessary permission nodes for this module directly in the `plugin.yml` file.
6. **Register commands:** Implement the command logic and ensure they are registered within the plugin.
7. **Update the `README`:** Add a line to the `README.md` file documenting the new module.

## Gotchas
- **Do not read existing modules:** Rely on this checklist and the project's established standard structure to avoid consuming tokens on unnecessary context.
- Verify that all 7 steps have been completed before finalizing the module creation.