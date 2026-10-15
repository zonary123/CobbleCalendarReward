package com.kingpixel.ultracalendar.command.base;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Handles /calendar help.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarHelpCommand {

  private CalendarHelpCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("help")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.help", 0))
        .executes(context -> {
          CommandTree.sendMessage(context.getSource(), UltraCalendar.language.getMessageAdminHelp());
          return 1;
        })
    );
  }
}
