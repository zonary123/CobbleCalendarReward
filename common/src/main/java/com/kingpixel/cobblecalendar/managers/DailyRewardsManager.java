package com.kingpixel.cobblecalendar.managers;

import com.google.gson.Gson;
import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.UserInfo;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@ToString
public class DailyRewardsManager {
  public static final String PATH_USER_INFO = CobbleCalendar.PATH + "/data/";

  private final Map<UUID, UserInfo> userInfoMap = new HashMap<>();

  public void init(ServerPlayerEntity player) {
    if (CobbleCalendar.config.getDatabase().getType() == DataBaseType.JSON) {
      UUID playerUUID = player.getUuid();
      CompletableFuture<Boolean> futureRead = Utils.readFileAsync(PATH_USER_INFO, playerUUID + ".json",
        fileContent -> {
          Gson gson = Utils.newWithoutSpacingGson();
          UserInfo userInfo = gson.fromJson(fileContent, UserInfo.class);
          userInfoMap.put(playerUUID, userInfo);
        });

      if (!futureRead.join()) {
        CobbleCalendar.LOGGER.info("No userinfo file found for " + CobbleCalendar.MOD_NAME + ". Attempting to generate one.");
        UserInfo newUserInfo = new UserInfo(player);
        userInfoMap.put(playerUUID, newUserInfo);
      }
      userInfoMap.get(playerUUID).writeInfo(playerUUID);
    }
  }


}
