package com.kingpixel.cobblerandomreward.application;

import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import com.kingpixel.cobblerandomreward.api.action.RandomRewardActionResult;
import com.kingpixel.cobblerandomreward.config.OldConfig;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Application service providing headless operations for rolling/distributing random rewards,
 * reloading configurations, and migrating legacy reward files in CobbleRandomReward.
 */
public class RandomRewardActionService {

  private static final RandomRewardActionService INSTANCE = new RandomRewardActionService();

  public static RandomRewardActionService getInstance() {
    return INSTANCE;
  }

  /**
   * Finds the reward entries for a reward pool by its identifier.
   *
   * @param poolId the reward pool identifier
   * @return optional containing the list of reward entries if present
   */
  public Optional<List<ItemChance>> findPool(String poolId) {
    if (poolId == null || poolId.isBlank()
        || CobbleRandomReward.rewardsConfig == null
        || CobbleRandomReward.rewardsConfig.getRewards() == null) {
      return Optional.empty();
    }
    Map<String, List<ItemChance>> rewardsMap = CobbleRandomReward.rewardsConfig.getRewards();
    List<ItemChance> exact = rewardsMap.get(poolId);
    if (exact != null) {
      return Optional.of(Collections.unmodifiableList(exact));
    }
    for (Map.Entry<String, List<ItemChance>> entry : rewardsMap.entrySet()) {
      if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(poolId) && entry.getValue() != null) {
        return Optional.of(Collections.unmodifiableList(entry.getValue()));
      }
    }
    return Optional.empty();
  }

  /**
   * Headlessly rolls and distributes a single random reward from the specified pool to a player.
   *
   * @param player the target player
   * @param poolId the reward pool identifier
   * @return future with the action result
   */
  public CompletableFuture<RandomRewardActionResult> rollAndGiveReward(ServerPlayerEntity player, String poolId) {
    return rollAndGiveRewards(player, poolId, 1, false);
  }

  /**
   * Headlessly rolls and distributes multiple random rewards from the specified pool to a player.
   *
   * @param player        the target player
   * @param poolId        the reward pool identifier
   * @param amountRewards the number of independent reward rolls to perform
   * @return future with the action result
   */
  public CompletableFuture<RandomRewardActionResult> rollAndGiveRewards(ServerPlayerEntity player,
                                                                        String poolId,
                                                                        int amountRewards) {
    return rollAndGiveRewards(player, poolId, amountRewards, false);
  }

  /**
   * Headlessly rolls and distributes multiple random rewards from the specified pool to a player,
   * optionally notifying the player on missing/empty pools.
   *
   * @param player             the target player
   * @param poolId             the reward pool identifier
   * @param amountRewards      the number of independent reward rolls to perform
   * @param notifyPlayerErrors whether to send localization error messages to the player
   * @return future with the action result
   */
  public CompletableFuture<RandomRewardActionResult> rollAndGiveRewards(ServerPlayerEntity player,
                                                                        String poolId,
                                                                        int amountRewards,
                                                                        boolean notifyPlayerErrors) {
    if (player == null) {
      return CompletableFuture.completedFuture(
          RandomRewardActionResult.offline("Player is offline.")
      );
    }

    if (poolId == null || poolId.isBlank()) {
      return CompletableFuture.completedFuture(
          RandomRewardActionResult.poolNotFound("Reward pool identifier is empty.", poolId)
      );
    }

    if (amountRewards <= 0) {
      return CompletableFuture.completedFuture(
          RandomRewardActionResult.invalidAmount(
              "Amount of rewards must be greater than zero.",
              poolId,
              amountRewards
          )
      );
    }

    return CompletableFuture.supplyAsync(() -> {
      Optional<List<ItemChance>> optPool = findPool(poolId);
      if (optPool.isEmpty()) {
        String invalidMsg = CobbleRandomReward.language != null
            ? CobbleRandomReward.language.getInvalidType()
            : "Invalid reward pool: " + poolId;
        if (notifyPlayerErrors) {
          player.sendMessage(AdventureTranslator.toNative(invalidMsg), false);
        }
        return RandomRewardActionResult.poolNotFound(invalidMsg, poolId);
      }

      List<ItemChance> items = optPool.get();
      if (items.isEmpty()) {
        String emptyMsg = CobbleRandomReward.language != null && CobbleRandomReward.language.getEmptyType() != null
            ? CobbleRandomReward.language.getEmptyType().replace("%type%", poolId)
            : "Reward pool is empty: " + poolId;
        if (notifyPlayerErrors) {
          player.sendMessage(AdventureTranslator.toNative(emptyMsg), false);
        }
        return RandomRewardActionResult.poolEmpty(emptyMsg, poolId);
      }

      List<ItemChance> rolled = ItemChance.getRewards(new ArrayList<>(items), player, amountRewards);
      List<String> grantedDescriptions = new ArrayList<>();
      for (ItemChance reward : rolled) {
        if (reward == null) continue;
        reward.giveReward(player);
        String label = reward.getDisplayname() != null && !reward.getDisplayname().isBlank()
            ? reward.getDisplayname()
            : (reward.getItem() != null ? reward.getItem() : "");
        grantedDescriptions.add(label);
      }

      return RandomRewardActionResult.success(
          "Granted " + grantedDescriptions.size() + " reward(s) from pool " + poolId,
          poolId,
          amountRewards,
          grantedDescriptions
      );
    }, CobbleRandomReward.EXECUTOR).exceptionally(ex -> {
      CobbleUtils.LOGGER.error(CobbleRandomReward.MOD_ID, "Error giving random reward: " + ex.getMessage());
      return RandomRewardActionResult.failure(RandomRewardActionResult.Status.ERROR, ex.getMessage(), poolId);
    });
  }

  /**
   * Headlessly reloads CobbleRandomReward configurations, languages, and reward pools.
   *
   * @return future with the reload action result
   */
  public CompletableFuture<RandomRewardActionResult> reloadConfigs() {
    return CompletableFuture.supplyAsync(() -> {
      CobbleRandomReward.load();
      int poolCount = CobbleRandomReward.rewardsConfig != null
          && CobbleRandomReward.rewardsConfig.getRewards() != null
          ? CobbleRandomReward.rewardsConfig.getRewards().size()
          : 0;
      String reloadMsg = CobbleRandomReward.language != null
          ? CobbleRandomReward.language.getReload()
          : "Configuration reloaded.";
      return RandomRewardActionResult.success(reloadMsg, "all", poolCount);
    }, CobbleRandomReward.EXECUTOR).exceptionally(ex -> {
      CobbleUtils.LOGGER.error(CobbleRandomReward.MOD_ID, "Error reloading configuration: " + ex.getMessage());
      return RandomRewardActionResult.failure(RandomRewardActionResult.Status.ERROR, ex.getMessage());
    });
  }

  /**
   * Headlessly migrates legacy CobbleUtils random reward configurations into CobbleRandomReward pools.
   *
   * @return future with the migration action result
   */
  public CompletableFuture<RandomRewardActionResult> migrateLegacyConfigs() {
    return CompletableFuture.supplyAsync(() -> {
      OldConfig.init();
      CobbleRandomReward.load();
      int poolCount = CobbleRandomReward.rewardsConfig != null
          && CobbleRandomReward.rewardsConfig.getRewards() != null
          ? CobbleRandomReward.rewardsConfig.getRewards().size()
          : 0;
      return RandomRewardActionResult.success("Legacy reward pools migrated.", "all", poolCount);
    }, CobbleRandomReward.EXECUTOR).exceptionally(ex -> {
      CobbleUtils.LOGGER.error(CobbleRandomReward.MOD_ID, "Error migrating legacy configs: " + ex.getMessage());
      return RandomRewardActionResult.failure(RandomRewardActionResult.Status.ERROR, ex.getMessage());
    });
  }
}
