package com.kingpixel.cobblecalendarreward.command;

import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
import com.kingpixel.cobblecalendarreward.command.base.CalendarResetCommand;
import com.kingpixel.cobblecalendarreward.command.base.CalendarRewardCommand;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * @author Carlos Varas Alonso - 10/06/2024 14:08
 */
public class CommandTree {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    CobbleCalendarReward.config.getCommands().forEach(literal -> {
      LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(literal);

      CalendarRewardCommand.register(dispatcher, base);
      CalendarResetCommand.register(dispatcher, base);

      dispatcher.register(
        base.then(
          CommandManager.literal("reload")
            .requires(source -> LuckPermsUtil.checkPermission(source, 2, "cobblecalendarreward.reload"))
            .executes(context -> {
              if (context.getSource().isExecutedByPlayer()) {
                context.getSource().getPlayer().sendMessage(
                  AdventureTranslator.toNative(CobbleCalendarReward.language.getMessageReload()
                    .replace("%prefix%", CobbleCalendarReward.language.getPrefix())
                  )
                );
              }
              CobbleCalendarReward.load();

              return 1;
            })
        )
      );
    });
  }


}
