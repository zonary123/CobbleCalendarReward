package com.kingpixel.cobblecalendarreward.database;

import com.kingpixel.cobblecalendarreward.models.UserInfo;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:02
 */
public interface DatabaseClient {
  void connect();

  UserInfo getUserInfo(ServerPlayerEntity player);

  boolean canClaim(ServerPlayerEntity player);

  void updateUserInfo(ServerPlayerEntity player);

  void updateUserInfoLastJoin(ServerPlayerEntity player, LocalDate date);

  void updateUserInfoResetDay(ServerPlayerEntity player);

  void disconnect();

  void save();

}
