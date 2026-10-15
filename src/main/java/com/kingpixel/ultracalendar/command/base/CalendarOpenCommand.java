package com.kingpixel.ultracalendar.command.base;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles /calendar open and /calendar gui.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarOpenCommand {

  private CalendarOpenCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("open")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.user", 0))
        .executes(context -> execute(context.getSource()))
    );

    base.then(
      CommandManager.literal("gui")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.user", 0))
        .executes(context -> execute(context.getSource()))
    );
  }

  public static int execute(ServerCommandSource source) {
    if (!source.isExecutedByPlayer()) {
      UltraCalendar.LOGGER.info("This command can only be executed by a player.");
      return 0;
    }
    if (!UltraCalendar.config.isActive()) return 0;
    ServerPlayerEntity player = source.getPlayer();
    if (player == null) return 0;
    DailyRewardUI.open(player);
    return 1;
  }
}
