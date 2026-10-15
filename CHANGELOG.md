# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.3] - 2026-10-16

### Added

- **Daily Playtime Tracking**: Rewards can now require players to accumulate daily playtime (or total playtime) before claiming.
- **Playtime Completion Notification**: Players receive a chat notification as soon as they reach the required daily playtime to claim their reward.
- **Login Playtime Reminders**: Added an optional reminder when joining the server displaying how much playtime is still required today.
- **Configurable Timezone**: Added `timezone` configuration option to synchronize daily calendar resets across different server regions.
- **Asynchronous Menu Loading**: Opening the reward calendar is now non-blocking, loading player data in the background without freezing the server.
- **Multi-Database Support & In-Memory Caching**: Database structure backed by high-speed memory caching, supporting SQLite, MySQL, MariaDB, H2, and MongoDB via CobbleUtils.
- **Complete Administration Command Suite**: Added a full set of administration commands with in-game feedback and console support:
  - `/calendar settime <player> <seconds>` & `/calendar addtime <player> <seconds>`: Set or add to a player's daily playtime.
  - `/calendar claim <player> [day]`: Force claim a reward for a player directly.
  - `/calendar info <player>`: Inspect a player's current streak, daily playtime, and claim eligibility.
  - `/calendar resetall`: Reset calendar progress for all online players at once.
  - `/calendar save` / `/calendar saveall`: Manually trigger a database save of all pending data.
  - `/calendar other <player>`: Open the calendar menu for another player.
  - `/calendar reload`: Reload configurations and rewards with console and chat feedback.
  - `/calendar help`: Display available commands.
- **Command Aliases for Players**: Added `/calendar open` and `/calendar gui` as subcommands to open the calendar interface.
- **Grouped Admin Namespace**: Administration commands can be executed directly (e.g. `/calendar reset`) or grouped under `/calendar admin <subcommand>`.
- **Playtime Placeholders**: Added `%remaining%`, `%required%`, `%playtime%`, `%daily_playtime%`, and `%total_playtime%` for item titles, lores, and chat messages.

### Changed

- **Standardized Operator Permissions (Level 2)**: Adjusted all administrative commands to standard operator permission level 2 (`ultracalendarreward.*`) instead of owner-only level 4.
- **Single-Query Batch Saving (`saveAll`)**: Periodic background saves now group pending player updates into a single batch query instead of firing multiple individual queries.
- **Dirty-Flag Change Tracking**: Tracks modified player data in memory, saving only players who actually have pending changes.
- **Non-Blocking Player Disconnects**: Saving player data on quit is fully asynchronous, preventing server lag on player leaves.
- **Graceful Shutdown Protection**: Skips individual quit saves when the server is stopping to prevent shutdown hangs, delegating final persistence to the batch save.
- **Managed Background Tasks**: Auto-saving and reminder tasks are safely tracked and cancelled on server stop or reload.
- **Dynamic Reminder Interval**: Periodic reward alert intervals adapt dynamically to the `checkReward` setting in `config.json`.
- **In-Memory Reward and Timezone Caching**: Cached daily epoch and reward lookups in memory to avoid repetitive timezone calculations on the game loop.
- **Thread-Safe Reward Delivery**: Reward item grants and command dispatches are safely executed on the server main thread.
- **Architecture & Codebase Modernization**: Refactored internal code into cohesive `base` and `admin` command packages and decomposed monolithic UI methods into modular units.
- Upgraded CobbleUtils library to version 1.3.7.

### Fixed

- Removed silent exceptions across the mod, ensuring clear logging in the console if a configuration error or database issue occurs.

## [2.0.2] - 2026-10-12

### Changed

- **License**: Changed project license from GPL-3.0 to CC-BY-NC-4.0 (Non-Commercial).
- **Cobblemon 1.8.0 Compatibility**: Updated compatibility and dependencies to support Cobblemon 1.8.0 (Minecraft 1.21.1).

## [2.0.1] - 2026-08-01

### Fixed

- Fixed a MongoDB database integration issue that prevented user data from saving and loading correctly.

## [2.0.0] - 2026-07-22

### Added

- Mod renamed from CobbleCalendarRewards to UltraCalendar.
- Support for organize calendars by subfolders like `Year/Month` (e.g. `rewards/2026/07/1.json`) alongside standard flat root calendars.
- Support for SQL databases (`SQLite`, `MySQL`, `MariaDB`, `H2`).
- Fully customizable close button in language configuration.

### Changed

- Improved database and file storage integrations using CobbleUtils.
- General performance and menu responsiveness optimizations.

### Fixed

- Fixed hardcoded close button position and behavior in the menu.

