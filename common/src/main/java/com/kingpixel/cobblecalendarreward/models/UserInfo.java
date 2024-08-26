package com.kingpixel.cobblecalendarreward.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
import com.kingpixel.cobblecalendarreward.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendarreward.managers.DailyRewardsManager;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 15/08/2024 17:03
 */
@Getter
@Setter
@ToString
public class UserInfo implements Serializable {
  private UUID uuid;
  private String name;
  private short day;
  private LocalDate dayclaimed;
  private LocalDate lastJoin;

  public UserInfo() {
    this.uuid = UUID.randomUUID();
    this.name = "";
    this.day = 0;
    this.dayclaimed = LocalDate.now();
    this.lastJoin = LocalDate.now();
  }

  public UserInfo(ServerPlayerEntity player) {
    this.uuid = player.getUuid();
    this.name = player.getGameProfile().getName();
    this.day = 0;
    this.dayclaimed = LocalDate.now().minusDays(1);
    this.lastJoin = LocalDate.now();
  }

  public UserInfo(UUID uuid, String name, short day, LocalDate dayclaimed, LocalDate lastJoin) {
    this.uuid = uuid;
    this.name = name;
    this.day = day;
    this.dayclaimed = dayclaimed;
    this.lastJoin = lastJoin;
  }

  public boolean canClaim() {
    LocalDate today = LocalDate.now();
    return today.isAfter(dayclaimed);
  }

  public boolean canClaim(int day) {
    LocalDate today = LocalDate.now();
    return today.isAfter(dayclaimed) && day == this.day + 1;
  }


  public void writeInfo(UUID uuid) {
    File dir = Utils.getAbsolutePath(DailyRewardsManager.PATH_USER_INFO);
    if (!dir.exists()) {
      dir.mkdirs();
    }

    File file = new File(dir, uuid.toString() + ".json");
    try (FileWriter writer = new FileWriter(file)) {
      Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
        .disableHtmlEscaping()
        .create();
      gson.toJson(this, writer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void computeDay() {
    long daysBetween = ChronoUnit.DAYS.between(this.getDayclaimed(), this.getLastJoin());

    if (CobbleCalendarReward.config.isDebug()) {
      CobbleCalendarReward.LOGGER.info("Days between: " + daysBetween);
    }

    if (CobbleCalendarReward.config.isAutoReset()) {
      if (this.getDay() >= CobbleCalendarReward.config.maxDay() + 1) {
        reset(true);
      } else if (daysBetween >= 2) {
        reset(false);
      }
    } else {
      if (this.getDay() >= CobbleCalendarReward.config.maxDay() + 1) {
        reset(true);
      }
    }


    this.setLastJoin(LocalDate.now());
  }

  public void reset(boolean completeall) {
    if (completeall) {
      this.setDayclaimed(LocalDate.now());
    } else {
      this.setDayclaimed(LocalDate.now().minusDays(1));
    }
    this.setDay((short) 0);
    DatabaseClientFactory.databaseClient.updateUserInfo(this);
  }

  public void claim() {
    this.setDayclaimed(LocalDate.now());
    this.setDay((short) (getDay() + 1));
  }
}
