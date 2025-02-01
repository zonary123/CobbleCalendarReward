package com.kingpixel.cobblecalendar.config;

import com.google.gson.Gson;
import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.Rewards;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Getter
@Data
@ToString
public class Config {
  private boolean debug;
  private boolean active;
  private boolean autoReset;
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

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleCalendar.PATH, "config.json",
      el -> {
        Gson gson = Utils.newGson();
        CobbleCalendar.config = gson.fromJson(el, Config.class);
        if (CobbleCalendar.config.rewards != null) {
          for (Rewards reward : CobbleCalendar.config.rewards) {
            Utils.writeFileAsync(CobbleCalendar.PATH_REWARDS, reward.getDay() + ".json",
              Utils.newGson().toJson(reward));
          }
          CobbleCalendar.config.rewards = null;
        }

        String data = gson.toJson(CobbleCalendar.config);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleCalendar.PATH, "config.json",
          data);
        if (!futureWrite.join()) {
          CobbleCalendar.LOGGER.fatal("Could not write config.json file for " + CobbleCalendar.MOD_NAME + ".");
        }
      });

    if (!futureRead.join()) {
      CobbleCalendar.LOGGER.info("No config.json file found for" + CobbleCalendar.MOD_NAME + ". Attempting to generate one.");
      Gson gson = Utils.newGson();
      CobbleCalendar.config = this;
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleCalendar.PATH, "config.json",
        data);
      createRewards();
      if (!futureWrite.join()) {
        CobbleCalendar.LOGGER.fatal("Could not write config.json file for " + CobbleCalendar.MOD_NAME + ".");
      }
    }

    CobbleCalendar.config.readRewards();
  }

  private void readRewards() {
    if (rewards == null) rewards = new ArrayList<>();
    rewards.clear();
    File folder = Utils.getAbsolutePath(CobbleCalendar.PATH_REWARDS);
    if (!folder.exists()) {
      folder.mkdirs();
      createRewards();
      return;
    }

    for (File file : folder.listFiles()) {
      CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleCalendar.PATH_REWARDS, file.getName(),
        el -> {
          Gson gson = Utils.newGson();
          Rewards reward = gson.fromJson(el, Rewards.class);
          reward.check();
          rewards.add(reward);
          Utils.writeFileAsync(CobbleCalendar.PATH_REWARDS, reward.getDay() + ".json",
            Utils.newGson().toJson(reward));
        });
      if (!futureRead.join()) {
        CobbleCalendar.LOGGER.fatal("Could not read rewards file for " + CobbleCalendar.MOD_NAME + ".");
      }
    }

    rewards.sort(Comparator.comparingInt(Rewards::getDay));

  }

  private void createRewards() {
    var rewards = List.of(new Rewards(1, 0),
      new Rewards(2, 1),
      new Rewards(3, 2),
      new Rewards(4, 3),
      new Rewards(5, 4),
      new Rewards(6, 5),
      new Rewards(7, 6),
      new Rewards(8, 7),
      new Rewards(9, 8),
      new Rewards(10, 9),
      new Rewards(11, 10),
      new Rewards(12, 11),
      new Rewards(13, 12),
      new Rewards(14, 13),
      new Rewards(15, 14),
      new Rewards(16, 15),
      new Rewards(17, 16),
      new Rewards(18, 17),
      new Rewards(19, 18),
      new Rewards(20, 19),
      new Rewards(21, 20),
      new Rewards(22, 21),
      new Rewards(23, 22),
      new Rewards(24, 23),
      new Rewards(25, 24),
      new Rewards(26, 25),
      new Rewards(27, 26),
      new Rewards(28, 27),
      new Rewards(29, 28),
      new Rewards(30, 29),
      new Rewards(31, 30)
    );
    this.rewards = rewards;
    for (Rewards reward : rewards) {
      Utils.writeFileAsync(CobbleCalendar.PATH_REWARDS, reward.getDay() + ".json",
        Utils.newGson().toJson(reward));
    }
  }

  public int maxDay() {
    return getRewards().stream()
      .mapToInt(Rewards::getDay)
      .max()
      .orElse(0);

  }
}