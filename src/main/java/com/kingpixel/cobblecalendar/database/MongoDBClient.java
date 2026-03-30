package com.kingpixel.cobblecalendar.database;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.User;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDBClient extends DatabaseClient {
  private MongoCollection<User> mongoCollection;
  private MongoClient mongoClient;

  public MongoDBClient(DataBaseConfig database) {
    try {
      CodecRegistry pojoCodecRegistry = fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        fromProviders(PojoCodecProvider.builder().automatic(true).build())
      );
      MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(database.getUrl()))
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .codecRegistry(pojoCodecRegistry)
        .build();
      mongoClient = MongoClients.create(settings);

      MongoDatabase mongoDatabase = mongoClient.getDatabase(database.getDatabase());
      mongoCollection = mongoDatabase.getCollection("calendarrewards", User.class);

      connect();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public void connect() {
    CobbleCalendar.LOGGER.info("Using MongoDB database client.");
  }


  @Override
  public void disconnect() {
    CobbleCalendar.LOGGER.info("Disconnecting MongoDB database client and saving all data.");
    saveAll().join();
    if (mongoClient != null) mongoClient.close();
  }

  @Override
  public CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid) {
    return CobbleCalendar.ASYNC.supply(() -> mongoCollection.find(Filters.eq("uuid", uuid), User.class).first());
  }


  @Override
  public CompletableFuture<Void> saveOrUpdateUser(@NonNull User user) {
    return CobbleCalendar.ASYNC.runAsync(() -> mongoCollection.replaceOne(Filters.eq("uuid", user.getUuid()), user));
  }

}