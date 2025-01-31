package com.kingpixel.cobblerandomreward.neoforge;

import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CobbleRandomReward.MOD_ID)
public class CobbleRandomRewardNeoForge {

  public CobbleRandomRewardNeoForge(IEventBus modBus) {
    CobbleRandomReward.init();
  }
}
