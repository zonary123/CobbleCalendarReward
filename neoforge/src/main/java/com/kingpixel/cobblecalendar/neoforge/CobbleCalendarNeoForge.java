package com.kingpixel.cobblecalendar.neoforge;

import com.kingpixel.cobblecalendar.CobbleCalendar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CobbleCalendar.MOD_ID)
public class CobbleCalendarNeoForge {

  public CobbleCalendarNeoForge(IEventBus modBus) {
    CobbleCalendar.init();
  }
}
