package com.kingpixel.cobblecalendar.models;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendar.managers.DailyRewardsManager;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
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
 * Represents user information for the CobbleCalendar.
 * Author: Carlos Varas Alonso - 15/08/2024 17:03
 */
@Getter
@Setter
@ToString
@Data
public class UserInfo implements Serializable {
  private UUID uuid;
  private String name;
  private int day;
  private long dayClaimed;
  private long lastJoin;

  public UserInfo() {
  }

  /**
   * Constructs a new UserInfo object for the given player.
   *
   * @param player the player entity
   */
  public UserInfo(ServerPlayerEntity player) {
    this.uuid = player.getUuid();
    this.name = player.getGameProfile().getName();
    this.day = 0;
    this.dayClaimed = LocalDate.now().minusDays(1).toEpochDay();
    this.lastJoin = LocalDate.now().toEpochDay();
  }

  /**
   * Checks if the user can claim today's reward.
   *
   * @return true if the user can claim, false otherwise
   */
  public boolean canClaim() {
    return LocalDate.now().isAfter(LocalDate.ofEpochDay(dayClaimed));
  }

  /**
   * Checks if the user can claim the reward for the specified day.
   *
   * @param day the day to check
   *
   * @return true if the user can claim, false otherwise
   */
  public boolean canClaim(int day) {
    return canClaim() && day == this.day + 1;
  }

  /**
   * Writes the user information to a JSON file asynchronously.
   *
   * @param player the player entity
   */
  public void writeInfo(ServerPlayerEntity player) {
    File folder = Utils.getAbsolutePath(DailyRewardsManager.PATH_USER_INFO);
    if (!folder.exists() && !folder.mkdirs()) {
      CobbleCalendar.LOGGER.warn("Failed to create user info directory: " + folder.getPath());
      return;
    }

    String json = Utils.newWithoutSpacingGson().toJson(this);
    Utils.writeFileAsync(DailyRewardsManager.PATH_USER_INFO, player.getUuidAsString() + ".json", json);
  }

  /**
   * Computes the day difference and updates the last join date.
   */
  public void computeDay(ServerPlayerEntity player) {
    long daysBetween = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(dayClaimed), LocalDate.ofEpochDay(lastJoin));

    if (CobbleCalendar.config.isDebug()) {
      CobbleCalendar.LOGGER.info("Days between: " + daysBetween);
    }

    boolean shouldReset = day >= CobbleCalendar.config.maxDay() ||
      (CobbleCalendar.config.isAutoReset() && daysBetween > CobbleCalendar.config.getResetMarginDays() + 1);

    if (CobbleCalendar.config.isDebug()) {
      CobbleCalendar.LOGGER.info("Should reset: " + shouldReset);
    }

    if (shouldReset) reset(player, day >= CobbleCalendar.config.maxDay());

    this.lastJoin = LocalDate.now().toEpochDay();
  }

  /**
   * Resets the user's claim status.
   *
   * @param completeAll whether to complete all days
   */
  public void reset(ServerPlayerEntity player, boolean completeAll) {
    String message;
    if (completeAll) {
      message = CobbleCalendar.language.getMessageCompleted();
      this.dayClaimed = LocalDate.now().toEpochDay();
    } else {
      message = CobbleCalendar.language.getMessageReset();
      this.dayClaimed = LocalDate.now().minusDays(1).toEpochDay();
    }
    if (CobbleCalendar.config.isSendMessageReset()) {
      message = message
        .replace("%day%", String.valueOf(this.day));
      PlayerUtils.sendMessage(
        player,
        message,
        CobbleCalendar.language.getPrefix(),
        TypeMessage.CHAT
      );
    }

    this.day = 0;
    DatabaseClientFactory.databaseClient.updateUserInfo(player, this);
  }

  /**
   * Claims the reward for the current day.
   */
  public void claim() {
    long now = LocalDate.now().toEpochDay();
    this.dayClaimed = now;
    this.lastJoin = now;
    this.day++;
  }
}
