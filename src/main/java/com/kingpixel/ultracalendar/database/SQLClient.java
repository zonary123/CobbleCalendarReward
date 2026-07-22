package com.kingpixel.ultracalendar.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.sql.SQLManager;
import com.kingpixel.cobbleutils.util.sql.SQLService;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.models.User;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLClient extends DatabaseClient {
  private final SQLManager sqlManager;

  public SQLClient(DataBaseConfig database) {
    this.sqlManager = SQLService.getOrCreateManager(database);
    createTables();
  }

  private void createTables() {
    if (sqlManager == null) return;
    sqlManager.execute("""
      CREATE TABLE IF NOT EXISTS calendarrewards_users (
        playerUUID VARCHAR(36) PRIMARY KEY,
        data TEXT NOT NULL
      )
      """);
  }

  @Override
  public void connect() {
    UltraCalendar.LOGGER.info("Using SQL database client via CobbleUtils.");
  }

  @Override
  public void disconnect() {
    UltraCalendar.LOGGER.info("Disconnecting SQL database client and saving all data.");
    saveAll().join();
  }

  @Override
  public CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid) {
    if (sqlManager == null) return CompletableFuture.completedFuture(null);
    return UltraCalendar.ASYNC.supply(() -> {
      return sqlManager.query(
        "SELECT data FROM calendarrewards_users WHERE playerUUID = ?",
        rs -> {
          if (!rs.next()) return null;
          String json = rs.getString("data");
          User user = UtilsFile.getGson().fromJson(json, User.class);
          if (user != null) {
            user.fix();
          }
          return user;
        },
        uuid.toString()
      );
    });
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(@NotNull User user) {
    if (sqlManager == null) return CompletableFuture.completedFuture(null);
    return UltraCalendar.ASYNC.runAsync(() -> {
      String json = UtilsFile.getGson().toJson(user);
      sqlManager.execute("""
        REPLACE INTO calendarrewards_users (playerUUID, data) VALUES (?, ?)
        """, user.getUuid().toString(), json);
    });
  }
}

