package com.kingpixel.cobblecalendar.database;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.User;
import com.kingpixel.cobbleutils.util.UtilsFile;
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
    CobbleCalendar.LOGGER.info("Using JSON database client.");
  }

  @Override
  public void disconnect() {
    CobbleCalendar.LOGGER.info("Disconnecting JSON database client and saving all data.");
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
        CobbleCalendar.LOGGER.error("Error saving user " + user.getName() + " with UUID " + user.getUuid(), throwable);
        return null;
      });
  }

  private Path getUserFile(UUID uuid) {
    return CobbleCalendar.getPath().resolve("data").resolve(uuid.toString() + ".json");
  }
}
