package com.kingpixel.ultracalendar.database.json;

import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.database.DatabaseClient;
import com.kingpixel.ultracalendar.models.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * JSON file-based database client implementation.
 */
public class JSONClient extends DatabaseClient {

  @Override
  public void connect() {
    UltraCalendar.LOGGER.info("Connected to JSON file database.");
  }

  @Override
  public void disconnect() {
    UltraCalendar.LOGGER.info("Disconnecting JSON database client and flushing pending saves...");
    try {
      saveAll().join();
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error flushing pending user data during JSON database disconnect", e);
    }
  }

  @Override
  public CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid) {
    return UtilsFile.readAsync(getUserFile(uuid), User.class)
      .exceptionally(throwable -> {
        UltraCalendar.LOGGER.error("Error reading JSON user file for UUID: " + uuid, throwable);
        return null;
      });
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(@NotNull User user) {
    return UtilsFile.writeAsync(getUserFile(user.getUuid()), user)
      .exceptionally(throwable -> {
        UltraCalendar.LOGGER.error("Error saving user " + user.getName() + " (" + user.getUuid() + ") to JSON file", throwable);
        return null;
      });
  }

  private Path getUserFile(@NotNull UUID uuid) {
    return UltraCalendar.getPath().resolve("data").resolve(uuid.toString() + ".json");
  }
}
