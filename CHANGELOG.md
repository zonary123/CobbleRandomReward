# Changelog

## [1.1.0] - 2026-11-20

### Added

- **Custom Menu Support for External UI Mods**: Companion mods and custom UI systems (like Rotom Phone, web dashboards, or custom GUI plugins) can now replace CobbleRandomReward's default reward pool and preview menus with their own screens using the new `RandomRewardMenuProvider` SPI.
- **Headless Reward Distribution & Direct Controls**: Companion mods can now roll and distribute random rewards from any configured pool, reload configurations, or trigger legacy reward migrations directly in code without opening a chest menu.
- **Rich Reward Pool & Player ViewModels**: External mods can now easily inspect configured reward pools, entry weights, calculated drop probabilities, and player access contexts using clean, read-only data models.
- **Dedicated Lightweight API JAR**: Published builds now automatically produce and distribute `CobbleRandomReward-...-api.jar`, making it simple for addon developers to build integrations without packaging the whole mod.

## [1.0.4] - 2026-10-18

### Added

- **Built-in Documentation (WIKI.md)**: Added a comprehensive wiki guide that automatically extracts into `config/cobblerandomreward/` on startup, detailing all reward commands, weighted pool configuration, and permissions.

### Changed

- Updated CobbleUtils dependency to version 1.3.8.

## [1.0.3] - 2026-09-05

### Changed

- **Async Thread Management**: Switched reward execution pool to CobbleUtils `AsyncContext` with daemon threads.
