package com.kingpixel.cobblecalendar.fabric;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import net.fabricmc.api.ModInitializer;

public class CobbleCalendarFabric implements ModInitializer {
  @Override
  public void onInitialize() {
    CobbleCalendar.init();
  }
}
