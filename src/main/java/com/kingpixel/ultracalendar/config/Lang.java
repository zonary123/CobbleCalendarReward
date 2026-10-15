package com.kingpixel.ultracalendar.config;

import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultracalendar.UltraCalendar;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

@Data
public class Lang {
  private String prefix;
  private String fill;
  private String titlemenu;
  private String messageReload;
  private String messageCanClaim;
  private String messageClaimed;
  private String messageVerySoon;
  private String messageCompleted;
  private String messageReset;
  private String messageNeedPlayTime;
  private String messagePlayTimeCompleted;
  private String messageJoinNeedPlayTime;
  private String messageAdminResetSuccess;
  private String messageAdminResetAllSuccess;
  private String messageAdminSetDaySuccess;
  private String messageAdminSetTimeSuccess;
  private String messageAdminAddTimeSuccess;
  private String messageAdminClaimSuccess;
  private String messageAdminSaveSuccess;
  private String messageAdminInfo;
  private String messageAdminPlayerNotFound;
  private String messageAdminRewardNotFound;
  private String messageAdminHelp;
  private String messageAdminConsoleMustSpecifyPlayer;
  private ItemModel globalClaimed;
  private ItemModel globalCanClaim;
  private ItemModel globalNotClaimed;
  private ItemModel closeButton;

  public Lang() {
    this.prefix = "&8[&6UltraCalendarRewards&8] ";
    this.fill = "minecraft:gray_stained_glass_pane";
    this.titlemenu = "<#d9c36a>Calendar Rewards";
    this.messageReload = "%prefix% <#64de7c>Reloaded.";
    this.messageCanClaim = "%prefix% <#64de7c>You can claim rewards in <#ecca18>/calendar";
    this.messageClaimed = "%prefix% <#d65549>You have already claimed your rewards for today.";
    this.messageVerySoon = "%prefix% <#d65549>This reward will be available very soon.";
    this.messageCompleted = "%prefix% <#64de7c>You have completed the calendar and the data was reset to day 1.";
    this.messageReset = "%prefix% <#64de7c>The calendar was reset because you lost your streak.";
    this.messageNeedPlayTime = "%prefix% <#d65549>You need to play for %remaining% more today to claim day %day%.";
    this.messagePlayTimeCompleted = "%prefix% <#64de7c>You have reached the required play time! You can now claim your Day %day% reward in <#ecca18>/calendar</#ecca18>!";
    this.messageJoinNeedPlayTime = "%prefix% <#ecca18>Play for %remaining% more today to unlock your Day %day% calendar reward in <#ecca18>/calendar</#ecca18>!";
    this.messageAdminResetSuccess = "%prefix% <#64de7c>Calendar progress has been reset for <#ecca18>%player%</#ecca18>.";
    this.messageAdminResetAllSuccess = "%prefix% <#64de7c>Reset calendar progress for <#ecca18>%count%</#ecca18> online players.";
    this.messageAdminSetDaySuccess = "%prefix% <#64de7c>Set calendar progress for <#ecca18>%player%</#ecca18> to Day <#ecca18>%day%</#ecca18>.";
    this.messageAdminSetTimeSuccess = "%prefix% <#64de7c>Set daily play time for <#ecca18>%player%</#ecca18> to <#ecca18>%time%</#ecca18>.";
    this.messageAdminAddTimeSuccess = "%prefix% <#64de7c>Added <#ecca18>%added%</#ecca18> to daily play time for <#ecca18>%player%</#ecca18> (total today: <#ecca18>%time%</#ecca18>).";
    this.messageAdminClaimSuccess = "%prefix% <#64de7c>Force claimed Day <#ecca18>%day%</#ecca18> reward for <#ecca18>%player%</#ecca18>.";
    this.messageAdminSaveSuccess = "%prefix% <#64de7c>All pending calendar data has been saved to the database.";
    this.messageAdminInfo = "%prefix% <#d9c36a>--- Calendar Info for %player% ---<newline><gray>Current Day: <#64de7c>%day%/%max_day%<newline><gray>Claimed Today: %claimed_today%<newline><gray>Today's Play Time: <#64de7c>%playtime_today%</#64de7c> (Required: <#ecca18>%playtime_required%</#ecca18>)<newline><gray>Total Play Time: <#64de7c>%playtime_total%<newline><gray>Eligible to Claim: %can_claim%";
    this.messageAdminPlayerNotFound = "%prefix% <#d65549>Player not found or offline.";
    this.messageAdminRewardNotFound = "%prefix% <#d65549>Reward for Day %day% is not configured.";
    this.messageAdminHelp = "%prefix% <#d9c36a>--- Calendar Commands ---<newline><#ecca18>/calendar <gray>- Open daily calendar menu<newline><#ecca18>/calendar help <gray>- Show this help menu<newline><#ecca18>/calendar reload <gray>- Reload configurations and rewards<newline><#ecca18>/calendar other <player> <gray>- Open calendar menu for a player<newline><#ecca18>/calendar reset [player] <gray>- Reset player calendar progress<newline><#ecca18>/calendar resetall <gray>- Reset all online players<newline><#ecca18>/calendar setday <player> <day> <gray>- Set player calendar day<newline><#ecca18>/calendar settime <player> <seconds> <gray>- Set daily play time<newline><#ecca18>/calendar addtime <player> <seconds> <gray>- Add to daily play time<newline><#ecca18>/calendar claim <player> [day] <gray>- Force claim reward for player<newline><#ecca18>/calendar info <player> <gray>- Inspect player calendar data<newline><#ecca18>/calendar save <gray>- Force save all dirty data to database";
    this.messageAdminConsoleMustSpecifyPlayer = "%prefix% <#d65549>You must specify a player when executing this command from the console.";
    this.globalClaimed = new ItemModel("minecraft:minecart");
    this.globalCanClaim = new ItemModel("minecraft:hopper_minecart");
    this.globalNotClaimed = new ItemModel("minecraft:chest_minecart");
    this.closeButton = new ItemModel("minecraft:barrier", "<red>Close", List.of("<gray>Click to close the menu"));
  }

  public void init() {
    try {
      Path langFile = UltraCalendar.getPath()
        .resolve("lang")
        .resolve(UltraCalendar.config.getLang() + ".json");

      UltraCalendar.language = UtilsFile.readOrCreate(langFile, Lang.class, Lang::new);
      Lang lang = UltraCalendar.language;
      applyDefaults(lang);
      UtilsFile.writeAsync(langFile, lang);
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error loading language file for " + UltraCalendar.MOD_NAME, e);
    }
  }

  private void applyDefaults(Lang lang) {
    if (lang.closeButton == null) {
      lang.closeButton = new ItemModel("minecraft:barrier", "<red>Close", List.of("<gray>Click to close the menu"));
    }
    lang.messageNeedPlayTime = defaultIfNull(lang.messageNeedPlayTime, "%prefix% <#d65549>You need to play for %remaining% more today to claim day %day%.");
    lang.messagePlayTimeCompleted = defaultIfNull(lang.messagePlayTimeCompleted, "%prefix% <#64de7c>You have reached the required play time! You can now claim your Day %day% reward in <#ecca18>/calendar</#ecca18>!");
    lang.messageJoinNeedPlayTime = defaultIfNull(lang.messageJoinNeedPlayTime, "%prefix% <#ecca18>Play for %remaining% more today to unlock your Day %day% calendar reward in <#ecca18>/calendar</#ecca18>!");
    lang.messageAdminResetSuccess = defaultIfNull(lang.messageAdminResetSuccess, "%prefix% <#64de7c>Calendar progress has been reset for <#ecca18>%player%</#ecca18>.");
    lang.messageAdminResetAllSuccess = defaultIfNull(lang.messageAdminResetAllSuccess, "%prefix% <#64de7c>Reset calendar progress for <#ecca18>%count%</#ecca18> online players.");
    lang.messageAdminSetDaySuccess = defaultIfNull(lang.messageAdminSetDaySuccess, "%prefix% <#64de7c>Set calendar progress for <#ecca18>%player%</#ecca18> to Day <#ecca18>%day%</#ecca18>.");
    lang.messageAdminSetTimeSuccess = defaultIfNull(lang.messageAdminSetTimeSuccess, "%prefix% <#64de7c>Set daily play time for <#ecca18>%player%</#ecca18> to <#ecca18>%time%</#ecca18>.");
    lang.messageAdminAddTimeSuccess = defaultIfNull(lang.messageAdminAddTimeSuccess, "%prefix% <#64de7c>Added <#ecca18>%added%</#ecca18> to daily play time for <#ecca18>%player%</#ecca18> (total today: <#ecca18>%time%</#ecca18>).");
    lang.messageAdminClaimSuccess = defaultIfNull(lang.messageAdminClaimSuccess, "%prefix% <#64de7c>Force claimed Day <#ecca18>%day%</#ecca18> reward for <#ecca18>%player%</#ecca18>.");
    lang.messageAdminSaveSuccess = defaultIfNull(lang.messageAdminSaveSuccess, "%prefix% <#64de7c>All pending calendar data has been saved to the database.");
    lang.messageAdminInfo = defaultIfNull(lang.messageAdminInfo, "%prefix% <#d9c36a>--- Calendar Info for %player% ---<newline><gray>Current Day: <#64de7c>%day%/%max_day%<newline><gray>Claimed Today: %claimed_today%<newline><gray>Today's Play Time: <#64de7c>%playtime_today%</#64de7c> (Required: <#ecca18>%playtime_required%</#ecca18>)<newline><gray>Total Play Time: <#64de7c>%playtime_total%<newline><gray>Eligible to Claim: %can_claim%");
    lang.messageAdminPlayerNotFound = defaultIfNull(lang.messageAdminPlayerNotFound, "%prefix% <#d65549>Player not found or offline.");
    lang.messageAdminRewardNotFound = defaultIfNull(lang.messageAdminRewardNotFound, "%prefix% <#d65549>Reward for Day %day% is not configured.");
    lang.messageAdminHelp = defaultIfNull(lang.messageAdminHelp, "%prefix% <#d9c36a>--- Calendar Commands ---<newline><#ecca18>/calendar <gray>- Open daily calendar menu<newline><#ecca18>/calendar help <gray>- Show this help menu<newline><#ecca18>/calendar reload <gray>- Reload configurations and rewards<newline><#ecca18>/calendar other <player> <gray>- Open calendar menu for a player<newline><#ecca18>/calendar reset [player] <gray>- Reset player calendar progress<newline><#ecca18>/calendar resetall <gray>- Reset all online players<newline><#ecca18>/calendar setday <player> <day> <gray>- Set player calendar day<newline><#ecca18>/calendar settime <player> <seconds> <gray>- Set daily play time<newline><#ecca18>/calendar addtime <player> <seconds> <gray>- Add to daily play time<newline><#ecca18>/calendar claim <player> [day] <gray>- Force claim reward for player<newline><#ecca18>/calendar info <player> <gray>- Inspect player calendar data<newline><#ecca18>/calendar save <gray>- Force save all dirty data to database");
    lang.messageAdminConsoleMustSpecifyPlayer = defaultIfNull(lang.messageAdminConsoleMustSpecifyPlayer, "%prefix% <#d65549>You must specify a player when executing this command from the console.");
  }

  private static <T> T defaultIfNull(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  public @NotNull String getPrefix() {
    return prefix != null ? prefix : "";
  }

  public @NotNull String getMessageClaimed() {
    return messageClaimed != null ? messageClaimed : "";
  }

  public @NotNull String getMessageVerySoon() {
    return messageVerySoon != null ? messageVerySoon : "";
  }

  public @NotNull String getMessageNeedPlayTime() {
    return messageNeedPlayTime != null ? messageNeedPlayTime : "";
  }

  public @NotNull String getMessagePlayTimeCompleted() {
    return messagePlayTimeCompleted != null ? messagePlayTimeCompleted : "";
  }

  public @NotNull String getMessageJoinNeedPlayTime() {
    return messageJoinNeedPlayTime != null ? messageJoinNeedPlayTime : "";
  }

  public @NotNull String getMessageAdminResetSuccess() {
    return messageAdminResetSuccess != null ? messageAdminResetSuccess : "";
  }

  public @NotNull String getMessageAdminResetAllSuccess() {
    return messageAdminResetAllSuccess != null ? messageAdminResetAllSuccess : "";
  }

  public @NotNull String getMessageAdminSetDaySuccess() {
    return messageAdminSetDaySuccess != null ? messageAdminSetDaySuccess : "";
  }

  public @NotNull String getMessageAdminSetTimeSuccess() {
    return messageAdminSetTimeSuccess != null ? messageAdminSetTimeSuccess : "";
  }

  public @NotNull String getMessageAdminAddTimeSuccess() {
    return messageAdminAddTimeSuccess != null ? messageAdminAddTimeSuccess : "";
  }

  public @NotNull String getMessageAdminClaimSuccess() {
    return messageAdminClaimSuccess != null ? messageAdminClaimSuccess : "";
  }

  public @NotNull String getMessageAdminSaveSuccess() {
    return messageAdminSaveSuccess != null ? messageAdminSaveSuccess : "";
  }

  public @NotNull String getMessageAdminInfo() {
    return messageAdminInfo != null ? messageAdminInfo : "";
  }

  public @NotNull String getMessageAdminPlayerNotFound() {
    return messageAdminPlayerNotFound != null ? messageAdminPlayerNotFound : "";
  }

  public @NotNull String getMessageAdminRewardNotFound() {
    return messageAdminRewardNotFound != null ? messageAdminRewardNotFound : "";
  }

  public @NotNull String getMessageAdminHelp() {
    return messageAdminHelp != null ? messageAdminHelp : "";
  }

  public @NotNull String getMessageAdminConsoleMustSpecifyPlayer() {
    return messageAdminConsoleMustSpecifyPlayer != null ? messageAdminConsoleMustSpecifyPlayer : "";
  }

  public @NotNull String getFill() {
    return fill != null ? fill : "minecraft:gray_stained_glass_pane";
  }

  public @NotNull String getTitlemenu() {
    return titlemenu != null ? titlemenu : "Calendar Rewards";
  }
}
