# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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