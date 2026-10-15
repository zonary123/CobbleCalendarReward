package com.kingpixel.ultracalendar.database.sql;

import com.kingpixel.cobbleutils.Model.DataBaseType;
import org.jetbrains.annotations.NotNull;

/**
 * Provides database dialect-specific SQL query strings for SQLite, MySQL, MariaDB, and H2.
 */
public final class SQLSentences {

  public static final String TABLE_NAME = "calendarrewards_users";
  private static final String CREATE_TABLE_PREFIX = "CREATE TABLE IF NOT EXISTS ";

  private SQLSentences() {}

  /**
   * Returns the table creation statement according to the database engine.
   */
  public static @NotNull String createTable(@NotNull DataBaseType type) {
    String dataType = switch (type) {
      case H2 -> "CLOB";
      case MYSQL, MARIADB -> "LONGTEXT";
      case SQLITE -> "TEXT";
      default -> "TEXT";
    };
    return CREATE_TABLE_PREFIX + TABLE_NAME + " (playerUUID VARCHAR(36) PRIMARY KEY, data " + dataType + " NOT NULL);";
  }

  /**
   * Returns the upsert (insert or update) statement according to the database engine.
   */
  public static @NotNull String saveOrUpdateUser(@NotNull DataBaseType type) {
    return switch (type) {
      case H2 -> "MERGE INTO " + TABLE_NAME + " (playerUUID, data) KEY (playerUUID) VALUES (?, ?);";
      case SQLITE -> "INSERT INTO " + TABLE_NAME + " (playerUUID, data) VALUES (?, ?) "
        + "ON CONFLICT(playerUUID) DO UPDATE SET data = excluded.data;";
      case MYSQL, MARIADB -> "INSERT INTO " + TABLE_NAME + " (playerUUID, data) VALUES (?, ?) "
        + "ON DUPLICATE KEY UPDATE data = VALUES(data);";
      default -> "REPLACE INTO " + TABLE_NAME + " (playerUUID, data) VALUES (?, ?);";
    };
  }

  /**
   * Returns the select statement for retrieving a single user by UUID.
   */
  public static @NotNull String findUser(@NotNull DataBaseType type) {
    return "SELECT data FROM " + TABLE_NAME + " WHERE playerUUID = ?;";
  }

  /**
   * Returns the delete statement for removing a user by UUID.
   */
  public static @NotNull String deleteUser(@NotNull DataBaseType type) {
    return "DELETE FROM " + TABLE_NAME + " WHERE playerUUID = ?;";
  }

  /**
   * Returns the statement for selecting all users.
   */
  public static @NotNull String findAllUsers(@NotNull DataBaseType type) {
    return "SELECT playerUUID, data FROM " + TABLE_NAME + ";";
  }
}
