package com.kingpixel.ultracalendar.command.admin;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Registry aggregator for all administration subcommands under /calendar <subcommand> and /calendar admin <subcommand>.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarAdminCommand {

  private CalendarAdminCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    // Register directly under /calendar <admin-command>
    CalendarResetCommand.register(base);
    CalendarSetDayCommand.register(base);
    CalendarSetTimeCommand.register(base);
    CalendarClaimCommand.register(base);
    CalendarInfoCommand.register(base);
    CalendarSaveCommand.register(base);
    CalendarReloadCommand.register(base);
    CalendarOtherCommand.register(base);

    // Also register under grouped /calendar admin <admin-command>
    LiteralArgumentBuilder<ServerCommandSource> admin = CommandManager.literal("admin")
      .requires(source -> PermissionApi.hasPermission(source, "ultracalendarreward.admin", 2));

    CalendarResetCommand.register(admin);
    CalendarSetDayCommand.register(admin);
    CalendarSetTimeCommand.register(admin);
    CalendarClaimCommand.register(admin);
    CalendarInfoCommand.register(admin);
    CalendarSaveCommand.register(admin);
    CalendarReloadCommand.register(admin);
    CalendarOtherCommand.register(admin);

    base.then(admin);
  }
}
