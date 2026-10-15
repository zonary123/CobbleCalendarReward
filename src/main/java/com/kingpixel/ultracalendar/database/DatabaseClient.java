package com.kingpixel.ultracalendar.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.ultracalendar.models.User;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base abstract class for database operations with unified Caffeine caching and asynchronous user loading.
 *
 * @author Carlos Varas Alonso
 */
public abstract class DatabaseClient {
  public static final Cache<UUID, User> USERS = Caffeine.newBuilder()
    .expireAfterAccess(60, TimeUnit.MINUTES)
    .maximumSize(5000)
    .build();

  public abstract void connect();

  public abstract void disconnect();

  /**
   * Fast, non-blocking synchronous cache retrieval.
   * Useful for high-frequency game ticks (e.g. playtime counting) and synchronous events.
   */
  public @Nullable User getUserDirect(@NotNull UUID uuid) {
    return USERS.getIfPresent(uuid);
  }

  /**
   * Fast, non-blocking synchronous cache retrieval for an online player.
   */
  public @Nullable User getUserDirect(@NotNull ServerPlayerEntity player) {
    return getUserDirect(player.getUuid());
  }

  /**
   * Asynchronous user retrieval. Checks local memory cache first. If not present,
   * queries the underlying database asynchronously, caches the result, and returns it.
   */
  public CompletableFuture<@Nullable User> getUser(@NotNull UUID uuid) {
    User cached = USERS.getIfPresent(uuid);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }
    return findUser(uuid).thenApply(user -> {
      if (user != null) {
        USERS.put(uuid, user);
      }
      return user;
    });
  }

  /**
   * Asynchronous user retrieval for an online player.
   */
  public CompletableFuture<@Nullable User> getUser(@NotNull ServerPlayerEntity player) {
    return getUser(player.getUuid());
  }

  /**
   * Pure database fetch for an online player.
   */
  public CompletableFuture<@Nullable User> findUser(@NotNull ServerPlayerEntity player) {
    return findUser(player.getUuid());
  }

  /**
   * Pure database fetch by UUID, bypassing the cache.
   */
  public abstract CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid);

  /**
   * Asynchronously saves or updates the user in the database.
   */
  public abstract CompletableFuture<Void> saveOrUpdateUser(@NotNull User user);

  /**
   * Saves all dirty users currently in the cache.
   */
  public CompletableFuture<Void> saveAll() {
    return CompletableFuture.allOf(USERS.asMap().values().stream()
      .map(User::save)
      .toArray(CompletableFuture[]::new));
  }
}
