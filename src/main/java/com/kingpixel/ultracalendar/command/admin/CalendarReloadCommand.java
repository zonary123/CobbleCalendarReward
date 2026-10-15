package com.kingpixel.ultracalendar.command.admin;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Handles /calendar reload.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarReloadCommand {

  private CalendarReloadCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("reload")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.reload", 2))
        .executes(context -> {
          UltraCalendar.load();
          CommandTree.sendMessage(context.getSource(), UltraCalendar.language.getMessageReload());
          return 1;
        })
    );
  }
}
