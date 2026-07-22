package com.kingpixel.ultracalendar.config;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.models.Rewards;
import lombok.Data;
import lombok.ToString;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
  private String lang;
  private int resetMarginDays;
  private List<String> commands;
  private DataBaseConfig database;
  private short rows;
  private int checkReward;
  private List<Rewards> rewards;

  public Config() {
    this.debug = false;
    this.active = true;
    this.autoReset = true;
    this.autoPlace = false;
    this.sendMessageReset = true;
    this.lang = "en";
    this.resetMarginDays = 3;
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
    if (resetMarginDays < 2) resetMarginDays = 2;
    if (commands == null) {
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

      LocalDate now = LocalDate.now();
      String year = String.valueOf(now.getYear());
      String month2 = String.format("%02d", now.getMonthValue());
      String month1 = String.valueOf(now.getMonthValue());

      Path yearMonthDir2 = rootRewardsDir.resolve(year).resolve(month2);
      Path yearMonthDir1 = rootRewardsDir.resolve(year).resolve(month1);

      Path activeDir = null;
      if (Files.exists(yearMonthDir2) && hasJsonFiles(yearMonthDir2)) {
        activeDir = yearMonthDir2;
      } else if (Files.exists(yearMonthDir1) && hasJsonFiles(yearMonthDir1)) {
        activeDir = yearMonthDir1;
      } else {
        activeDir = rootRewardsDir;
      }

      List<Path> jsonFiles;
      if (activeDir.equals(rootRewardsDir)) {
        File[] files = activeDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        jsonFiles = new ArrayList<>();
        if (files != null) {
          for (File f : files) jsonFiles.add(f.toPath());
        }
      } else {
        jsonFiles = UtilsFile.getAllJsonFiles(activeDir);
      }

      if (jsonFiles.isEmpty()) {
        createRewards(activeDir);
        return;
      }

      List<Integer> days = new ArrayList<>();
      List<Integer> slots = new ArrayList<>();

      for (Path file : jsonFiles) {
        try {
          Rewards reward = UtilsFile.read(file, Rewards.class);
          if (reward != null) {
            reward.check();
            if (days.contains(reward.getDay())) {
              UltraCalendar.LOGGER.warn("Duplicate day found in rewards file: " + file.toAbsolutePath());
            }
            if (slots.contains(reward.getSlot())) {
              UltraCalendar.LOGGER.warn("Duplicate slot found in rewards file: " + file.toAbsolutePath());
            }
            days.add(reward.getDay());
            slots.add(reward.getSlot());
            rewards.add(reward);
          }
        } catch (Exception e) {
          UltraCalendar.LOGGER.error("Could not read reward file: " + file.toAbsolutePath(), e);
        }
      }

      rewards.sort(Comparator.comparingInt(Rewards::getDay));
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error reading rewards files for " + UltraCalendar.MOD_NAME, e);
    }
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
}