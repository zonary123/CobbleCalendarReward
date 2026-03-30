package com.kingpixel.cobblecalendar.database;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:03
 */
public class DatabaseClientFactory {

  public static void createDatabaseClient(DataBaseConfig database) {
    if (CobbleCalendar.database != null) CobbleCalendar.database.disconnect();
    switch (database.getType()) {
      case MONGODB -> CobbleCalendar.database = new MongoDBClient(database);
      case JSON -> CobbleCalendar.database = new JSONClient();
      default -> throw new IllegalArgumentException("Unsupported database type: " + database.getType());
    }
    CobbleCalendar.database.connect();
  }

}
