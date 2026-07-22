package com.kingpixel.ultracalendar.database;

import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.models.User;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 07/08/2024 9:41
 */
public class JSONClient extends DatabaseClient {
  @Override
  public void connect() {
    UltraCalendar.LOGGER.info("Using JSON database client.");
  }

  @Override
  public void disconnect() {
    UltraCalendar.LOGGER.info("Disconnecting JSON database client and saving all data.");
    saveAll().join();
  }

  @Override
  public CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid) {
    return UtilsFile.readAsync(getUserFile(uuid), User.class);
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(@NotNull User user) {
    return UtilsFile.writeAsync(getUserFile(user.getUuid()), user)
      .exceptionally(throwable -> {
        UltraCalendar.LOGGER.error("Error saving user " + user.getName() + " with UUID " + user.getUuid(), throwable);
        return null;
      });
  }

  private Path getUserFile(UUID uuid) {
    return UltraCalendar.getPath().resolve("data").resolve(uuid.toString() + ".json");
  }
}

