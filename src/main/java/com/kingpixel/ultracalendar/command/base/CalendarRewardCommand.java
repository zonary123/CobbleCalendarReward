package com.kingpixel.ultracalendar.command.base;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles /calendar other <player>.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarRewardCommand {

  private CalendarRewardCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("other")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.other", 4))
        .then(
          CommandManager.argument("player", EntityArgumentType.player())
            .executes(context -> {
              if (!UltraCalendar.config.isActive()) return 0;
              ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
              if (player == null) {
                CommandTree.sendMessage(context.getSource(), UltraCalendar.language.getMessageAdminPlayerNotFound());
                return 0;
              }
              DailyRewardUI.open(player);
              return 1;
            })
        )
    );
  }
}
