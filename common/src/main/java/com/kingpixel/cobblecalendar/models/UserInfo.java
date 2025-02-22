package com.kingpixel.cobblecalendar.models;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendar.managers.DailyRewardsManager;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Author: Carlos Varas Alonso - 15/08/2024 17:03
 */
@Getter
@Setter
@ToString
public class UserInfo implements Serializable {
  private UUID uuid;
  private String name;
  private short day;
  private long dayclaimed;
  private long lastJoin;

  public UserInfo() {
    this.uuid = UUID.randomUUID();
    this.name = "";
    this.day = 0;
    this.dayclaimed = LocalDate.now().toEpochDay();
    this.lastJoin = LocalDate.now().toEpochDay();
  }

  public UserInfo(ServerPlayerEntity player) {
    this.uuid = player.getUuid();
    this.name = player.getGameProfile().getName();
    this.day = 0;
    this.dayclaimed = LocalDate.now().minusDays(1).toEpochDay();
    this.lastJoin = LocalDate.now().toEpochDay();
  }

  public UserInfo(UUID uuid, String name, short day, LocalDate dayclaimed, LocalDate lastJoin) {
    this.uuid = uuid;
    this.name = name;
    this.day = day;
    this.dayclaimed = dayclaimed.toEpochDay();
    this.lastJoin = lastJoin.toEpochDay();
  }

  public boolean canClaim() {
    LocalDate today = LocalDate.now();
    return today.isAfter(LocalDate.ofEpochDay(dayclaimed));
  }

  public boolean canClaim(int day) {
    LocalDate today = LocalDate.now();
    return today.isAfter(LocalDate.ofEpochDay(dayclaimed)) && day == this.day + 1;
  }

  public void writeInfo(UUID uuid) {
    File dir = Utils.getAbsolutePath(DailyRewardsManager.PATH_USER_INFO);
    if (!dir.exists()) {
      dir.mkdirs();
    }

    File file = new File(dir, uuid.toString() + ".json");

    Utils.writeFileAsync(file, Utils.newWithoutSpacingGson().toJson(this));
  }

  public void computeDay() {
    LocalDate dayClaimedDate = LocalDate.ofEpochDay(this.dayclaimed);
    LocalDate lastJoinDate = LocalDate.ofEpochDay(this.lastJoin);
    long daysBetween = ChronoUnit.DAYS.between(dayClaimedDate, lastJoinDate);

    if (CobbleCalendar.config.isDebug()) {
      CobbleCalendar.LOGGER.info("Days between: " + daysBetween);
    }

    boolean shouldReset = this.getDay() >= CobbleCalendar.config.maxDay();

    if (CobbleCalendar.config.isAutoReset()) {
      shouldReset = shouldReset || daysBetween >= CobbleCalendar.config.getResetMarginDays() + 1;
    }

    if (CobbleCalendar.config.isDebug()) {
      CobbleCalendar.LOGGER.info("Should reset: " + shouldReset);
    }

    if (shouldReset) {
      reset(this.getDay() >= CobbleCalendar.config.maxDay());
    }

    this.setLastJoin(LocalDate.now().toEpochDay());
  }

  public boolean isFinish() {
    if (this.getDay() >= CobbleCalendar.config.maxDay()) {
      reset(true);
      return true;
    }
    return false;
  }

  public void reset(boolean completeall) {
    if (completeall) {
      this.setDayclaimed(LocalDate.now().toEpochDay());
    } else {
      this.setDayclaimed(LocalDate.now().minusDays(1).toEpochDay());
    }
    this.setDay((short) 0);
    DatabaseClientFactory.databaseClient.updateUserInfo(this);
  }

  public void claim() {
    this.setDayclaimed(LocalDate.now().toEpochDay());
    this.setDay((short) (getDay() + 1));
  }
}