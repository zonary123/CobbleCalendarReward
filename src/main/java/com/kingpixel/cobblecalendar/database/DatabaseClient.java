package com.kingpixel.cobblecalendar.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobblecalendar.models.User;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:02
 */
public abstract class DatabaseClient {
  public static final Cache<UUID, User> USERS = Caffeine.newBuilder()
    .build();

  public abstract void connect();

  public abstract void disconnect();

  public @Nullable User getUser(@NotNull ServerPlayerEntity player) {
    return getUser(player.getUuid());
  }

  public @Nullable User getUser(@NotNull UUID uuid) {
    return USERS.getIfPresent(uuid);
  }

  public CompletableFuture<@Nullable User> findUser(@NotNull ServerPlayerEntity player) {
    return findUser(player.getUuid());
  }

  public abstract CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid);

  public abstract CompletableFuture<Void> saveOrUpdateUser(@NotNull User user);

  public CompletableFuture<Void> saveAll() {
    return CompletableFuture.allOf(USERS.asMap().values().stream()
      .map(User::save)
      .toArray(CompletableFuture[]::new));
  }

}
