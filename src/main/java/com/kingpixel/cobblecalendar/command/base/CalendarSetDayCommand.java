package com.kingpixel.cobblecalendar.command.base;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.User;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;

public class CalendarSetDayCommand {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("setDay")
          .requires(source -> PermissionApi.hasPermission(source, "cobblecalendarreward.setday", 4))
          .then(
            CommandManager.argument("day", IntegerArgumentType.integer(0, CobbleCalendar.config.maxDay()))
              .suggests((context, builder) -> {
                for (int i = 0; i <= CobbleCalendar.config.maxDay(); i++) {
                  builder.suggest(i);
                }
                return builder.buildFuture();
              })
              .then(
                CommandManager.argument("player", EntityArgumentType.player())
                  .executes(context -> {
                    if (!CobbleCalendar.config.isActive()) return 0;

                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    int day = IntegerArgumentType.getInteger(context, "day");

                    User user = CobbleCalendar.database.getUser(player);
                    if (user == null) return 0;

                    long today = LocalDate.now().toEpochDay();

                    user.setDay(Math.max(day, 0));
                    user.setLastJoin(today);
                    user.setDayClaimed(today - 1);

                    user.markDirty();
                    user.fix();

                    return 1;
                  })
              )
          )
      )
    );
  }
}