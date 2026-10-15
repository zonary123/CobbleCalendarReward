package com.kingpixel.ultracalendar.command.base;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Registry aggregator for base player subcommands under /calendar.
 *
 * @author Carlos Varas Alonso
 */
public class CalendarBaseCommand {

  private CalendarBaseCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    CalendarOpenCommand.register(base);
    CalendarHelpCommand.register(base);
  }
}
