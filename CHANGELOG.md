# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.3] - 2026-10-15

### Added

- **Cross-Server Playtime Synchronization**: Playtime requirements now synchronize across multiple servers on proxy networks with MySQL, MariaDB, and MongoDB.
- **Playtime Completion Notification**: Players now receive a chat message as soon as they have played the necessary time to claim their reward.
- **Login Playtime Reminders**: Added reminders when joining the server displaying how much playtime is still needed today.
- **Configurable Timezone**: Added timezone option to synchronize daily calendar resets across servers in different regions.
- **Daily and Total Playtime Options**: Rewards can now require either daily playtime or overall total playtime.

### Changed

- Enhanced user data saving when disconnecting or changing servers to prevent playtime loss.
- Added playtime placeholders (`%remaining%`, `%required%`, `%playtime%`) to item titles and lore in the calendar menu.

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

