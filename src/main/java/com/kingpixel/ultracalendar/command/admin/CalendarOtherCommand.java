package com.kingpixel.ultracalendar.command.admin;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.api.UltraCalendarApi;
import com.kingpixel.ultracalendar.command.CommandTree;
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
public class CalendarOtherCommand {

  private static final String ARG_PLAYER = "player";

  private CalendarOtherCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("other")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.other", 2))
        .then(
          CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
            .executes(context -> {
              if (!UltraCalendar.config.isActive()) return 0;
              ServerPlayerEntity player = EntityArgumentType.getPlayer(context, ARG_PLAYER);
              if (player == null) {
                CommandTree.sendMessage(context.getSource(), UltraCalendar.language.getMessageAdminPlayerNotFound());
                return 0;
              }
              UltraCalendarApi.openCalendarMenu(player);
              return 1;
            })
        )
    );
  }
}
