package com.kingpixel.ultracalendar.api.ui;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Service Provider Interface (SPI) for UltraCalendar user interfaces.
 * Allows companion mods (e.g. Rotom Phone, custom graphical screens, web dashboards)
 * to intercept and replace default chest GUI menus with custom screens.
 */
public interface CalendarMenuProvider {

  /**
   * Opens the calendar rewards menu for a player.
   *
   * @param player the player opening the calendar menu
   */
  void openCalendarMenu(ServerPlayerEntity player);
}
