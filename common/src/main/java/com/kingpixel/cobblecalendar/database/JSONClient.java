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
    UserInfo userInfo = CobbleCalendar.manager.getUserInfoMap().get(player.getUuid());
    if (userInfo != null) return userInfo;
    CobbleCalendar.manager.init(player);
    return CobbleCalendar.manager.getUserInfoMap().get(player.getUuid());
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
