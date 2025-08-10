package com.kingpixel.cobblecalendar.database;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.UserInfo;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 07/08/2024 9:41
 */
public class JSONClient implements DatabaseClient {
  public static final String PATH_USER_INFO = CobbleCalendar.PATH + "/data/";

  public JSONClient(String uri, String user, String password) {
  }

  @Override public void connect() {
  }

  @Override public UserInfo getUserInfo(ServerPlayerEntity player) {
    UserInfo userInfo = CobbleCalendar.userInfoMap.get(player.getUuid());
    if (userInfo != null) return userInfo;
    UUID playerUUID = player.getUuid();
    File file = Utils.getAbsolutePath(PATH_USER_INFO + player.getUuidAsString() + ".json");
    if (!file.exists()) {
      userInfo = new UserInfo(player);
      CobbleCalendar.userInfoMap.put(playerUUID, userInfo);
      updateUserInfo(player, userInfo);
      return userInfo;
    }

    try {
      String data = Utils.readFileSync(file);
      userInfo = Utils.newWithoutSpacingGson().fromJson(data, UserInfo.class);
      if (userInfo == null) {
        userInfo = new UserInfo(player);
        CobbleCalendar.LOGGER.error("Error reading user info for player: " + player.getGameProfile().getName() + " " +
          "UUID: " + player.getUuidAsString() + ". Creating new user info.");
        updateUserInfo(player, userInfo);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    CobbleCalendar.userInfoMap.put(playerUUID, userInfo);
    return userInfo;
  }

  @Override public void updateUserInfo(ServerPlayerEntity player, UserInfo userInfo) {
    if (userInfo == null) return;
    userInfo.writeInfo(player);
  }

  @Override public void disconnect() {
  }

  @Override public void save() {
  }
}
