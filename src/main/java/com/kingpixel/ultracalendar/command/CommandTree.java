package com.kingpixel.ultracalendar.command;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.base.*;
import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

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
        .executes(context -> {
          if (!context.getSource().isExecutedByPlayer()) {
            UltraCalendar.LOGGER.info("This command can only be executed by a player.");
            return 0;
          }
          if (!UltraCalendar.config.isActive()) return 0;
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          DailyRewardUI.open(player);
          return 1;
        });

      CalendarRewardCommand.register(base);
      CalendarResetCommand.register(base);
      CalendarSetDayCommand.register(base);
      CalendarSetTimeCommand.register(base);
      CalendarClaimCommand.register(base);
      CalendarInfoCommand.register(base);
      CalendarSaveCommand.register(base);
      CalendarHelpCommand.register(base);

      base.then(
        CommandManager.literal("reload")
          .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.reload", 4))
          .executes(context -> {
            UltraCalendar.load();
            sendMessage(context.getSource(), UltraCalendar.language.getMessageReload());
            return 1;
          })
      );

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
