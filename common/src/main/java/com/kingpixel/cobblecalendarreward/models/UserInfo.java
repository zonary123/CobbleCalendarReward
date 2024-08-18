package com.kingpixel.cobblecalendarreward.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
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
    return today.isAfter(dayclaimed) && today.minusDays(1).isEqual(dayclaimed);
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

    if (daysBetween >= 2 || this.getDay() >= CobbleCalendarReward.config.maxDay() + 1) {
      this.setDay((short) 0);
    }

    this.setLastJoin(LocalDate.now());
  }

  public void reset() {
    this.setDayclaimed(LocalDate.now().minusDays(1));
    this.setDay((short) 0);
  }

  public void claim() {
    this.setDayclaimed(LocalDate.now());
    this.setDay((short) (getDay() + 1));
  }
}
