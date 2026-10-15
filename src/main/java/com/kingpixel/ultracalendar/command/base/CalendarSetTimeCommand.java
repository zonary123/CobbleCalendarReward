package com.kingpixel.ultracalendar.command.base;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.kingpixel.ultracalendar.models.User;
import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles /calendar settime and /calendar addtime.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarSetTimeCommand {

  private static final String ARG_PLAYER = "player";
  private static final String ARG_SECONDS = "seconds";

  private CalendarSetTimeCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("settime")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.settime", 4))
        .then(
          CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
            .then(
              CommandManager.argument(ARG_SECONDS, IntegerArgumentType.integer(0))
                .executes(context -> {
                  if (!UltraCalendar.config.isActive()) return 0;
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(context, ARG_PLAYER);
                  int seconds = IntegerArgumentType.getInteger(context, ARG_SECONDS);
                  setTime(context.getSource(), player, seconds);
                  return 1;
                })
            )
        )
    );

    base.then(
      CommandManager.literal("addtime")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.settime", 4))
        .then(
          CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
            .then(
              CommandManager.argument(ARG_SECONDS, IntegerArgumentType.integer(1))
                .executes(context -> {
                  if (!UltraCalendar.config.isActive()) return 0;
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(context, ARG_PLAYER);
                  int seconds = IntegerArgumentType.getInteger(context, ARG_SECONDS);
                  addTime(context.getSource(), player, seconds);
                  return 1;
                })
            )
        )
    );
  }

  private static void setTime(ServerCommandSource source, ServerPlayerEntity player, int seconds) {
    if (player == null) {
      CommandTree.sendMessage(source, UltraCalendar.language.getMessageAdminPlayerNotFound());
      return;
    }

    UltraCalendar.database.getUser(player).thenAccept(user -> {
      if (user == null) return;

      long today = User.getTodayEpoch();
      if (user.getLastJoin() != today) {
        user.setLastJoin(today);
      }
      user.setDailyPlayTime(seconds);
      if (user.getTotalPlayTime() < seconds) {
        user.setTotalPlayTime(seconds);
      }
      user.markDirty();
      user.save();

      String msg = UltraCalendar.language.getMessageAdminSetTimeSuccess()
        .replace("%player%", player.getGameProfile().getName())
        .replace("%time%", DailyRewardUI.formatDuration(seconds));
      CommandTree.sendMessage(source, msg);

      UltraCalendar.sendAlert(player, user);
    });
  }

  private static void addTime(ServerCommandSource source, ServerPlayerEntity player, int seconds) {
    if (player == null) {
      CommandTree.sendMessage(source, UltraCalendar.language.getMessageAdminPlayerNotFound());
      return;
    }

    UltraCalendar.database.getUser(player).thenAccept(user -> {
      if (user == null) return;

      user.addPlayTime(seconds);
      user.save();

      String msg = UltraCalendar.language.getMessageAdminAddTimeSuccess()
        .replace("%player%", player.getGameProfile().getName())
        .replace("%added%", DailyRewardUI.formatDuration(seconds))
        .replace("%time%", DailyRewardUI.formatDuration(user.getDailyPlayTime()));
      CommandTree.sendMessage(source, msg);

      UltraCalendar.sendAlert(player, user);
    });
  }
}
