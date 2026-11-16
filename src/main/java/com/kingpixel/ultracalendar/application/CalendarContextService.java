package com.kingpixel.ultracalendar.application;

import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.api.model.CalendarRewardEntryView;
import com.kingpixel.ultracalendar.api.model.PlayerCalendarContext;
import com.kingpixel.ultracalendar.models.CalendarMode;
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Application service providing query and projection views for UltraCalendar.
 */
public class CalendarContextService {

  public CompletableFuture<PlayerCalendarContext> getPlayerContext(ServerPlayerEntity player) {
    if (player == null) {
      return CompletableFuture.completedFuture(null);
    }

    if (UltraCalendar.database == null) {
      return CompletableFuture.completedFuture(null);
    }

    return UltraCalendar.database.getUser(player).thenApplyAsync(user -> {
      if (user == null) return null;

      List<Rewards> rewards = UltraCalendar.config != null ? UltraCalendar.config.getRewards() : List.of();
      List<CalendarRewardEntryView> dayViews = new ArrayList<>();
      int totalClaimable = 0;

      for (Rewards reward : rewards) {
        if (reward == null) continue;
        int day = reward.getDay();
        boolean claimed = user.isClaimed(day);
        boolean available = user.canClaim(reward);

        long requiredSeconds = reward.getRequiredTime() != null ? reward.getRequiredTime().toSeconds() : 0;
        long currentPlayTime = reward.isDailyTime() ? user.getDailyPlayTime() : user.getTotalPlayTime();
        boolean hasPlayTime = currentPlayTime >= requiredSeconds;
        boolean canClaim = !claimed && available && hasPlayTime;

        if (canClaim) {
          totalClaimable++;
        }

        long remainingPlayTime = Math.max(0, requiredSeconds - currentPlayTime);
        int rewardsCount = (reward.getRewards() != null && reward.getRewards().getLootTable() != null)
          ? reward.getRewards().getLootTable().values().stream().mapToInt(Collection::size).sum()
          : 0;

        dayViews.add(new CalendarRewardEntryView(
          day,
          claimed,
          available,
          hasPlayTime,
          canClaim,
          requiredSeconds,
          remainingPlayTime,
          rewardsCount
        ));
      }

      CalendarMode mode = UltraCalendar.config != null ? UltraCalendar.config.getMode() : CalendarMode.STREAK;
      int targetDay = user.getTargetDay();
      boolean canClaimToday = user.canClaimNextReward();

      return new PlayerCalendarContext(
        player.getUuid(),
        player.getGameProfile().getName(),
        user.getDay(),
        user.getDayClaimed(),
        user.getDailyPlayTime(),
        user.getTotalPlayTime(),
        user.getClaimedDays(),
        mode,
        targetDay,
        canClaimToday,
        dayViews,
        totalClaimable
      );
    }, UltraCalendar.ASYNC::runAsync);
  }

  public CompletableFuture<CalendarRewardEntryView> getRewardEntry(ServerPlayerEntity player, int day) {
    return getPlayerContext(player).thenApply(ctx -> ctx != null ? ctx.getDay(day).orElse(null) : null);
  }

  public List<Rewards> getAllRewards() {
    return UltraCalendar.config != null ? UltraCalendar.config.getRewards() : Collections.emptyList();
  }
}
