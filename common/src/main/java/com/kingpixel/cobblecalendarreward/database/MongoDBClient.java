package com.kingpixel.cobblecalendarreward.database;

import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
import com.kingpixel.cobblecalendarreward.models.UserInfo;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.time.LocalDate;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDBClient implements DatabaseClient {
  private MongoCollection<UserInfo> mongoCollection;
  private MongoClient mongoClient;

  public MongoDBClient(DataBaseConfig database) {
    try {
      // Configurar el CodecRegistry para POJOs
      CodecRegistry pojoCodecRegistry = fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        fromProviders(PojoCodecProvider.builder().automatic(true).build())
      );

      // Configurar el MongoClientSettings con UuidRepresentation.STANDARD
      MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(database.getUrl()))
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .codecRegistry(pojoCodecRegistry)
        .build();

      // Crear el MongoClient
      mongoClient = MongoClients.create(settings);

      // Obtener la base de datos y colección
      MongoDatabase mongoDatabase = mongoClient.getDatabase(database.getDatabase());
      mongoCollection = mongoDatabase.getCollection("calendarrewards", UserInfo.class);

      connect();
    } catch (Exception e) {
      e.printStackTrace();
      CobbleCalendarReward.LOGGER.error("Error connecting to MongoDB" + e);
    }
  }

  @Override public void connect() {
    CobbleCalendarReward.LOGGER.info("Connected to MongoDB");
  }

  @Override public UserInfo getUserInfo(ServerPlayerEntity player) {
    UserInfo userInfo = mongoCollection.find(
      Filters.eq("uuid", player.getUuid()),
      UserInfo.class
    ).first();
    if (userInfo == null) {
      userInfo = new UserInfo(player);
      mongoCollection.insertOne(userInfo);
    }
    return userInfo;
  }

  @Override public boolean canClaim(ServerPlayerEntity player) {
    return getUserInfo(player).canClaim();
  }

  @Override public void updateUserInfo(ServerPlayerEntity player) {
    UserInfo userInfo = getUserInfo(player);
    userInfo.claim();
    mongoCollection.replaceOne(Filters.eq("uuid", player.getUuid()), userInfo);
  }

  @Override public void updateUserInfoLastJoin(ServerPlayerEntity player, LocalDate date) {
    UserInfo userInfo = getUserInfo(player);
    userInfo.setLastJoin(date);
    mongoCollection.replaceOne(Filters.eq("uuid", player.getUuid()), userInfo);
  }

  @Override public void updateUserInfoResetDay(ServerPlayerEntity player) {
    UserInfo userInfo = getUserInfo(player);
    userInfo.reset();
    mongoCollection.replaceOne(Filters.eq("uuid", player.getUuid()), userInfo);
  }


  @Override public void disconnect() {
    if (mongoClient != null) {
      mongoClient.close();
      CobbleCalendarReward.LOGGER.info("Disconnected from MongoDB");
    }
  }

  @Override public void save() {
    CobbleCalendarReward.LOGGER.info("Saved to MongoDB");
  }
}