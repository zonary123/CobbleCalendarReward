package com.kingpixel.ultracalendar.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBManager;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBService;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.models.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MongoDBClient extends DatabaseClient {
  private MongoCollection<User> mongoCollection;
  private MongoDBManager manager;

  public MongoDBClient(DataBaseConfig database) {
    try {
      this.manager = MongoDBService.getOrCreateManager(database);
      if (this.manager != null) {
        this.mongoCollection = this.manager.getDatabase(database.getDatabase()).getCollection("calendarrewards", User.class);
      }
      connect();
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error initializing MongoDBClient with CobbleUtils MongoDBService", e);
    }
  }

  @Override
  public void connect() {
    UltraCalendar.LOGGER.info("Using MongoDB database client via CobbleUtils.");
  }

  @Override
  public void disconnect() {
    UltraCalendar.LOGGER.info("Disconnecting MongoDB database client and saving all data.");
    saveAll().join();
  }

  @Override
  public CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid) {
    if (mongoCollection == null) return CompletableFuture.completedFuture(null);
    return UltraCalendar.ASYNC.supply(() -> mongoCollection.find(Filters.eq("uuid", uuid), User.class).first());
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(@NonNull User user) {
    if (mongoCollection == null) return CompletableFuture.completedFuture(null);
    return UltraCalendar.ASYNC.runAsync(() -> mongoCollection.replaceOne(
      Filters.eq("uuid", user.getUuid()),
      user,
      new com.mongodb.client.model.ReplaceOptions().upsert(true)
    ));
  }
}
