package com.kingpixel.ultracalendar.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.ultracalendar.UltraCalendar;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:03
 */
public class DatabaseClientFactory {

  private DatabaseClientFactory() {}

  public static void createDatabaseClient(DataBaseConfig database) {
    if (UltraCalendar.database != null) UltraCalendar.database.disconnect();
    switch (database.getType()) {
      case MONGODB -> UltraCalendar.database = new MongoDBClient(database);
      case JSON -> UltraCalendar.database = new JSONClient();
      case SQLITE, MYSQL, MARIADB, H2 -> UltraCalendar.database = new SQLClient(database);
      default -> throw new IllegalArgumentException("Unsupported database type: " + database.getType());
    }
    UltraCalendar.database.connect();
  }

}


