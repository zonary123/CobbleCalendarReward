package com.kingpixel.ultracalendar.config;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.models.Rewards;
import lombok.Data;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Data
@ToString
public class Config {
  private boolean debug;
  private boolean active;
  private boolean autoReset;
  private boolean autoPlace;
  private boolean sendMessageReset;
  private boolean sendPlayTimeAlertOnJoin;
  private String lang;
  private String timezone;
  private int resetMarginDays;
  private DurationValue defaultPlayTime;
  private List<String> commands;
  private DataBaseConfig database;
  private short rows;
  private int checkReward;
  private List<Rewards> rewards;

  private transient volatile java.time.ZoneId parsedZoneId;
  private transient volatile java.util.Map<Integer, Rewards> rewardsByDay;

  public Config() {
    this.debug = false;
    this.active = true;
    this.autoReset = true;
    this.autoPlace = false;
    this.sendMessageReset = true;
    this.sendPlayTimeAlertOnJoin = true;
    this.lang = "en";
    this.timezone = "";
    this.resetMarginDays = 3;
    this.defaultPlayTime = DurationValue.ofMillis(0);
    this.commands = new ArrayList<>();
    this.commands.add("calendar");
    this.commands.add("calendarreward");
    this.database = new DataBaseConfig(
      DataBaseType.JSON,
      "calendarrewards",
      "mongodb://localhost:27017",
      "user",
      "password"
    );
    this.rows = 5;
    this.checkReward = 15;
    this.rewards = null;
  }

  private void check() {
    parsedZoneId = null;
    rewardsByDay = null;
    if (resetMarginDays < 2) resetMarginDays = 2;
    if (defaultPlayTime == null) defaultPlayTime = DurationValue.ofMillis(0);
    if (timezone == null) timezone = "";
    if (commands == null || commands.isEmpty()) {
      commands = new ArrayList<>();
      commands.add("calendar");
      commands.add("calendarreward");
    }
    if (database == null) {
      database = new DataBaseConfig(
        DataBaseType.JSON,
        "calendarrewards",
        "mongodb://localhost:27017",
        "user",
        "password"
      );
    }
  }

  public void init() {
    try {
      Path configFile = UltraCalendar.getPath().resolve("config.json");
      UltraCalendar.config = UtilsFile.readOrCreate(configFile, Config.class, Config::new);

      if (UltraCalendar.config.rewards != null && !UltraCalendar.config.rewards.isEmpty()) {
        Path rewardsFolder = UltraCalendar.getPath().resolve("rewards");
        Files.createDirectories(rewardsFolder);
        for (Rewards reward : UltraCalendar.config.rewards) {
          UtilsFile.writeAsync(rewardsFolder.resolve(reward.getDay() + ".json"), reward);
        }
        UltraCalendar.config.rewards = null;
      }

      UltraCalendar.config.check();
      UtilsFile.writeAsync(configFile, UltraCalendar.config);
      UltraCalendar.config.readRewards();
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error initializing config for " + UltraCalendar.MOD_NAME, e);
    }
  }

  private void readRewards() {
    if (rewards == null) rewards = new ArrayList<>();
    rewards.clear();

    Path rootRewardsDir = UltraCalendar.getPath().resolve("rewards");
    try {
      if (!Files.exists(rootRewardsDir)) {
        Files.createDirectories(rootRewardsDir);
        createRewards(rootRewardsDir);
        return;
      }

      Path activeDir = determineActiveRewardsDir(rootRewardsDir);
      List<Path> jsonFiles = resolveJsonFiles(activeDir, rootRewardsDir);

      if (jsonFiles.isEmpty()) {
        createRewards(activeDir);
        return;
      }

      loadRewardsFromFiles(jsonFiles);
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error reading rewards files for " + UltraCalendar.MOD_NAME, e);
    }
  }

  private Path determineActiveRewardsDir(Path rootRewardsDir) {
    LocalDate now = LocalDate.now(getParsedZoneId());
    String year = String.valueOf(now.getYear());
    String month2 = String.format("%02d", now.getMonthValue());
    String month1 = String.valueOf(now.getMonthValue());

    Path yearMonthDir2 = rootRewardsDir.resolve(year).resolve(month2);
    Path yearMonthDir1 = rootRewardsDir.resolve(year).resolve(month1);

    if (Files.exists(yearMonthDir2) && hasJsonFiles(yearMonthDir2)) {
      return yearMonthDir2;
    } else if (Files.exists(yearMonthDir1) && hasJsonFiles(yearMonthDir1)) {
      return yearMonthDir1;
    }
    return rootRewardsDir;
  }

  private List<Path> resolveJsonFiles(Path activeDir, Path rootRewardsDir) {
    if (activeDir.equals(rootRewardsDir)) {
      File[] files = activeDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
      List<Path> jsonFiles = new ArrayList<>();
      if (files != null) {
        for (File f : files) jsonFiles.add(f.toPath());
      }
      return jsonFiles;
    }
    return UtilsFile.getAllJsonFiles(activeDir);
  }

  private void loadRewardsFromFiles(List<Path> jsonFiles) {
    Set<Integer> days = new HashSet<>();
    Set<Integer> slots = new HashSet<>();

    for (Path file : jsonFiles) {
      try {
        Rewards reward = UtilsFile.read(file, Rewards.class);
        if (reward != null) {
          reward.check();
          if (!days.add(reward.getDay())) {
            UltraCalendar.LOGGER.warn("Duplicate day {} found in rewards file: {}", reward.getDay(), file.toAbsolutePath());
          }
          if (!slots.add(reward.getSlot())) {
            UltraCalendar.LOGGER.warn("Duplicate slot {} found in rewards file: {}", reward.getSlot(), file.toAbsolutePath());
          }
          rewards.add(reward);
        }
      } catch (Exception e) {
        UltraCalendar.LOGGER.error("Could not read reward file: {}", file.toAbsolutePath(), e);
      }
    }

    rewards.sort(Comparator.comparingInt(Rewards::getDay));
    Map<Integer, Rewards> map = new HashMap<>();
    for (Rewards r : rewards) {
      map.put(r.getDay(), r);
    }
    this.rewardsByDay = Map.copyOf(map);
  }

  private boolean hasJsonFiles(Path dir) {
    File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".json"));
    return files != null && files.length > 0;
  }

  private void createRewards(Path dir) {
    try {
      Files.createDirectories(dir);
      for (int day = 1; day <= 31; day++) {
        Rewards reward = new Rewards(day, day - 1);
        UtilsFile.writeAsync(dir.resolve(day + ".json"), reward);
        if (this.rewards != null) {
          this.rewards.add(reward);
        }
      }
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error creating default rewards for " + UltraCalendar.MOD_NAME, e);
    }
  }

  public int maxDay() {
    return rewards == null ? 0 : rewards.size();
  }

  public @NotNull ZoneId getParsedZoneId() {
    if (parsedZoneId == null) {
      synchronized (this) {
        if (parsedZoneId == null) {
          if (timezone != null && !timezone.isBlank()) {
            try {
              parsedZoneId = ZoneId.of(timezone.trim());
            } catch (Exception e) {
              UltraCalendar.LOGGER.warn("Invalid timezone ID '{}' in config, falling back to system default timezone: {}",
                timezone, e.getMessage());
              parsedZoneId = ZoneId.systemDefault();
            }
          } else {
            parsedZoneId = ZoneId.systemDefault();
          }
        }
      }
    }
    return parsedZoneId;
  }

  public @Nullable Rewards getReward(int day) {
    if (rewardsByDay != null) {
      return rewardsByDay.get(day);
    }
    if (rewards == null) return null;
    for (Rewards reward : rewards) {
      if (reward != null && reward.getDay() == day) return reward;
    }
    return null;
  }

  public @NotNull DurationValue getDefaultPlayTime() {
    if (defaultPlayTime == null) {
      defaultPlayTime = DurationValue.ofMillis(0);
    }
    return defaultPlayTime;
  }

  public @NotNull List<Rewards> getRewards() {
    if (rewards == null) {
      rewards = new ArrayList<>();
    }
    return rewards;
  }
}