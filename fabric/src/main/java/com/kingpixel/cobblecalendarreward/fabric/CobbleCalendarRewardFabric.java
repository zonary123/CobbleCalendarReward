package com.kingpixel.cobblecalendarreward.fabric;

import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
import net.fabricmc.api.ModInitializer;

public class CobbleCalendarRewardFabric implements ModInitializer {
  @Override
  public void onInitialize() {
    CobbleCalendarReward.init();
  }
}
