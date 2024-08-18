package com.kingpixel.cobblecalendarreward.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:03
 */
public class DatabaseClientFactory {
  public static DatabaseClient databaseClient;

  public static void createDatabaseClient(DataBaseConfig database) {
    if (databaseClient != null) {
      databaseClient.disconnect();
    }
    switch (database.getType()) {
      case MONGODB -> databaseClient = new MongoDBClient(database);
      case JSON -> databaseClient = new JSONClient(database.getUrl(), database.getUser(),
        database.getPassword());
      default -> databaseClient = new JSONClient(database.getUrl(), database.getUser(),
        database.getPassword());
    }
    databaseClient.connect();
  }
}
