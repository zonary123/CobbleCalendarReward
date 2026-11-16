package com.kingpixel.ultracalendar.api.model;

import com.kingpixel.ultracalendar.models.CalendarMode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Snapshot context viewmodel for a player's calendar progression and rewards.
 */
@Getter
public class PlayerCalendarContext {

  private final UUID playerUuid;
  private final String playerName;
  private final int currentStreakDay;
  private final long dayClaimed;
  private final long dailyPlayTime;
  private final long totalPlayTime;
  private final List<Integer> claimedDays;
  private final CalendarMode mode;
  private final int targetDay;
  private final boolean canClaimToday;
  private final List<CalendarRewardEntryView> days;
  private final int totalRewardsClaimable;

  public PlayerCalendarContext(UUID playerUuid,
                               String playerName,
                               int currentStreakDay,
                               long dayClaimed,
                               long dailyPlayTime,
                               long totalPlayTime,
                               List<Integer> claimedDays,
                               CalendarMode mode,
                               int targetDay,
                               boolean canClaimToday,
                               List<CalendarRewardEntryView> days,
                               int totalRewardsClaimable) {
    this.playerUuid = playerUuid;
    this.playerName = playerName;
    this.currentStreakDay = currentStreakDay;
    this.dayClaimed = dayClaimed;
    this.dailyPlayTime = dailyPlayTime;
    this.totalPlayTime = totalPlayTime;
    this.claimedDays = claimedDays != null ? Collections.unmodifiableList(claimedDays) : Collections.emptyList();
    this.mode = mode;
    this.targetDay = targetDay;
    this.canClaimToday = canClaimToday;
    this.days = days != null ? Collections.unmodifiableList(days) : Collections.emptyList();
    this.totalRewardsClaimable = totalRewardsClaimable;
  }

  public Optional<CalendarRewardEntryView> getDay(int day) {
    return days.stream().filter(d -> d.getDay() == day).findFirst();
  }
}
