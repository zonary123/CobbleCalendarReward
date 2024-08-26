package com.kingpixel.cobblecalendarreward.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
import com.kingpixel.cobblecalendarreward.models.LocalDateAdapter;
import com.kingpixel.cobblecalendarreward.models.UserInfo;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@ToString
public class DailyRewardsManager {
  public static final String PATH_USER_INFO = CobbleCalendarReward.PATH + "/data/";

  private final Map<UUID, UserInfo> userInfoMap = new HashMap<>();

  public void init(ServerPlayerEntity player) {
    if (CobbleCalendarReward.config.getDatabase().getType() == DataBaseType.JSON) {
      UUID playerUUID = player.getUuid();
      CompletableFuture<Boolean> futureRead = Utils.readFileAsync(PATH_USER_INFO, playerUUID + ".json",
        fileContent -> {
          Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .disableHtmlEscaping()
            .create();
          UserInfo userInfo = gson.fromJson(fileContent, UserInfo.class);
          userInfo.setLastJoin(LocalDate.now());
          userInfoMap.put(playerUUID, userInfo);
        });

      if (!futureRead.join()) {
        CobbleCalendarReward.LOGGER.info("No userinfo file found for " + CobbleCalendarReward.MOD_NAME + ". Attempting to generate one.");
        UserInfo newUserInfo = new UserInfo(player);
        userInfoMap.put(playerUUID, newUserInfo);
        newUserInfo.writeInfo(playerUUID);
      }
      userInfoMap.get(playerUUID).writeInfo(playerUUID);
    }
  }


}
