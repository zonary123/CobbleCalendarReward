package com.kingpixel.cobblecalendar.database;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.UserInfo;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.bson.UuidRepresentation;
import com.kingpixel.cobbleutils.bson.codecs.configuration.CodecRegistry;
import com.kingpixel.cobbleutils.bson.codecs.pojo.PojoCodecProvider;
import com.kingpixel.cobbleutils.mongodb.ConnectionString;
import com.kingpixel.cobbleutils.mongodb.MongoClientSettings;
import com.kingpixel.cobbleutils.mongodb.client.MongoClient;
import com.kingpixel.cobbleutils.mongodb.client.MongoClients;
import com.kingpixel.cobbleutils.mongodb.client.MongoCollection;
import com.kingpixel.cobbleutils.mongodb.client.MongoDatabase;
import com.kingpixel.cobbleutils.mongodb.client.model.Filters;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.kingpixel.cobbleutils.bson.codecs.configuration.CodecRegistries.fromProviders;
import static com.kingpixel.cobbleutils.bson.codecs.configuration.CodecRegistries.fromRegistries;

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
      CobbleCalendar.LOGGER.error("Error connecting to MongoDB" + e);
    }
  }

  @Override public void connect() {
    CobbleCalendar.LOGGER.info("Connected to MongoDB");
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
    userInfo.computeDay(player);
    return userInfo;
  }

  @Override public void updateUserInfo(ServerPlayerEntity player, UserInfo userInfo) {
    mongoCollection.replaceOne(Filters.eq("uuid", userInfo.getUuid()), userInfo);
  }


  @Override public void disconnect() {
    if (mongoClient != null) {
      mongoClient.close();
      CobbleCalendar.LOGGER.info("Disconnected from MongoDB");
    }
  }

  @Override public void save() {
    CobbleCalendar.LOGGER.info("Saved to MongoDB");
  }
}