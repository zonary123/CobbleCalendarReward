package com.kingpixel.ultracalendar.command.base;

import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import com.kingpixel.cobbleutils.api.PermissionApi;
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
        .then(
          CommandManager.literal("other")
            .requires(source -> PermissionApi.hasPermission(source, "cobblecalendarreward.other", 4))
            .then(
              CommandManager.argument("player", EntityArgumentType.players())
                .executes(context -> {
                  if (!UltraCalendar.config.isActive()) return 0;
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                  DailyRewardUI.open(player);
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
