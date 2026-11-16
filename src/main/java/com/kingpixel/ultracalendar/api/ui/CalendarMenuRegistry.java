package com.kingpixel.ultracalendar.api.ui;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe registry for UltraCalendar UI menu providers.
 * Allows companion mods (e.g. Rotom Phone, custom GUIs) to register custom menu providers.
 * If no custom provider is registered, it automatically falls back to {@link DefaultGooeyCalendarMenuProvider}.
 */
public final class CalendarMenuRegistry {

  private static final CalendarMenuProvider DEFAULT_PROVIDER = new DefaultGooeyCalendarMenuProvider();
  private static final AtomicReference<CalendarMenuProvider> CUSTOM_PROVIDER = new AtomicReference<>(null);

  private CalendarMenuRegistry() {}

  /**
   * Registers a custom menu provider. Passing null unregisters any custom provider.
   *
   * @param provider the custom menu provider, or null to revert to default
   */
  public static void register(CalendarMenuProvider provider) {
    CUSTOM_PROVIDER.set(provider);
  }

  /**
   * Unregisters the custom menu provider, reverting back to the default GooeyLibs implementation.
   */
  public static void unregister() {
    CUSTOM_PROVIDER.set(null);
  }

  /**
   * Checks whether a custom menu provider is currently registered.
   *
   * @return true if a custom provider is active
   */
  public static boolean hasCustomProvider() {
    return CUSTOM_PROVIDER.get() != null;
  }

  /**
   * Returns the active menu provider (custom if present, otherwise default).
   *
   * @return the active menu provider
   */
  public static CalendarMenuProvider getProvider() {
    CalendarMenuProvider custom = CUSTOM_PROVIDER.get();
    return custom != null ? custom : DEFAULT_PROVIDER;
  }

  /**
   * Opens the calendar rewards menu using the active provider.
   *
   * @param player the player
   */
  public static void openCalendarMenu(ServerPlayerEntity player) {
    if (player == null) return;
    getProvider().openCalendarMenu(player);
  }
}
