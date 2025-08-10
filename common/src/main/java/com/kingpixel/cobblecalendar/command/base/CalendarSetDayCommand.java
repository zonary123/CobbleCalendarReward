package com.kingpixel.cobblecalendar.command.base;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendar.models.UserInfo;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 24/02/2025 22:54
 */
public class CalendarSetDayCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("setDay")
          .requires(source -> PermissionApi.hasPermission(source, "cobblecalendarreward.setday", 4))
          .then(
            CommandManager.argument("day", IntegerArgumentType.integer(0, CobbleCalendar.config.maxDay()))
              .suggests((commandContext, suggestionsBuilder) -> {
                for (int i = 1; i < CobbleCalendar.config.maxDay() + 1; i++) {
                  suggestionsBuilder.suggest(String.valueOf(i));
                }
                return suggestionsBuilder.buildFuture();
              })
              .then(
                CommandManager.argument("player", EntityArgumentType.players())
                  .executes(context -> {
                    if (!CobbleCalendar.config.isActive()) return 0;
                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    if (player == null) return 0;
                    CompletableFuture.runAsync(() -> {
                        int day = IntegerArgumentType.getInteger(context, "day");
                        UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
                        userInfo.setDay(Math.max(day - 1, 0));
                        long now = LocalDate.now().toEpochDay();
                        userInfo.setLastJoin(now);
                        userInfo.setDayClaimed(now - 1);
                        DatabaseClientFactory.databaseClient.updateUserInfo(player, userInfo);
                      }, CobbleCalendar.EXECUTOR_CALENDAR)
                      .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                      });
                    return 1;
                  })
              )
          )
      )
    );
  }
}
