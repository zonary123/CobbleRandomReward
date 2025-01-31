package com.kingpixel.cobblerandomreward.fabric;

import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import net.fabricmc.api.ModInitializer;

public class CobbleRandomRewardFabric implements ModInitializer {
  @Override
  public void onInitialize() {
    CobbleRandomReward.init();
  }
}
