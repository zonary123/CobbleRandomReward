package com.kingpixel.cobblerandomreward.config;

import com.google.gson.reflect.TypeToken;
import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 31/01/2025 6:19
 */
@Getter
@Setter
public class RewardsConfig {
  private final Map<String, List<ItemChance>> rewards = new HashMap<>();

  public void init() {
    rewards.clear();
    File folder = Utils.getAbsolutePath(CobbleRandomReward.PATH_REWARDS);
    if (!folder.exists()) {
      folder.mkdirs();
      createDefault();
    }

    for (File file : folder.listFiles()) {
      if (file.getName().endsWith(".json")) {
        String id = file.getName().replace(".json", "");
        try {
          rewards.put(id, Utils.newGson().fromJson(Utils.readFileSync(file), new TypeToken<List<ItemChance>>() {
          }.getType()));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }


  }

  private void createDefault() {
    Utils.writeFileAsync(
      CobbleRandomReward.PATH_REWARDS, "default.json", Utils.newGson().toJson(ItemChance.defaultItemChances())
    );
  }
}
