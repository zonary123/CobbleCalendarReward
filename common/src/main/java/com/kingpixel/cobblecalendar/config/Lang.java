package com.kingpixel.cobblecalendar.config;

import com.google.gson.Gson;
import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
public class Lang {
  private String prefix;
  private String fill;
  private String titlemenu;
  private String messageReload;
  private String messageCanClaim;
  private String messageClaimed;
  private String messageVerySoon;

  /**
   * Constructor to generate a file if one doesn't exist.
   */
  public Lang() {
    this.prefix = "&8[&6CobbleCalendarRewards&8] ";
    this.fill = "minecraft:gray_stained_glass_pane";
    this.titlemenu = "<#d9c36a>Calendar Rewards";
    this.messageReload = "%prefix% <#64de7c>Reloaded.";
    this.messageCanClaim = "%prefix% <#64de7c>You can claim rewards in <#ecca18>/calendar";
    this.messageClaimed = "%prefix% <#d65549>You have already claimed your rewards for today.";
    this.messageVerySoon = "%prefix% <#d65549>This reward will be available very soon.";
  }

  /**
   * Method to initialize the config.
   */
  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleCalendar.PATH_LANG,
      CobbleCalendar.config.getLang() + ".json",
      el -> {
        Gson gson = Utils.newGson();
        Lang lang = gson.fromJson(el, Lang.class);
        this.prefix = lang.getPrefix();
        this.fill = lang.getFill();
        this.titlemenu = lang.getTitlemenu();
        this.messageReload = lang.getMessageReload();
        this.messageCanClaim = lang.getMessageCanClaim();
        this.messageClaimed = lang.getMessageClaimed();
        this.messageVerySoon = lang.getMessageVerySoon();

        String data = gson.toJson(this);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleCalendar.PATH_LANG, CobbleCalendar.config.getLang() +
            ".json",
          data);
        if (!futureWrite.join()) {
          CobbleCalendar.LOGGER.fatal("Could not write lang.json file for " + CobbleCalendar.MOD_NAME + ".");
        }
      });

    if (!futureRead.join()) {
      CobbleCalendar.LOGGER.info("No lang.json file found for" + CobbleCalendar.MOD_NAME + ". Attempting to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleCalendar.PATH_LANG, CobbleCalendar.config.getLang() +
          ".json",
        data);

      if (!futureWrite.join()) {
        CobbleCalendar.LOGGER.fatal("Could not write lang.json file for " + CobbleCalendar.MOD_NAME + ".");
      }
    }
  }

}
