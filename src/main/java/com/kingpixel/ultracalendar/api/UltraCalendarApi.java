package com.kingpixel.ultracalendar.api;

import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.api.action.CalendarActionResult;
import com.kingpixel.ultracalendar.api.model.CalendarRewardEntryView;
import com.kingpixel.ultracalendar.api.model.PlayerCalendarContext;
import com.kingpixel.ultracalendar.api.ui.CalendarMenuProvider;
import com.kingpixel.ultracalendar.api.ui.CalendarMenuRegistry;
import com.kingpixel.ultracalendar.application.CalendarActionService;
import com.kingpixel.ultracalendar.application.CalendarContextService;
import com.kingpixel.ultracalendar.config.Config;
import com.kingpixel.ultracalendar.config.Lang;
import com.kingpixel.ultracalendar.database.DatabaseClient;
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Unified Public API for UltraCalendar.
 * Primary entry point for companion mods (Rotom Phone, custom GUIs, Web panels)
 * to interact with the calendar rewards system headlessly or via pluggable UI providers.
 */
public final class UltraCalendarApi {

  private static final CalendarActionService ACTION_SERVICE = new CalendarActionService();
  private static final CalendarContextService CONTEXT_SERVICE = new CalendarContextService();

  private UltraCalendarApi() {}

  // ================= UI / Menu Provider Methods =================

  /**
   * Opens the calendar rewards menu using the active provider.
   *
   * @param player the player opening the menu
   */
  public static void openCalendarMenu(ServerPlayerEntity player) {
    CalendarMenuRegistry.openCalendarMenu(player);
  }

  /**
   * Registers a custom menu provider.
   *
   * @param provider the custom provider
   */
  public static void registerMenuProvider(CalendarMenuProvider provider) {
    CalendarMenuRegistry.register(provider);
  }

  /**
   * Unregisters the custom menu provider, reverting back to the default GooeyLibs implementation.
   */
  public static void unregisterMenuProvider() {
    CalendarMenuRegistry.unregister();
  }

  /**
   * Checks whether a custom menu provider is currently registered.
   *
   * @return true if a custom provider is active
   */
  public static boolean hasCustomMenuProvider() {
    return CalendarMenuRegistry.hasCustomProvider();
  }

  /**
   * Retrieves the active menu provider.
   *
   * @return active menu provider
   */
  public static CalendarMenuProvider getMenuProvider() {
    return CalendarMenuRegistry.getProvider();
  }

  // ================= Headless Action Methods =================

  /**
   * Claims the current eligible calendar reward for a player headlessly.
   *
   * @param player the player
   * @return future containing action result
   */
  public static CompletableFuture<CalendarActionResult> claimReward(ServerPlayerEntity player) {
    return ACTION_SERVICE.claimReward(player);
  }

  /**
   * Claims a specific day's calendar reward for a player headlessly.
   *
   * @param player the player
   * @param day    the day number (1-based)
   * @return future containing action result
   */
  public static CompletableFuture<CalendarActionResult> claimReward(ServerPlayerEntity player, int day) {
    return ACTION_SERVICE.claimReward(player, day);
  }

  /**
   * Claims all currently available/unclaimed rewards for a player headlessly.
   *
   * @param player the player
   * @return future containing list of action results
   */
  public static CompletableFuture<List<CalendarActionResult>> claimAllAvailable(ServerPlayerEntity player) {
    return ACTION_SERVICE.claimAllAvailable(player);
  }

  /**
   * Resets progress for a player headlessly.
   *
   * @param player    the player
   * @param completed true if marked as completed streak reset
   * @return future containing action result
   */
  public static CompletableFuture<CalendarActionResult> resetUser(ServerPlayerEntity player, boolean completed) {
    return ACTION_SERVICE.resetUser(player, completed);
  }

  /**
   * Sets calendar day for a player headlessly.
   *
   * @param player the player
   * @param day    the target day
   * @return future containing action result
   */
  public static CompletableFuture<CalendarActionResult> setDay(ServerPlayerEntity player, int day) {
    return ACTION_SERVICE.setDay(player, day);
  }

  // ================= Query / View Context Methods =================

  /**
   * Retrieves the complete calendar context for a player.
   *
   * @param player the player
   * @return future containing PlayerCalendarContext
   */
  public static CompletableFuture<PlayerCalendarContext> getPlayerContext(ServerPlayerEntity player) {
    return CONTEXT_SERVICE.getPlayerContext(player);
  }

  /**
   * Retrieves a specific day's reward entry view for a player.
   *
   * @param player the player
   * @param day    the day number
   * @return future containing CalendarRewardEntryView or null
   */
  public static CompletableFuture<CalendarRewardEntryView> getRewardEntry(ServerPlayerEntity player, int day) {
    return CONTEXT_SERVICE.getRewardEntry(player, day);
  }

  /**
   * Retrieves all configured calendar rewards.
   *
   * @return list of rewards
   */
  public static List<Rewards> getAllRewards() {
    return CONTEXT_SERVICE.getAllRewards();
  }

  // ================= Service & Data Access =================

  public static CalendarActionService getActionService() {
    return ACTION_SERVICE;
  }

  public static CalendarContextService getContextService() {
    return CONTEXT_SERVICE;
  }

  public static CompletableFuture<User> getUser(ServerPlayerEntity player) {
    return UltraCalendar.database != null ? UltraCalendar.database.getUser(player) : CompletableFuture.completedFuture(null);
  }

  public static User getUserDirect(ServerPlayerEntity player) {
    return UltraCalendar.database != null ? UltraCalendar.database.getUserDirect(player) : null;
  }

  public static User getUserDirect(UUID uuid) {
    return DatabaseClient.USERS.getIfPresent(uuid);
  }

  public static Config getConfig() {
    return UltraCalendar.config;
  }

  public static Lang getLanguage() {
    return UltraCalendar.language;
  }
}
