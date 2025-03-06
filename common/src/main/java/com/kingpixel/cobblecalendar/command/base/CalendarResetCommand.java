package com.kingpixel.cobblecalendar.command.base;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendar.models.UserInfo;
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
public class CalendarResetCommand implements Command<ServerCommandSource> {
  private static Map<UUID, Long> cooldowns = new HashMap<>();

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("reset")
          .requires(source -> PermissionApi.hasPermission(source, "cobblecalendarreward.reset", 4))
          .executes(
            context -> {
              if (!context.getSource().isExecutedByPlayer()) {
                CobbleCalendar.LOGGER.info("This command can only be executed by a player.");
                return 0;
              }
              if (!CobbleCalendar.config.isActive()) return 0;
              ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
              reset(player);
              return 1;
            }).then(
            CommandManager.argument("player", EntityArgumentType.players())
              .executes(context -> {
                if (!CobbleCalendar.config.isActive()) return 0;
                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                reset(player);
                return 1;
              })
          )
      )
    );

  }

  private static void reset(ServerPlayerEntity player) {
    UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
    userInfo.reset(player, true);
    userInfo.setDayClaimed(0);
    DatabaseClientFactory.databaseClient.updateUserInfo(player, userInfo);
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }

}
