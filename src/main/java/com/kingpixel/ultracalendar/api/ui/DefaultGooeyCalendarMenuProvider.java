package com.kingpixel.ultracalendar.api.ui;

import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Default GooeyLibs-based menu provider implementation for UltraCalendar.
 */
public class DefaultGooeyCalendarMenuProvider implements CalendarMenuProvider {

  @Override
  public void openCalendarMenu(ServerPlayerEntity player) {
    if (player == null) return;
    DailyRewardUI.open(player);
  }
}
