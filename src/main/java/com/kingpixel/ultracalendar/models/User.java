package com.kingpixel.ultracalendar.models;

import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.ultracalendar.UltraCalendar;
import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class User implements Serializable {

  private UUID uuid;
  private String name;

  private int day;
  private long dayClaimed;
  private long lastJoin;
  private long dailyPlayTime;
  private long totalPlayTime;
  private String lastMonth;
  private List<Integer> claimedDays;

  private transient volatile AtomicBoolean dirty;

  public User(@NotNull ServerPlayerEntity player) {
    this.uuid = player.getUuid();
    this.name = player.getGameProfile().getName();

    long today = today();
    this.day = 0;
    this.dayClaimed = today - 1;
    this.lastJoin = today;
    this.dailyPlayTime = 0;
    this.totalPlayTime = 0;
    this.lastMonth = getCurrentYearMonth();
    this.claimedDays = new ArrayList<>();
    markDirty();
  }

  public @NotNull List<Integer> getClaimedDays() {
    if (claimedDays == null) {
      claimedDays = new ArrayList<>();
    }
    return claimedDays;
  }

  public void setDay(int day) {
    this.day = day;
    markDirty();
  }

  public void setDayClaimed(long dayClaimed) {
    this.dayClaimed = dayClaimed;
    markDirty();
  }

  public void setLastJoin(long lastJoin) {
    this.lastJoin = lastJoin;
    markDirty();
  }

  public void setDailyPlayTime(long dailyPlayTime) {
    this.dailyPlayTime = dailyPlayTime;
    markDirty();
  }

  public void setTotalPlayTime(long totalPlayTime) {
    this.totalPlayTime = totalPlayTime;
    markDirty();
  }

  public void setLastMonth(String lastMonth) {
    this.lastMonth = lastMonth;
    markDirty();
  }

  private static volatile long cachedTodayEpoch = 0;
  private static volatile long lastEpochCheckMillis = 0;

  public static long getTodayEpoch() {
    long now = System.currentTimeMillis();
    if (now - lastEpochCheckMillis < 2000 && cachedTodayEpoch != 0) {
      return cachedTodayEpoch;
    }
    try {
      ZoneId zone = (UltraCalendar.config != null) ? UltraCalendar.config.getParsedZoneId() : ZoneId.systemDefault();
      cachedTodayEpoch = LocalDate.now(zone).toEpochDay();
      lastEpochCheckMillis = now;
      return cachedTodayEpoch;
    } catch (Exception e) {
      return LocalDate.now(ZoneId.systemDefault()).toEpochDay();
    }
  }

  public static int getTodayDayOfMonth() {
    ZoneId zone = (UltraCalendar.config != null) ? UltraCalendar.config.getParsedZoneId() : ZoneId.systemDefault();
    try {
      return LocalDate.now(zone).getDayOfMonth();
    } catch (Exception e) {
      return LocalDate.now(ZoneId.systemDefault()).getDayOfMonth();
    }
  }

  public static String getCurrentYearMonth() {
    ZoneId zone = (UltraCalendar.config != null) ? UltraCalendar.config.getParsedZoneId() : ZoneId.systemDefault();
    try {
      LocalDate now = LocalDate.now(zone);
      return now.getYear() + "-" + String.format("%02d", now.getMonthValue());
    } catch (Exception e) {
      LocalDate now = LocalDate.now(ZoneId.systemDefault());
      return now.getYear() + "-" + String.format("%02d", now.getMonthValue());
    }
  }

  private long today() {
    return getTodayEpoch();
  }

  public @NotNull AtomicBoolean getDirty() {
    AtomicBoolean d = dirty;
    if (d == null) {
      synchronized (this) {
        d = dirty;
        if (d == null) {
          dirty = d = new AtomicBoolean(false);
        }
      }
    }
    return d;
  }

  public boolean isDirty() {
    return dirty != null && dirty.get();
  }

  public void markDirty() {
    getDirty().set(true);
  }

  public void clearDirty() {
    if (dirty != null) dirty.set(false);
  }

  public void addPlayTime(long seconds) {
    long today = today();
    if (lastJoin != today) {
      lastJoin = today;
      dailyPlayTime = 0;
    }
    dailyPlayTime += seconds;
    totalPlayTime += seconds;
    markDirty();
  }

  public int getTargetDay() {
    if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.MONTH_DAY) {
      return getTodayDayOfMonth();
    }
    return this.day + 1;
  }

  public boolean isClaimed(int day) {
    if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.MONTH_DAY) {
      return getClaimedDays().contains(day);
    }
    return this.day >= day;
  }

  public boolean isAvailableDay(int day) {
    if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.MONTH_DAY) {
      int todayDay = getTodayDayOfMonth();
      if (day > todayDay) return false;
      if (day == todayDay) return true;
      int margin = UltraCalendar.config.getResetMarginDays();
      return (todayDay - day) <= margin;
    }
    return canClaim() && (this.day + 1 == day);
  }

  public boolean canClaim() {
    if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.MONTH_DAY) {
      return !isClaimed(getTodayDayOfMonth());
    }
    return today() > dayClaimed;
  }

  public boolean canClaim(@Nullable Rewards reward) {
    if (reward == null) return false;
    int rewardDay = reward.getDay();
    if (isClaimed(rewardDay)) return false;
    if (!isAvailableDay(rewardDay)) return false;

    DurationValue duration = reward.getRequiredTime();
    long requiredSeconds = duration != null ? duration.toSeconds() : 0;
    long currentPlayTime = reward.isDailyTime() ? dailyPlayTime : totalPlayTime;
    return currentPlayTime >= requiredSeconds;
  }

  public boolean canClaimNextReward() {
    if (UltraCalendar.config == null) return false;
    if (UltraCalendar.config.getMode() == CalendarMode.MONTH_DAY) {
      return canClaim(UltraCalendar.config.getReward(getTodayDayOfMonth()));
    }
    if (day >= UltraCalendar.config.maxDay()) return false;
    return canClaim(UltraCalendar.config.getReward(day + 1));
  }

  public boolean claim() {
    if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.MONTH_DAY) {
      return claim(UltraCalendar.config.getReward(getTodayDayOfMonth()));
    }
    int nextDay = this.day + 1;
    Rewards reward = (UltraCalendar.config != null) ? UltraCalendar.config.getReward(nextDay) : null;
    return claim(reward);
  }

  public boolean claim(@Nullable Rewards reward) {
    if (reward == null || !canClaim(reward)) return false;
    long today = today();

    if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.MONTH_DAY) {
      int rewardDay = reward.getDay();
      if (!getClaimedDays().contains(rewardDay)) {
        getClaimedDays().add(rewardDay);
      }
      this.day = getClaimedDays().size();
    } else {
      this.day++;
    }

    this.dayClaimed = today;
    this.lastJoin = today;
    this.lastMonth = getCurrentYearMonth();

    markDirty();
    return true;
  }

  public boolean hasMissedDays() {
    int margin = (UltraCalendar.config != null) ? UltraCalendar.config.getResetMarginDays() : 3;
    return today() - dayClaimed > margin;
  }

  private boolean checkMonthReset(@NotNull ServerPlayerEntity player) {
    if (UltraCalendar.config == null || UltraCalendar.config.getMode() != CalendarMode.MONTH_DAY) {
      return false;
    }
    String currentMonth = getCurrentYearMonth();
    if (lastMonth != null && !lastMonth.equals(currentMonth)) {
      if (UltraCalendar.config.isAutoReset()) {
        reset(player, true);
        return true;
      }
    }
    this.lastMonth = currentMonth;
    return false;
  }

  public void onJoin(@NotNull ServerPlayerEntity player) {
    fix();

    if (checkMonthReset(player)) {
      return;
    }

    if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.STREAK) {
      if (hasMissedDays() && UltraCalendar.config.isAutoReset()) {
        reset(player, false);
        return;
      }

      if (day >= UltraCalendar.config.maxDay()) {
        reset(player, true);
        return;
      }
    }

    long today = today();
    if (lastJoin != today) {
      lastJoin = today;
      dailyPlayTime = 0;
      markDirty();
    }
  }

  public void reset(@NotNull ServerPlayerEntity player, boolean completed) {
    long today = today();

    String message = completed
      ? UltraCalendar.language.getMessageCompleted()
      : UltraCalendar.language.getMessageReset();

    if (UltraCalendar.config.isSendMessageReset()) {
      message = message.replace("%day%", String.valueOf(day));

      PlayerUtils.sendMessage(
        player,
        message,
        UltraCalendar.language.getPrefix(),
        TypeMessage.CHAT
      );
    }

    this.day = 0;
    getClaimedDays().clear();
    this.lastMonth = getCurrentYearMonth();
    this.dayClaimed = completed ? today : today - 1;
    this.lastJoin = today;
    this.dailyPlayTime = 0;

    markDirty();
  }

  public void fix() {
    long today = today();
    boolean changed = false;

    if (day < 0) {
      day = 0;
      changed = true;
    }

    if (dailyPlayTime < 0) {
      dailyPlayTime = 0;
      changed = true;
    }

    if (totalPlayTime < 0) {
      totalPlayTime = 0;
      changed = true;
    }

    if (totalPlayTime < dailyPlayTime) {
      totalPlayTime = dailyPlayTime;
      changed = true;
    }

    if (dayClaimed > today) {
      dayClaimed = today;
      changed = true;
    }

    if (lastJoin > today) {
      lastJoin = today;
      changed = true;
    }

    if (lastMonth == null || lastMonth.isBlank()) {
      lastMonth = getCurrentYearMonth();
      changed = true;
    }

    if (claimedDays == null) {
      claimedDays = new ArrayList<>();
      changed = true;
    }

    if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.STREAK) {
      if (hasMissedDays() && UltraCalendar.config.isAutoReset()) {
        day = 0;
        dayClaimed = today - 1;
        dailyPlayTime = 0;
        changed = true;
      }

      if (day > UltraCalendar.config.maxDay()) {
        day = UltraCalendar.config.maxDay();
        changed = true;
      }
    }

    if (changed) {
      markDirty();
    }
  }

  public @NotNull CompletableFuture<Void> save() {
    if (getDirty().compareAndSet(true, false)) {
      return UltraCalendar.database.saveOrUpdateUser(this)
        .exceptionally(throwable -> {
          markDirty();
          return null;
        });
    }
    return CompletableFuture.completedFuture(null);
  }
}