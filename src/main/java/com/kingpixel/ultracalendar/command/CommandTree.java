package com.kingpixel.ultracalendar.command;

import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.base.CalendarResetCommand;
import com.kingpixel.ultracalendar.command.base.CalendarRewardCommand;
import com.kingpixel.ultracalendar.command.base.CalendarSetDayCommand;
import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 10/06/2024 14:08
 */
public class CommandTree {

  private CommandTree() {}

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    UltraCalendar.config.getCommands().forEach(literal -> {
      LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(literal)
        .requires(source -> LuckPermsUtil.checkPermission(source, 4, "ultracalendarreward.user"))
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

      base.then(
        CommandManager.literal("reload")
          .requires(source -> LuckPermsUtil.checkPermission(source, 4, "ultracalendarreward.reload"))
          .executes(context -> {
            if (context.getSource().isExecutedByPlayer()) {
              context.getSource().getPlayer().sendMessage(
                AdventureTranslator.toNative(UltraCalendar.language.getMessageReload()
                  .replace("%prefix%", UltraCalendar.language.getPrefix())
                )
              );
            }
            UltraCalendar.load();
            return 1;
          })
      );

      dispatcher.register(base);
    });
  }


}

