# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.3] - 2026-10-16

### Added

- **Cross-Server Playtime Synchronization**: Playtime requirements now synchronize across multiple servers on proxy networks with MySQL, MariaDB, and MongoDB.
- **Playtime Completion Notification**: Players now receive a chat message as soon as they have played the necessary time to claim their reward.
- **Login Playtime Reminders**: Added reminders when joining the server displaying how much playtime is still needed today.
- **Configurable Timezone**: Added timezone option to synchronize daily calendar resets across servers in different regions.
- **Daily and Total Playtime Options**: Rewards can now require either daily playtime or overall total playtime.
- **Asynchronous Menu Loading**: Opening the reward calendar is now completely smooth and non-blocking, fetching player data asynchronously in the background so the server never freezes or stutters.
- **Multi-Database Support & In-Memory Caching**: Modernized database structure with high-speed memory caching, offering full support for SQLite, MySQL, MariaDB, H2, and MongoDB via CobbleUtils services.
- **Complete Administration Command Suite**: Added a full set of administration commands with rich in-game feedback and console support:
  - `/calendar settime <player> <seconds>` and `/calendar addtime <player> <seconds>` to adjust daily playtime.
  - `/calendar claim <player> [day]` to force claim rewards for players directly into their inventory.
  - `/calendar info <player>` to inspect player progress, playtime, and claim eligibility.
  - `/calendar resetall` to reset progress for all online players simultaneously (ideal for new events or seasons).
  - `/calendar save` / `/calendar saveall` to immediately persist all pending data to the database.
  - `/calendar help` to display available commands.
  - `/calendar other <player>` to open the calendar interface on behalf of another player.
  - Full feedback messages and console execution compatibility for `/calendar reset`, `/calendar setday`, and `/calendar reload`.
- **Command Aliases for Players**: Added `/calendar open` and `/calendar gui` so players can open their calendar using explicit subcommands in addition to `/calendar`.
- **Grouped Admin Command Namespace**: All administrative commands can now also be accessed under `/calendar admin <subcommand>` (e.g., `/calendar admin reset`, `/calendar admin reload`).

### Changed

- **Standardized Operator Permissions (Level 2)**: Adjusted all administrative commands from owner-only (level 4) to standard operator permission level 2 (`ultracalendarreward.*`), making them accessible to server staff and moderators.
- **Command Architecture Modularization**: Restructured internal command logic into clean `base` and `admin` modules for improved stability and maintainability.
- **Single-Query Batch Saving (`saveAll`)**: Periodic background saves now group all pending player updates into a single batch request instead of firing multiple individual queries, drastically reducing database load.
- **Dirty-Flag Change Tracking**: The server now tracks exact player modifications in memory, ensuring only players with actual data changes are written to disk or the database.
- **Non-Blocking Player Disconnects**: Saving player data on quit is now fully asynchronous without freezing the server main thread.
- **Graceful Shutdown Protection**: When the server is stopping, individual disconnect saves are avoided to prevent shutdown freezes, letting the batch save process all pending data cleanly.
- **Intelligent Background Task Lifecycle**: Background tasks (auto-saving and claim reminders) are now actively tracked and safely shut down during server stopping or reloading, preventing duplicate tasks and resource leaks.
- **Config-Driven Reward Reminders**: Periodic reward alert intervals now adapt dynamically to the `checkReward` setting in `config.json`.
- **Date & Reward Lookup Caching**: Optimized daily epoch calculations and reward queries into instant in-memory lookups, eliminating hundreds of redundant calendar and timezone calculations per second on the game loop.
- **Thread-Safe Reward Delivery**: Player item deliveries and calendar menu updates are now safely dispatched to the server thread, preventing inventory concurrency issues.
- **Architecture & Codebase Modernization**: Refactored internal classes to adhere to Single Responsibility and Clean Code standards, decomposing complex UI handlers and event registrations into modular, readable methods with zero dead code.
- Added playtime placeholders (`%remaining%`, `%required%`, `%playtime%`) to item titles and lore in the calendar menu.
- Upgraded CobbleUtils library to version 1.3.7 for better stability and connection handling.

### Removed

- **Redis Overhead**: Completely removed redundant Redis dependencies and pub/sub message listeners, keeping data synchronization clean, direct, and focused on the database.

### Fixed

- Removed silent errors across the mod, ensuring server administrators get clear warnings in the console if a timezone ID is invalid or a database operation fails.

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

