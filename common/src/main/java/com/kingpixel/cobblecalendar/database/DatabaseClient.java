package com.kingpixel.cobblecalendar.database;

import com.kingpixel.cobblecalendar.models.UserInfo;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:02
 */
public interface DatabaseClient {
  void connect();

  UserInfo getUserInfo(ServerPlayerEntity player);
  
  void updateUserInfo(UserInfo userInfo);

  void disconnect();

  void save();

}
