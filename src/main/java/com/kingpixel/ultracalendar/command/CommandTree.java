package com.kingpixel.ultracalendar.command;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.admin.CalendarAdminCommand;
import com.kingpixel.ultracalendar.command.base.CalendarBaseCommand;
import com.kingpixel.ultracalendar.command.base.CalendarOpenCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Main command registry tree for UltraCalendar.
 *
 * @author Carlos Varas Alonso
 */
public class CommandTree {

  private CommandTree() {}

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    UltraCalendar.config.getCommands().forEach(literal -> {
      LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(literal)
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.user", 0))
        .executes(context -> CalendarOpenCommand.execute(context.getSource()));

      CalendarBaseCommand.register(base);
      CalendarAdminCommand.register(base);

      dispatcher.register(base);
    });
  }

  /**
   * Helper to send formatted text messages to any ServerCommandSource (player or console).
   * Automatically replaces the %prefix% placeholder and formats Adventure MiniMessage tags.
   */
  public static void sendMessage(ServerCommandSource source, String message) {
    if (source == null || message == null || message.isEmpty()) return;
    String formatted = message.replace("%prefix%", UltraCalendar.language.getPrefix());
    source.sendMessage(AdventureTranslator.toNative(formatted));
  }
}
