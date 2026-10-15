package com.kingpixel.ultracalendar.database.mongodb;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBService;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.database.DatabaseClient;
import com.kingpixel.ultracalendar.models.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MongoDB database client implementation using CobbleUtils MongoDBService.
 */
public class MongoDBClient extends DatabaseClient {
  public static final String COLLECTION_NAME = "calendarrewards";
  private MongoCollection<Document> mongoCollection;

  public MongoDBClient(@NotNull DataBaseConfig database) {
    try {
      this.mongoCollection = MongoDBService.getCollection(database, COLLECTION_NAME);
      connect();
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error initializing MongoDBClient via CobbleUtils MongoDBService", e);
    }
  }

  @Override
  public void connect() {
    UltraCalendar.LOGGER.info("Connected to MongoDB collection '" + COLLECTION_NAME + "' via CobbleUtils.");
  }

  @Override
  public void disconnect() {
    UltraCalendar.LOGGER.info("Disconnecting MongoDB client and flushing pending saves...");
    try {
      saveAll().join();
    } catch (Exception e) {
      UltraCalendar.LOGGER.error("Error flushing pending user data during MongoDB disconnect", e);
    }
    USERS.invalidateAll();
  }

  @Override
  public CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid) {
    if (mongoCollection == null) {
      return CompletableFuture.completedFuture(null);
    }
    return UltraCalendar.ASYNC.supply(() -> {
      try {
        Document doc = mongoCollection.find(Filters.eq("uuid", uuid.toString())).first();
        if (doc == null) return null;
        User user = UtilsFile.getGson().fromJson(doc.toJson(), User.class);
        if (user != null) {
          user.fix();
        }
        return user;
      } catch (Exception e) {
        UltraCalendar.LOGGER.error("Error reading user with UUID " + uuid + " from MongoDB", e);
        return null;
      }
    });
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(@NotNull User user) {
    if (mongoCollection == null) {
      return CompletableFuture.completedFuture(null);
    }
    return UltraCalendar.ASYNC.runAsync(() -> {
      try {
        Document doc = Document.parse(UtilsFile.getGson().toJson(user));
        doc.put("uuid", user.getUuid().toString());
        mongoCollection.replaceOne(
          Filters.eq("uuid", user.getUuid().toString()),
          doc,
          new ReplaceOptions().upsert(true)
        );
      } catch (Exception e) {
        UltraCalendar.LOGGER.error("Error saving user {} ({}) to MongoDB", user.getName(), user.getUuid(), e);
      }
    });
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUsers(@NotNull List<User> users) {
    if (mongoCollection == null || users.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    if (users.size() == 1) {
      return saveOrUpdateUser(users.get(0));
    }
    return UltraCalendar.ASYNC.runAsync(() -> {
      try {
        List<WriteModel<Document>> operations = new ArrayList<>(users.size());
        for (User user : users) {
          Document doc = Document.parse(UtilsFile.getGson().toJson(user));
          doc.put("uuid", user.getUuid().toString());
          operations.add(new ReplaceOneModel<>(
            Filters.eq("uuid", user.getUuid().toString()),
            doc,
            new ReplaceOptions().upsert(true)
          ));
        }
        mongoCollection.bulkWrite(operations);
      } catch (Exception e) {
        UltraCalendar.LOGGER.error("Error bulk saving {} users to MongoDB", users.size(), e);
      }
    });
  }
}
