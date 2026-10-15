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
      if (UltraCalendar.language.closeButton == null) {
        UltraCalendar.language.closeButton = new ItemModel("minecraft:barrier", "<red>Close", List.of("<gray>Click to close the menu"));
      }
      if (UltraCalendar.language.messageNeedPlayTime == null) {
        UltraCalendar.language.messageNeedPlayTime = "%prefix% <#d65549>You need to play for %remaining% more today to claim day %day%.";
      }
      if (UltraCalendar.language.messagePlayTimeCompleted == null) {
        UltraCalendar.language.messagePlayTimeCompleted = "%prefix% <#64de7c>You have reached the required play time! You can now claim your Day %day% reward in <#ecca18>/calendar</#ecca18>!";
      }
      if (UltraCalendar.language.messageJoinNeedPlayTime == null) {
        UltraCalendar.language.messageJoinNeedPlayTime = "%prefix% <#ecca18>Play for %remaining% more today to unlock your Day %day% calendar reward in <#ecca18>/calendar</#ecca18>!";
      }
      UtilsFile.writeAsync(langFile, UltraCalendar.language);
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error loading language file for " + UltraCalendar.MOD_NAME, e);
    }
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

  public @NotNull String getFill() {
    return fill != null ? fill : "minecraft:gray_stained_glass_pane";
  }

  public @NotNull String getTitlemenu() {
    return titlemenu != null ? titlemenu : "Calendar Rewards";
  }
}
