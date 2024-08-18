package com.kingpixel.cobblecalendarreward.database;

import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
import com.kingpixel.cobblecalendarreward.models.UserInfo;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;

/**
 * @author Carlos Varas Alonso - 07/08/2024 9:41
 */
public class JSONClient implements DatabaseClient {
  public JSONClient(String uri, String user, String password) {
  }

  @Override public void connect() {
  }

  @Override public UserInfo getUserInfo(ServerPlayerEntity player) {
    return CobbleCalendarReward.manager.getUserInfoMap().getOrDefault(player.getUuid(), new UserInfo(player));
  }


  @Override
  public boolean canClaim(ServerPlayerEntity player) {
    /* UserInfo userInfo = getUserInfo(player);
   LocalDate dayClaimed = userInfo.getDayclaimed();
    LocalDate today = LocalDate.now();
    return dayClaimed.getDayOfMonth() != today.getDayOfMonth() || dayClaimed.getMonth() != today.getMonth();*/
    return getUserInfo(player).canClaim();
  }


  @Override public void updateUserInfo(ServerPlayerEntity player) {
    UserInfo userInfo = getUserInfo(player);
    userInfo.claim();
    userInfo.writeInfo(player.getUuid());
  }


  @Override public void updateUserInfoLastJoin(ServerPlayerEntity player, LocalDate date) {
    UserInfo userInfo = getUserInfo(player);
    userInfo.computeDay();
    userInfo.writeInfo(player.getUuid());
  }

  @Override public void updateUserInfoResetDay(ServerPlayerEntity player) {
    UserInfo userInfo = getUserInfo(player);
    userInfo.reset();
    userInfo.writeInfo(player.getUuid());
  }


  @Override public void disconnect() {

  }

  @Override public void save() {
    CobbleCalendarReward.manager.getUserInfoMap().forEach((uuid, userInfo) -> {
      userInfo.writeInfo(uuid);
    });
  }
}
