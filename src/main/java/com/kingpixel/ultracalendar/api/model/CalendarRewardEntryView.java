package com.kingpixel.ultracalendar.api.model;

import lombok.Getter;

/**
 * Viewmodel representing a single day entry in the calendar rewards system.
 */
@Getter
public class CalendarRewardEntryView {

  private final int day;
  private final boolean claimed;
  private final boolean available;
  private final boolean hasPlayTime;
  private final boolean canClaim;
  private final long requiredPlayTimeSeconds;
  private final long remainingPlayTimeSeconds;
  private final int rewardsCount;

  public CalendarRewardEntryView(int day,
                                 boolean claimed,
                                 boolean available,
                                 boolean hasPlayTime,
                                 boolean canClaim,
                                 long requiredPlayTimeSeconds,
                                 long remainingPlayTimeSeconds,
                                 int rewardsCount) {
    this.day = day;
    this.claimed = claimed;
    this.available = available;
    this.hasPlayTime = hasPlayTime;
    this.canClaim = canClaim;
    this.requiredPlayTimeSeconds = requiredPlayTimeSeconds;
    this.remainingPlayTimeSeconds = Math.max(0, remainingPlayTimeSeconds);
    this.rewardsCount = rewardsCount;
  }
}
