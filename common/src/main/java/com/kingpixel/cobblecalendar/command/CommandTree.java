package com.kingpixel.cobblecalendar.command;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.command.base.CalendarResetCommand;
import com.kingpixel.cobblecalendar.command.base.CalendarRewardCommand;
import com.kingpixel.cobblecalendar.command.base.CalendarSetDayCommand;
import com.kingpixel.cobblecalendar.ui.DailyRewardUI;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
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

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    CobbleCalendar.config.getCommands().forEach(literal -> {
      LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(literal)
        .requires(source -> LuckPermsUtil.checkPermission(source, 4, "cobblecalendarreward.user"))
        .executes(context -> {
          if (!context.getSource().isExecutedByPlayer()) {
            CobbleCalendar.LOGGER.info("This command can only be executed by a player.");
            return 0;
          }
          if (!CobbleCalendar.config.isActive()) return 0;
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          DailyRewardUI.open(player);
          return 1;
        });

      CalendarRewardCommand.register(dispatcher, base);
      CalendarResetCommand.register(dispatcher, base);
      CalendarSetDayCommand.register(dispatcher, base);

      dispatcher.register(
        base.then(
          CommandManager.literal("reload")
            .requires(source -> LuckPermsUtil.checkPermission(source, 4, "cobblecalendarreward.reload"))
            .executes(context -> {
              if (context.getSource().isExecutedByPlayer()) {
                context.getSource().getPlayer().sendMessage(
                  AdventureTranslator.toNative(CobbleCalendar.language.getMessageReload()
                    .replace("%prefix%", CobbleCalendar.language.getPrefix())
                  )
                );
              }
              CobbleCalendar.load();

              return 1;
            })
        )
      );
    });
  }


}
