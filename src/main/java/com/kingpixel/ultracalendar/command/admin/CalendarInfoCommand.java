package com.kingpixel.ultracalendar.command.admin;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles /calendar info <player>.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarInfoCommand {

  private static final String ARG_PLAYER = "player";

  private CalendarInfoCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("info")
        .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.info", 2))
        .then(
          CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
            .executes(context -> {
              if (!UltraCalendar.config.isActive()) return 0;
              ServerPlayerEntity player = EntityArgumentType.getPlayer(context, ARG_PLAYER);
              showInfo(context.getSource(), player);
              return 1;
            })
        )
    );
  }

  private static void showInfo(ServerCommandSource source, ServerPlayerEntity player) {
    if (player == null) {
      CommandTree.sendMessage(source, UltraCalendar.language.getMessageAdminPlayerNotFound());
      return;
    }

    UltraCalendar.database.getUser(player).thenAccept(user -> {
      if (user == null) return;

      long today = User.getTodayEpoch();
      boolean claimedToday = user.getDayClaimed() >= today;
      int nextDay = user.getDay() + 1;
      Rewards nextReward = UltraCalendar.config.getReward(nextDay);
      long requiredSeconds = nextReward != null ? nextReward.getRequiredTime().toSeconds() : 0;
      long dailyPlayTime = user.getDailyPlayTime();
      long totalPlayTime = user.getTotalPlayTime();
      boolean canClaimNext = user.canClaimNextReward();

      String msg = UltraCalendar.language.getMessageAdminInfo()
        .replace("%player%", player.getGameProfile().getName())
        .replace("%day%", String.valueOf(user.getDay()))
        .replace("%max_day%", String.valueOf(UltraCalendar.config.maxDay()))
        .replace("%claimed_today%", claimedToday ? "<green>Yes</green>" : "<red>No</red>")
        .replace("%playtime_today%", DailyRewardUI.formatDuration(dailyPlayTime))
        .replace("%playtime_required%", DailyRewardUI.formatDuration(requiredSeconds))
        .replace("%playtime_total%", DailyRewardUI.formatDuration(totalPlayTime))
        .replace("%can_claim%", canClaimNext ? "<green>Yes</green>" : "<red>No</red>");

      CommandTree.sendMessage(source, msg);
    });
  }
}
