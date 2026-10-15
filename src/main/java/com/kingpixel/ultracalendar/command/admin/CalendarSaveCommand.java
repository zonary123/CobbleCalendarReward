package com.kingpixel.ultracalendar.command.admin;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Handles /calendar save and /calendar saveall.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarSaveCommand {

  private CalendarSaveCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("save")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.save", 2))
        .executes(context -> {
          executeSave(context.getSource());
          return 1;
        })
    );

    base.then(
      CommandManager.literal("saveall")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.save", 2))
        .executes(context -> {
          executeSave(context.getSource());
          return 1;
        })
    );
  }

  private static void executeSave(ServerCommandSource source) {
    UltraCalendar.database.saveAll().thenRun(() -> {
      CommandTree.sendMessage(source, UltraCalendar.language.getMessageAdminSaveSuccess());
    });
  }
}
