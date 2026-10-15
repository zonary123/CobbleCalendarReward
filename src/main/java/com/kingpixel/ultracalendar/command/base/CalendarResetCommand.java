package com.kingpixel.ultracalendar.command.base;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.kingpixel.ultracalendar.models.User;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Handles /calendar reset, /calendar reset <player>, and /calendar resetall.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarResetCommand {

  private CalendarResetCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("reset")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.reset", 4))
        .executes(context -> {
          if (!UltraCalendar.config.isActive()) return 0;
          if (!context.getSource().isExecutedByPlayer()) {
            CommandTree.sendMessage(context.getSource(), UltraCalendar.language.getMessageAdminConsoleMustSpecifyPlayer());
            return 0;
          }
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          resetPlayer(context.getSource(), player, false);
          return 1;
        })
        .then(
          CommandManager.argument("player", EntityArgumentType.player())
            .executes(context -> {
              if (!UltraCalendar.config.isActive()) return 0;
              ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
              resetPlayer(context.getSource(), target, true);
              return 1;
            })
        )
    );

    base.then(
      CommandManager.literal("resetall")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.resetall", 4))
        .executes(context -> {
          if (!UltraCalendar.config.isActive()) return 0;
          resetAllPlayers(context.getSource());
          return 1;
        })
    );
  }

  private static void resetPlayer(ServerCommandSource source, ServerPlayerEntity target, boolean notifyAdmin) {
    if (target == null) {
      CommandTree.sendMessage(source, UltraCalendar.language.getMessageAdminPlayerNotFound());
      return;
    }

    UltraCalendar.database.getUser(target).thenAccept(user -> {
      if (user == null) return;
      user.reset(target, true);
      user.markDirty();
      user.save();

      if (notifyAdmin) {
        String msg = UltraCalendar.language.getMessageAdminResetSuccess()
          .replace("%player%", target.getGameProfile().getName());
        CommandTree.sendMessage(source, msg);
      }
    });
  }

  private static void resetAllPlayers(ServerCommandSource source) {
    if (CobbleUtils.server == null) return;
    List<ServerPlayerEntity> players = CobbleUtils.server.getPlayerManager().getPlayerList();
    int count = 0;

    for (ServerPlayerEntity player : players) {
      if (player == null || player.isDisconnected()) continue;
      User user = UltraCalendar.database.getUserDirect(player);
      if (user != null) {
        user.reset(player, true);
        user.markDirty();
        user.save();
      } else {
        UltraCalendar.database.getUser(player).thenAccept(loaded -> {
          if (loaded != null) {
            loaded.reset(player, true);
            loaded.markDirty();
            loaded.save();
          }
        });
      }
      count++;
    }

    String msg = UltraCalendar.language.getMessageAdminResetAllSuccess()
      .replace("%count%", String.valueOf(count));
    CommandTree.sendMessage(source, msg);
  }
}
