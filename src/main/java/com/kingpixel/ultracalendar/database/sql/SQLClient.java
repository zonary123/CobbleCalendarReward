package com.kingpixel.ultracalendar.database.sql;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.sql.SQLManager;
import com.kingpixel.cobbleutils.util.sql.SQLService;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.database.DatabaseClient;
import com.kingpixel.ultracalendar.models.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SQL database client implementation supporting SQLite, MySQL, MariaDB, and H2 via CobbleUtils' SQLService.
 */
public class SQLClient extends DatabaseClient {
  private final DataBaseType dbType;
  private final SQLManager sqlManager;

  public SQLClient(@NotNull DataBaseConfig database) {
    this.dbType = database.getType() != null ? database.getType() : DataBaseType.SQLITE;
    this.sqlManager = SQLService.getOrCreateManager(database);
    createTables();
  }

  private void createTables() {
    if (sqlManager == null) {
      UltraCalendar.LOGGER.warn("SQLManager is not available; table creation skipped.");
      return;
    }
    try {
      sqlManager.execute(SQLSentences.createTable(dbType));
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Failed to create tables in SQL database for UltraCalendar", e);
    }
  }

  @Override
  public void connect() {
    UltraCalendar.LOGGER.info("Connected to SQL database (" + dbType + ") via CobbleUtils SQLService.");
  }

  @Override
  public void disconnect() {
    UltraCalendar.LOGGER.info("Disconnecting SQL database (" + dbType + ") and flushing pending saves...");
    try {
      saveAll().join();
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error flushing pending user data during SQL disconnect", e);
    }
  }

  @Override
  public CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid) {
    if (sqlManager == null) {
      return CompletableFuture.completedFuture(null);
    }
    return sqlManager.queryAsync(
      SQLSentences.findUser(dbType),
      rs -> {
        if (!rs.next()) return null;
        String json = rs.getString("data");
        if (json == null || json.isBlank()) return null;
        User user = UtilsFile.getGson().fromJson(json, User.class);
        if (user != null) {
          user.fix();
        }
        return user;
      },
      uuid.toString()
    ).exceptionally(throwable -> {
      UltraCalendar.LOGGER.error("Error reading user with UUID " + uuid + " from SQL (" + dbType + ")", throwable);
      return null;
    });
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(@NotNull User user) {
    if (sqlManager == null) {
      return CompletableFuture.completedFuture(null);
    }
    String json = UtilsFile.getGson().toJson(user);
    return sqlManager.executeAsync(
      SQLSentences.saveOrUpdateUser(dbType),
      user.getUuid().toString(),
      json
    ).thenAccept(rows -> {
      // Successfully updated rows
    }).exceptionally(throwable -> {
      UltraCalendar.LOGGER.error("Error saving user " + user.getName() + " (" + user.getUuid() + ") to SQL (" + dbType + ")", throwable);
      return null;
    });
  }

  public DataBaseType getDbType() {
    return dbType;
  }
}
