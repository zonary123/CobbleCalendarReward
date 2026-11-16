package com.kingpixel.ultracalendar.application;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.api.action.CalendarActionResult;
import com.kingpixel.ultracalendar.models.CalendarMode;
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Application service providing headless actions for UltraCalendar:
 * claiming rewards, resetting player progress, and administrative modifications.
 */
public class CalendarActionService {

  public CompletableFuture<CalendarActionResult> claimReward(ServerPlayerEntity player) {
    if (player == null) {
      return CompletableFuture.completedFuture(
        CalendarActionResult.failure(CalendarActionResult.Status.PLAYER_OFFLINE, "Player is offline or null")
      );
    }

    if (UltraCalendar.database == null) {
      return CompletableFuture.completedFuture(
        CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "Database client is not available")
      );
    }

    return UltraCalendar.database.getUser(player).thenComposeAsync(user -> {
      if (user == null) {
        return CompletableFuture.completedFuture(
          CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "User data not found for player")
        );
      }
      int targetDay = user.getTargetDay();
      return claimReward(player, targetDay);
    }, UltraCalendar.ASYNC::runAsync);
  }

  public CompletableFuture<CalendarActionResult> claimReward(ServerPlayerEntity player, int day) {
    CompletableFuture<CalendarActionResult> future = new CompletableFuture<>();

    if (player == null) {
      future.complete(CalendarActionResult.failure(CalendarActionResult.Status.PLAYER_OFFLINE, "Player is offline or null"));
      return future;
    }

    if (!UltraCalendar.config.isActive()) {
      future.complete(CalendarActionResult.failure(CalendarActionResult.Status.CALENDAR_DISABLED, "UltraCalendar is currently disabled"));
      return future;
    }

    if (UltraCalendar.database == null) {
      future.complete(CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "Database client is not available"));
      return future;
    }

    UltraCalendar.database.getUser(player).thenAcceptAsync(user -> {
      if (user == null) {
        future.complete(CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "User data not found for player"));
        return;
      }

      Rewards reward = UltraCalendar.config.getReward(day);
      if (reward == null) {
        future.complete(CalendarActionResult.failure(CalendarActionResult.Status.NO_REWARD_FOUND, "Reward for day " + day + " not found", day));
        return;
      }

      boolean isClaimed = user.isClaimed(day);
      if (isClaimed) {
        future.complete(CalendarActionResult.alreadyClaimed("Reward for day " + day + " has already been claimed", day));
        return;
      }

      boolean isAvailable = user.canClaim(reward);
      if (!isAvailable) {
        future.complete(CalendarActionResult.notAvailableYet("Reward for day " + day + " is not available yet", day));
        return;
      }

      long requiredSeconds = reward.getRequiredTime() != null ? reward.getRequiredTime().toSeconds() : 0;
      long currentPlayTime = reward.isDailyTime() ? user.getDailyPlayTime() : user.getTotalPlayTime();
      if (currentPlayTime < requiredSeconds) {
        future.complete(CalendarActionResult.needPlayTime(
          "Required playtime not met for day " + day,
          day,
          requiredSeconds,
          currentPlayTime
        ));
        return;
      }

      boolean claimed = user.claim(reward);
      if (!claimed) {
        future.complete(CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "Failed to claim reward for day " + day, day));
        return;
      }

      user.markDirty();
      user.save();

      if (CobbleUtils.server != null && !CobbleUtils.server.isStopping()) {
        CobbleUtils.server.execute(() -> {
          if (!player.isDisconnected() && reward.getRewards() != null) {
            reward.getRewards().giveRewards(player);
          }
        });
      }

      future.complete(CalendarActionResult.success("Successfully claimed reward for day " + day, day));
    }, UltraCalendar.ASYNC::runAsync).exceptionally(ex -> {
      future.complete(CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "Error claiming reward: " + ex.getMessage(), day));
      return null;
    });

    return future;
  }

  public CompletableFuture<List<CalendarActionResult>> claimAllAvailable(ServerPlayerEntity player) {
    if (player == null) {
      return CompletableFuture.completedFuture(List.of(
        CalendarActionResult.failure(CalendarActionResult.Status.PLAYER_OFFLINE, "Player is offline or null")
      ));
    }

    return UltraCalendar.database.getUser(player).thenComposeAsync(user -> {
      if (user == null) {
        return CompletableFuture.completedFuture(List.of(
          CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "User data not found for player")
        ));
      }

      List<Rewards> rewards = UltraCalendar.config.getRewards();
      List<CompletableFuture<CalendarActionResult>> futures = new ArrayList<>();

      for (Rewards reward : rewards) {
        if (reward == null) continue;
        int day = reward.getDay();
        if (!user.isClaimed(day) && user.canClaim(reward)) {
          long requiredSeconds = reward.getRequiredTime() != null ? reward.getRequiredTime().toSeconds() : 0;
          long currentPlayTime = reward.isDailyTime() ? user.getDailyPlayTime() : user.getTotalPlayTime();
          if (currentPlayTime >= requiredSeconds) {
            futures.add(claimReward(player, day));
          }
        }
      }

      if (futures.isEmpty()) {
        return CompletableFuture.completedFuture(List.of());
      }

      return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }, UltraCalendar.ASYNC::runAsync);
  }

  public CompletableFuture<CalendarActionResult> resetUser(ServerPlayerEntity player, boolean completed) {
    if (player == null) {
      return CompletableFuture.completedFuture(
        CalendarActionResult.failure(CalendarActionResult.Status.PLAYER_OFFLINE, "Player is offline or null")
      );
    }

    return UltraCalendar.database.getUser(player).thenApplyAsync(user -> {
      if (user == null) {
        return CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "User data not found for player");
      }
      user.reset(player, completed);
      user.markDirty();
      user.save();
      return CalendarActionResult.success("User calendar reset successfully", 0);
    }, UltraCalendar.ASYNC::runAsync);
  }

  public CompletableFuture<CalendarActionResult> setDay(ServerPlayerEntity player, int day) {
    if (player == null) {
      return CompletableFuture.completedFuture(
        CalendarActionResult.failure(CalendarActionResult.Status.PLAYER_OFFLINE, "Player is offline or null")
      );
    }

    return UltraCalendar.database.getUser(player).thenApplyAsync(user -> {
      if (user == null) {
        return CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "User data not found for player");
      }
      user.setDay(day);
      if (UltraCalendar.config != null && UltraCalendar.config.getMode() == CalendarMode.MONTH_DAY) {
        if (!user.getClaimedDays().contains(day)) {
          user.getClaimedDays().add(day);
        }
      }
      user.markDirty();
      user.save();
      return CalendarActionResult.success("Player calendar day set to " + day, day);
    }, UltraCalendar.ASYNC::runAsync);
  }
}
