package com.kingpixel.cobblecalendarreward.command.base;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
import com.kingpixel.cobblecalendarreward.ui.DailyRewardUI;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 02/08/2024 12:23
 */
public class CalendarRewardCommand implements Command<ServerCommandSource> {
  private static Map<UUID, Long> cooldowns = new HashMap<>();

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .executes(context -> {
          if (!context.getSource().isExecutedByPlayer()) {
            CobbleCalendarReward.LOGGER.info("This command can only be executed by a player.");
            return 0;
          }
          if (!CobbleCalendarReward.config.isActive()) return 0;
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          UIManager.openUIForcefully(player, DailyRewardUI.getPage(player));
          return 1;
        })
        .then(
          CommandManager.literal("other")
            .requires(source -> LuckPermsUtil.checkPermission(source, 2, "cobblecalendarreward.other"))
            .then(
              CommandManager.argument("player", EntityArgumentType.players())
                .executes(context -> {
                  if (!CobbleCalendarReward.config.isActive()) return 0;
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                  UIManager.openUIForcefully(player, DailyRewardUI.getPage(player));
                  return 1;
                })
            )
        )
    );

  }


  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }

}
