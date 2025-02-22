package com.kingpixel.cobblecalendar.database;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.UserInfo;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 07/08/2024 9:41
 */
public class JSONClient implements DatabaseClient {
  public JSONClient(String uri, String user, String password) {
  }

  @Override public void connect() {
  }

  @Override public UserInfo getUserInfo(ServerPlayerEntity player) {
    CobbleCalendar.manager.init(player);
    return CobbleCalendar.manager.getUserInfoMap().getOrDefault(player.getUuid(), new UserInfo(player));
  }

  @Override public void updateUserInfo(UserInfo userInfo) {
    userInfo.writeInfo(userInfo.getUuid());
  }

  @Override public void disconnect() {
  }

  @Override public void save() {
  }
}
