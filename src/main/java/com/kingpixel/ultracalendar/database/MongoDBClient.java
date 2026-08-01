package com.kingpixel.ultracalendar.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBManager;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBService;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.models.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MongoDBClient extends DatabaseClient {
  private MongoCollection<Document> mongoCollection;
  private MongoDBManager manager;

  public MongoDBClient(DataBaseConfig database) {
    try {
      this.manager = MongoDBService.getOrCreateManager(database);
      if (this.manager != null) {
        this.mongoCollection = this.manager.getDatabase(database.getDatabase()).getCollection("calendarrewards");
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
    return UltraCalendar.ASYNC.supply(() -> {
      Document doc = mongoCollection.find(Filters.eq("uuid", uuid.toString())).first();
      if (doc == null) return null;
      User user = UtilsFile.getGson().fromJson(doc.toJson(), User.class);
      if (user != null) user.fix();
      return user;
    });
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(@NonNull User user) {
    if (mongoCollection == null) return CompletableFuture.completedFuture(null);
    return UltraCalendar.ASYNC.runAsync(() -> {
      Document doc = Document.parse(UtilsFile.getGson().toJson(user));
      doc.put("uuid", user.getUuid().toString());
      mongoCollection.replaceOne(
        Filters.eq("uuid", user.getUuid().toString()),
        doc,
        new ReplaceOptions().upsert(true)
      );
    });
  }
}
