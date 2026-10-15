package com.kingpixel.ultracalendar.command.base;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles /calendar claim <player> and /calendar claim <player> <day>.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarClaimCommand {

  private static final String ARG_PLAYER = "player";
  private static final String ARG_DAY = "day";

  private CalendarClaimCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("claim")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.claim", 4))
        .then(
          CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
            .executes(context -> {
              if (!UltraCalendar.config.isActive()) return 0;
              ServerPlayerEntity player = EntityArgumentType.getPlayer(context, ARG_PLAYER);
              claimNext(context.getSource(), player);
              return 1;
            })
            .then(
              CommandManager.argument(ARG_DAY, IntegerArgumentType.integer(1, UltraCalendar.config.maxDay()))
                .executes(context -> {
                  if (!UltraCalendar.config.isActive()) return 0;
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(context, ARG_PLAYER);
                  int day = IntegerArgumentType.getInteger(context, ARG_DAY);
                  claimDay(context.getSource(), player, day);
                  return 1;
                })
            )
        )
    );
  }

  private static void claimNext(ServerCommandSource source, ServerPlayerEntity player) {
    if (player == null) {
      CommandTree.sendMessage(source, UltraCalendar.language.getMessageAdminPlayerNotFound());
      return;
    }

    UltraCalendar.database.getUser(player).thenAccept(user -> {
      if (user == null) return;
      int nextDay = user.getDay() + 1;
      executeClaim(source, player, user, nextDay);
    });
  }

  private static void claimDay(ServerCommandSource source, ServerPlayerEntity player, int day) {
    if (player == null) {
      CommandTree.sendMessage(source, UltraCalendar.language.getMessageAdminPlayerNotFound());
      return;
    }

    UltraCalendar.database.getUser(player).thenAccept(user -> {
      if (user == null) return;
      executeClaim(source, player, user, day);
    });
  }

  private static void executeClaim(ServerCommandSource source, ServerPlayerEntity player, User user, int day) {
    Rewards reward = UltraCalendar.config.getReward(day);
    if (reward == null) {
      String notFoundMsg = UltraCalendar.language.getMessageAdminRewardNotFound()
        .replace("%day%", String.valueOf(day));
      CommandTree.sendMessage(source, notFoundMsg);
      return;
    }

    long today = User.getTodayEpoch();
    user.setDay(day);
    user.setDayClaimed(today);
    user.setLastJoin(today);
    user.markDirty();
    user.save();

    if (CobbleUtils.server != null && !CobbleUtils.server.isStopping()) {
      CobbleUtils.server.execute(() -> {
        if (player.isDisconnected()) return;
        if (reward.getRewards() != null) {
          reward.getRewards().giveRewards(player);
        }
      });
    }

    String msg = UltraCalendar.language.getMessageAdminClaimSuccess()
      .replace("%player%", player.getGameProfile().getName())
      .replace("%day%", String.valueOf(day));
    CommandTree.sendMessage(source, msg);
  }
}
