package com.kingpixel.ultracalendar.command.admin;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.kingpixel.ultracalendar.models.User;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles /calendar setday <player> <day> and /calendar setday <day> <player>.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarSetDayCommand {

  private static final String ARG_PLAYER = "player";
  private static final String ARG_DAY = "day";

  private CalendarSetDayCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    SuggestionProvider<ServerCommandSource> daySuggestions = (context, builder) -> {
      for (int i = 0; i <= UltraCalendar.config.maxDay(); i++) {
        builder.suggest(i);
      }
      return builder.buildFuture();
    };

    base.then(
      CommandManager.literal("setday")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.setday", 2))
        // Syntax 1: /calendar setday <player> <day>
        .then(
          CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
            .then(
              CommandManager.argument(ARG_DAY, IntegerArgumentType.integer(0, UltraCalendar.config.maxDay()))
                .suggests(daySuggestions)
                .executes(context -> {
                  if (!UltraCalendar.config.isActive()) return 0;
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(context, ARG_PLAYER);
                  int day = IntegerArgumentType.getInteger(context, ARG_DAY);
                  applySetDay(context.getSource(), player, day);
                  return 1;
                })
            )
        )
        // Syntax 2: /calendar setday <day> <player> (legacy backward compatibility)
        .then(
          CommandManager.argument(ARG_DAY, IntegerArgumentType.integer(0, UltraCalendar.config.maxDay()))
            .suggests(daySuggestions)
            .then(
              CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
                .executes(context -> {
                  if (!UltraCalendar.config.isActive()) return 0;
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(context, ARG_PLAYER);
                  int day = IntegerArgumentType.getInteger(context, ARG_DAY);
                  applySetDay(context.getSource(), player, day);
                  return 1;
                })
            )
        )
    );
  }

  private static void applySetDay(ServerCommandSource source, ServerPlayerEntity player, int day) {
    if (player == null) {
      CommandTree.sendMessage(source, UltraCalendar.language.getMessageAdminPlayerNotFound());
      return;
    }

    UltraCalendar.database.getUser(player).thenAccept(user -> {
      if (user == null) return;

      long today = User.getTodayEpoch();
      user.setDay(Math.max(day, 0));
      user.setLastJoin(today);
      user.setDayClaimed(today - 1);

      user.markDirty();
      user.fix();
      user.save();

      String msg = UltraCalendar.language.getMessageAdminSetDaySuccess()
        .replace("%player%", player.getGameProfile().getName())
        .replace("%day%", String.valueOf(day));
      CommandTree.sendMessage(source, msg);
    });
  }
}
