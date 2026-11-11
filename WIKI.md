# 📖 UltraCalendar (CobbleCalendarRewards) Complete Wiki & Configuration Guide

Welcome to the **UltraCalendar** (CobbleCalendarRewards) documentation! UltraCalendar is a feature-rich, monthly advent calendar and streak reward system built for Minecraft Cobblemon servers. It incentivizes active player retention by awarding daily gifts across the entire month—complete with active in-game playtime requirements, timezone-aware day rollover, missed-day catchup margins, and rich reward payouts.

---

## 📌 Table of Contents
1. [Overview & Core Architecture](#-overview--core-architecture)
2. [Commands & Permissions](#-commands--permissions)
3. [Core Mechanics & Calendar Lifecycle](#-core-mechanics--calendar-lifecycle)
   - [Calendar Operating Modes (MONTH_DAY vs STREAK)](#calendar-operating-modes-month_day-vs-streak)
   - [Triple-State Button Display](#triple-state-button-display)
   - [Active Playtime Gating](#active-playtime-gating)
   - [Timezones & Monthly Auto-Reset](#timezones--monthly-auto-reset)
   - [Catchup Margin Days](#catchup-margin-days)
4. [Directory & File Organization](#-directory--file-organization)
5. [Complete JSON Configuration Schema](#-complete-json-configuration-schema)
   - [Global Settings (`config.json`)](#global-settings-configjson)
   - [Daily Calendar Slot (`rewards/day_<X>.json`)](#daily-calendar-slot-rewardsday_xjson)
   - [ItemModel Display Schema](#itemmodel-display-schema)
   - [AdvancedItemChance Reward Schema](#advanceditemchance-reward-schema)
6. [Production JSON Configuration Examples](#-production-json-configuration-examples)
   - [Example 1: Day 1 - Welcome Monthly Opener (`day_1.json`)](#example-1-day-1---welcome-monthly-opener-day_1json)
   - [Example 2: Day 15 - Mid-Month Milestone (`day_15.json`)](#example-2-day-15---mid-month-milestone-day_15json)
   - [Example 3: Day 30 - Grand Finale Mystery Chest (`day_30.json`)](#example-3-day-30---grand-finale-mystery-chest-day_30json)
7. [Database & Persistence Engine](#-database--persistence-engine)
8. [Troubleshooting & FAQ](#-troubleshooting--faq)

---

## 🌟 Overview & Core Architecture

- **Monthly Calendar Grid**: Renders an intuitive 31-day visual calendar interface (up to 5 or 6 chest rows) where players can visually track their claim history, current day's progress, and upcoming milestone rewards.
- **Active Playtime Verification**: Prevent players from logging in for 2 seconds just to click a button and disconnect. UltraCalendar integrates with `CobbleUtilsEvents.PLAY_TIME_EVENT` to require active in-game playtime (e.g. 15 or 30 minutes) before today's reward button unlocks.
- **Playtime Completion Alerts**: The instant an online player reaches the required active playtime for the day, the server triggers a celebratory sound and chat notification inviting them to open `/calendar`.
- **Timezone Synchronization**: Day changes occur strictly according to your configured server timezone (e.g. `"America/New_York"`, `"Europe/Madrid"`, `"UTC"`), preventing time exploitation via client clock manipulation.
- **Async Execution**: Player joins, playtime updates, and database flushes run in the background on UltraCalendar's dedicated `AsyncContext` worker threads.

---

## 💻 Commands & Permissions

Base command aliases (configurable in `config.json`): `/calendar`, `/calendarreward`

### Player Commands
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/calendar` | *None (All Players)* | Opens the monthly calendar interface showing all 31 days and their live statuses. |
| `/calendarreward` | *None (All Players)* | Alias to open the calendar interface. |
| `/calendar open` | *None (All Players)* | Subcommand alias to open the calendar interface. |
| `/calendar gui` | *None (All Players)* | Subcommand alias to open the calendar interface. |
| `/calendar help` | *None (All Players)* | Displays all available calendar commands and usage syntax. |

### Staff & Admin Commands
Admin subcommands can be executed directly (e.g. `/calendar reset`) or under the grouped administration namespace `/calendar admin <subcommand>`.

| Command | Arguments | Permission Node | Default Level | Description |
| :--- | :--- | :--- | :--- | :--- |
| `/calendar reload` | *None* | `ultracalendarreward.reload` | Level 2 (OP) | Hot-reloads configuration files, language strings, and all daily reward definitions without restarting the server. |
| `/calendar reset [player]` | `[player]` | `ultracalendarreward.reset` | Level 2 (OP) | Resets the specified player's (or sender's) calendar progress and daily playtime tracker. |
| `/calendar resetall` | *None* | `ultracalendarreward.resetall` | Level 2 (OP) | Resets calendar progress and daily playtime for all currently online players simultaneously. |
| `/calendar setday <player> <day>` | `<player> <day>` | `ultracalendarreward.setday` | Level 2 (OP) | Sets a player's calendar progress to a specific day (`0` to `maxDay`). |
| `/calendar settime <player> <seconds>` | `<player> <seconds>` | `ultracalendarreward.settime` | Level 2 (OP) | Sets a player's accumulated daily playtime in seconds. |
| `/calendar addtime <player> <seconds>` | `<player> <seconds>` | `ultracalendarreward.addtime` | Level 2 (OP) | Adds bonus active playtime in seconds to a player's daily count. |
| `/calendar claim <player> [day]` | `<player> [day]` | `ultracalendarreward.claim` | Level 2 (OP) | Force claims a reward for a player without consuming their daily eligibility. |
| `/calendar info <player>` | `<player>` | `ultracalendarreward.info` | Level 2 (OP) | Inspects a player's current day, today's claim status, daily/total playtime, and eligibility. |
| `/calendar other <player>` | `<player>` | `ultracalendarreward.other` | Level 2 (OP) | Opens the calendar menu on behalf of another player to view their current UI. |
| `/calendar save` | *None* | `ultracalendarreward.save` | Level 2 (OP) | Flushes all dirty in-memory player data and saves immediately to the database. |

---

## ⚡ Core Mechanics & Calendar Lifecycle

### Calendar Operating Modes (MONTH_DAY vs STREAK)
UltraCalendar supports two architectural game modes configured via `"mode"` in `config.json`:

1. **`MONTH_DAY` (Default - Real Calendar Date)**:
   - Each slot is directly tied to the real day of the month (`1` to `31`), calculated from the server's configured timezone (`LocalDate.now(zone).getDayOfMonth()`).
   - **Future Days**: Days ahead of today's date remain locked in the `notClaimed` state.
   - **Today's Day**: Unlocks as `canClaim` once today's active playtime requirement is reached.
   - **Past Missed Days (Catchup)**: Unclaimed days in the past remain claimable if they fall within the configured catchup margin window (`resetMarginDays`). If missed beyond that margin, they lock permanently for the remainder of the month.
   - **Monthly Rollover**: On the 1st of each month, all player claimed states reset automatically if `autoReset: true`.

2. **`STREAK` (Consecutive Login Rewards)**:
   - Evaluates rewards as a sequential daily login streak (Day 1, Day 2, Day 3...).
   - Each claim unlocks the subsequent day.
   - If a player misses more days than `"resetMarginDays"` (`today - dayClaimed > resetMarginDays`), their streak resets back to Day 1.
   - When a player claims the final configured reward (`maxDay`), the streak completes and resets.

### Triple-State Button Display
Every day button in the monthly grid dynamically renders one of **three distinct visual states**:
1. **`canClaim`**: Today's reward is unlocked! The player has logged in on the correct day (or a valid catchup day) and fulfilled the required playtime. Clicking it claims the reward.
2. **`claimed`**: The player has already claimed this day's reward earlier in the month.
3. **`notClaimed`**: The reward is currently inaccessible. This represents either:
   - A **future day** that hasn't arrived yet on the calendar.
   - **Today's reward**, but the player hasn't finished their required active playtime yet.
   - A **past day** that was missed beyond the configured catchup margin.

> [!NOTE]
> In the calendar menu, each reward button renders an ItemStack count matching the day number (`1` to `31`, capped at `64`), giving players instant visual clarity of day progression in the chest inventory.

### Active Playtime Gating
UltraCalendar supports two layers of playtime requirements:
- **`defaultPlayTime`** (in `config.json`): Baseline playtime applied to all days (e.g. `"15m"`).
- **`requiredTime`** (in individual `day_X.json` files): Overrides the default for specific high-value milestone days (e.g. require `"45m"` on Day 15 and Day 30).

When a player is on the server, UltraCalendar tracks their active minutes. If `sendPlayTimeAlertOnJoin: true`, the player receives a reminder upon joining stating how many minutes of playtime they still need before today's calendar gift becomes claimable.

### Timezones & Monthly Auto-Reset
Configure `"timezone"` in `config.json` with standard IANA timezone identifiers (e.g. `"America/New_York"`, `"Europe/Madrid"`, `"UTC"`):
- At midnight (`00:00:00`) in that timezone, the calendar advances to the next day.
- On the 1st of each month, if `"autoReset": true`, all player claim states reset automatically and a new monthly cycle begins.
- If `"sendMessageReset": true`, a server-wide announcement broadcast alerts players that a new calendar month has started.

### Catchup Margin Days
The `"resetMarginDays"` setting (default: `3`) controls player forgiveness. If a player misses logging in for a day or two, they can still catch up on missed days within the margin window rather than being permanently locked out.

---

## 📁 Directory & File Organization

All configurations and data files reside inside `config/ultracalendar/`:

```text
config/ultracalendar/
├── config.json          # Main settings, timezone, playtime defaults, database
├── WIKI.md              # Auto-extracted in-game reference guide
├── lang/
│   └── en.json          # Translatable messages, GUI titles, and alerts
├── rewards/             # Individual daily reward files (Day 1 through 31)
│   ├── day_1.json
│   ├── day_2.json
│   ├── ...
│   └── day_31.json
└── data/
    └── users/           # Player claims and accumulated playtime
        └── <uuid>.json
```

---

## 📝 Complete JSON Configuration Schema

### Global Settings (`config/ultracalendar/config.json`)

```json
{
  "debug": false,
  "active": true,
  "mode": "MONTH_DAY",
  "autoReset": true,
  "autoPlace": false,
  "sendMessageReset": true,
  "sendPlayTimeAlertOnJoin": true,
  "lang": "en",
  "timezone": "UTC",
  "resetMarginDays": 3,
  "defaultPlayTime": "15m",
  "commands": [
    "calendar",
    "calendarreward"
  ],
  "database": {
    "type": "JSON",
    "database": "calendarrewards",
    "url": "mongodb://localhost:27017",
    "user": "user",
    "password": "password"
  },
  "rows": 5,
  "checkReward": 15
}
```

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `debug` | `Boolean` | `false` | Enable verbose debug logging in the server console. |
| `active` | `Boolean` | `true` | Master switch to enable or disable the calendar system. |
| `mode` | `String` | `"MONTH_DAY"` | Progression mode: `"MONTH_DAY"` (day of month 1-31 with catchup) or `"STREAK"` (consecutive daily login streak). |
| `autoReset` | `Boolean` | `true` | Automatically resets all player calendar progress on the 1st day of every month (or on lost streak in STREAK mode). |
| `autoPlace` | `Boolean` | `false` | Automatically calculates sequential GUI slots for rewards instead of relying on manual `slot` numbers. |
| `sendMessageReset` | `Boolean` | `true` | Broadcast server-wide announcement when the calendar resets at the start of the month. |
| `sendPlayTimeAlertOnJoin` | `Boolean` | `true` | Sends a chat message when a player joins informing them how many minutes of playtime they need today. |
| `lang` | `String` | `"en"` | Language file identifier loaded from `lang/<lang>.json`. |
| `timezone` | `String` | `""` | Server timezone identifier (e.g. `"UTC"`, `"America/New_York"`). Empty string defaults to server host time. |
| `resetMarginDays` | `Integer` | `3` | Margin of days allowed for players to catch up on missed daily rewards (minimum: 2). |
| `defaultPlayTime` | `String` | `"0s"` | Default active playtime required each day before rewards unlock (e.g. `"15m"`, `"30m"`). |
| `commands` | `List<String>` | `["calendar"]` | Command aliases registered to open the calendar GUI. |
| `database` | `Object` | *DataBaseConfig* | Storage backend configuration (`JSON`, `SQLITE`, or `MONGODB`). |
| `rows` | `Short` | `5` | Number of chest GUI rows (1 to 6). 5 rows provide 45 slots, ideal for a 31-day calendar with navigation borders. |
| `checkReward` | `Integer` | `15` | Minutes between automatic chat reminders sent to online players. |

---

### Daily Calendar Slot (`config/ultracalendar/rewards/day_<X>.json`)

```json
{
  "day": 15,
  "slot": 16,
  "dailyTime": true,
  "requiredTime": "30m",
  "canClaim": { ... },
  "claimed": { ... },
  "notClaimed": { ... },
  "rewards": { ... }
}
```

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `day` | `Integer` | `1` | Day of the month this reward belongs to (1 to 31). |
| `slot` | `Integer` | `0` | GUI slot index in the calendar menu (0 to `rows * 9 - 1`). |
| `dailyTime` | `Boolean` | `true` | If `true`, playtime is evaluated for today only. If `false`, counts cumulative monthly playtime. |
| `requiredTime` | `String` | `"0s"` | Specific active playtime required for this day (e.g. `"30m"`). Overrides `defaultPlayTime`. |
| `canClaim` | `Object` | *ItemModel* | Button appearance when today's reward is unlocked and ready to claim. |
| `claimed` | `Object` | *ItemModel* | Button appearance after this day's reward has already been claimed. |
| `notClaimed` | `Object` | *ItemModel* | Button appearance when locked (future day, insufficient playtime, or missed). |
| `rewards` | `Object` | *AdvancedItemChance* | Complete reward distribution engine (items, currency, commands, animations). |

---

### ItemModel Display Schema
Defines the visual item shown for each of the three states:

```json
{
  "item": "minecraft:chest_minecart",
  "customModelData": 0,
  "displayname": "<gold><bold>Day %day% - Ready to Claim!</bold></gold>",
  "lore": [
    "<gray>You have fulfilled your active daily playtime.",
    "",
    "<yellow>Required Playtime: <green>%required_playtime%",
    "<green><bold>CLICK TO CLAIM YOUR REWARD!</bold></green>"
  ]
}
```

---

## 💡 Production JSON Configuration Examples

### Example 1: Day 1 - Welcome Monthly Opener (`day_1.json`)
Requires 5 minutes of active playtime to welcome players to the new month:

```json
{
  "day": 1,
  "slot": 10,
  "dailyTime": true,
  "requiredTime": "5m",
  "canClaim": {
    "item": "cobblemon:poke_ball",
    "displayname": "<green><bold>Day 1: Welcome Pack (Ready!)</bold></green>",
    "lore": [
      "<gray>Welcome to this month's calendar!",
      "",
      "<yellow>Includes PokéBalls and Starter Berries.",
      "<green><bold>Click to claim!</bold></green>"
    ]
  },
  "claimed": {
    "item": "minecraft:minecart",
    "displayname": "<dark_gray>Day 1: Claimed</dark_gray>",
    "lore": [
      "<gray>You already collected your Day 1 reward."
    ]
  },
  "notClaimed": {
    "item": "minecraft:chest_minecart",
    "displayname": "<yellow>Day 1: Locked</yellow>",
    "lore": [
      "<gray>Play on the server for 5 minutes today to unlock!",
      "",
      "<yellow>Required Playtime: <red>5m"
    ]
  },
  "rewards": {
    "id": "day_1_rewards",
    "showMenu": true,
    "title": "<green>Day 1 Rewards",
    "giveAll": true,
    "cumulativeLootTable": false,
    "amountRewardsPermission": { "": 1 },
    "animation": "NONE",
    "lootTable": {
      "": [
        {
          "chance": 100.0,
          "item": "cobblemon:poke_ball",
          "minAmount": 10,
          "maxAmount": 10,
          "commands": [
            "eco give %player% 500"
          ]
        }
      ]
    }
  }
}
```

---

### Example 2: Day 15 - Mid-Month Milestone (`day_15.json`)
A major mid-month milestone requiring 30 minutes of active playtime with a CS:GO roll animation:

```json
{
  "day": 15,
  "slot": 24,
  "dailyTime": true,
  "requiredTime": "30m",
  "canClaim": {
    "item": "cobblemon:ultra_ball",
    "displayname": "<gold><bold>Day 15: Mid-Month Milestone (READY!)</bold></gold>",
    "lore": [
      "<gray>Congratulations on reaching mid-month!",
      "",
      "<yellow>Playtime completed: <green>30m/30m",
      "<green><bold>CLICK TO ROLL YOUR MILESTONE LOOT!</bold></green>"
    ]
  },
  "claimed": {
    "item": "minecraft:minecart",
    "displayname": "<dark_gray>Day 15: Claimed</dark_gray>",
    "lore": [
      "<gray>You claimed the Mid-Month Milestone."
    ]
  },
  "notClaimed": {
    "item": "minecraft:barrier",
    "displayname": "<red>Day 15: Locked</red>",
    "lore": [
      "<gray>Requires 30 minutes of active gameplay on Day 15."
    ]
  },
  "rewards": {
    "id": "day_15_rewards",
    "showMenu": true,
    "title": "<gold><bold>Day 15 Milestone Chest</bold></gold>",
    "giveAll": false,
    "cumulativeLootTable": false,
    "amountRewardsPermission": { "": 1 },
    "animation": "CSGO",
    "newSound": {
      "sound": "minecraft:ui.toast.challenge_complete",
      "volume": 1.0,
      "pitch": 1.0
    },
    "lootTable": {
      "": [
        {
          "chance": 35.0,
          "item": "cobblemon:ability_patch",
          "minAmount": 1,
          "maxAmount": 1,
          "commands": [
            "eco give %player% 5000"
          ]
        },
        {
          "chance": 65.0,
          "item": "cobblemon:ultra_ball",
          "minAmount": 16,
          "maxAmount": 16,
          "commands": [
            "eco give %player% 2500"
          ]
        }
      ]
    }
  }
}
```

---

### Example 3: Day 30 - Grand Finale Mystery Chest (`day_30.json`)
The ultimate month-end reward awarding Master Balls, legendary items, and server announcements:

```json
{
  "day": 30,
  "slot": 38,
  "dailyTime": true,
  "requiredTime": "45m",
  "canClaim": {
    "item": "cobblemon:master_ball",
    "displayname": "<light_purple><bold>Day 30: Grand Monthly Finale (READY!)</bold></light_purple>",
    "lore": [
      "<gray>You have shown true dedication this month!",
      "",
      "<yellow>Playtime completed: <green>45m/45m",
      "<light_purple><bold>CLICK TO CLAIM YOUR GRAND PRIZE!</bold></light_purple>"
    ]
  },
  "claimed": {
    "item": "minecraft:minecart",
    "displayname": "<dark_gray>Day 30: Grand Prize Claimed</dark_gray>",
    "lore": [
      "<gray>Monthly calendar completed!"
    ]
  },
  "notClaimed": {
    "item": "minecraft:structure_void",
    "displayname": "<red>Day 30: Finale Locked</red>",
    "lore": [
      "<gray>Unlocks on the 30th after 45 minutes of active play."
    ]
  },
  "rewards": {
    "id": "day_30_grand_prize",
    "showMenu": true,
    "title": "<light_purple><bold>Grand Monthly Finale!</bold></light_purple>",
    "giveAll": true,
    "cumulativeLootTable": false,
    "amountRewardsPermission": { "": 1 },
    "animation": "CSGO",
    "newSound": {
      "sound": "minecraft:ui.toast.challenge_complete",
      "volume": 1.0,
      "pitch": 0.8
    },
    "lootTable": {
      "": [
        {
          "chance": 100.0,
          "item": "cobblemon:master_ball",
          "minAmount": 2,
          "maxAmount": 2,
          "displayname": "<purple><bold>Master Ball</bold></purple>",
          "commands": [
            "broadcast <gold>%player% <yellow>has completed the <light_purple>Monthly Calendar <yellow>and claimed the Grand Finale!",
            "eco give %player% 20000"
          ]
        }
      ]
    }
  }
}
```

---

## 💾 Database & Persistence Engine

UltraCalendar supports three data storage backends:
1. **JSON (`type: "JSON"`)**: File-based storage saved in `config/ultracalendar/data/<uuid>.json`.
2. **SQL (`type: "SQLITE" | "MYSQL" | "MARIADB" | "H2"`)**: Relational database storage powered by CobbleUtils' SQLService with automatic schema creation and connection pooling.
3. **MongoDB (`type: "MONGODB"`)**: Centralized high-performance NoSQL database for cross-server networks where player calendar claims and active playtime sync across all proxy servers.

---

## ❓ Troubleshooting & FAQ

### Why can't a player claim today's reward even though they are on the server?
Check their active daily playtime. If `"defaultPlayTime"` is `"15m"`, the player must spend 15 active minutes playing on the server before the button switches from `notClaimed` to `canClaim`.

### Can I reset a player's calendar to test from Day 1?
Yes! Execute:
```bash
/calendar reset <player>
```

### What happens in months with fewer than 31 days (like February)?
Days 29, 30, and 31 will simply never unlock during shorter months. You can safely place high-value grand finale rewards on Day 28 so every month guarantees a finale claim.
