package com.kingpixel.cobblerandomreward.application;

import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import com.kingpixel.cobblerandomreward.api.model.PlayerRandomRewardContext;
import com.kingpixel.cobblerandomreward.api.model.RandomRewardBoxView;
import com.kingpixel.cobblerandomreward.api.model.RewardEntryView;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.api.PermissionApi;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Application service providing read-only queries and immutable viewmodels
 * for CobbleRandomReward reward pools and player contexts.
 */
public class RandomRewardContextService {

  private static final RandomRewardContextService INSTANCE = new RandomRewardContextService();

  public static RandomRewardContextService getInstance() {
    return INSTANCE;
  }

  /**
   * Retrieves the identifiers of all configured random reward pools.
   *
   * @return unmodifiable set of pool identifiers
   */
  public Set<String> getAllPoolIds() {
    if (CobbleRandomReward.rewardsConfig == null || CobbleRandomReward.rewardsConfig.getRewards() == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(CobbleRandomReward.rewardsConfig.getRewards().keySet());
  }

  /**
   * Retrieves immutable views for all configured random reward boxes / pools.
   *
   * @return list of reward box views
   */
  public List<RandomRewardBoxView> getAllRewardBoxes() {
    if (CobbleRandomReward.rewardsConfig == null || CobbleRandomReward.rewardsConfig.getRewards() == null) {
      return Collections.emptyList();
    }
    Map<String, List<ItemChance>> pools = CobbleRandomReward.rewardsConfig.getRewards();
    List<RandomRewardBoxView> views = new ArrayList<>();
    for (Map.Entry<String, List<ItemChance>> entry : pools.entrySet()) {
      if (entry.getKey() == null) continue;
      views.add(buildBoxView(entry.getKey(), entry.getValue()));
    }
    return Collections.unmodifiableList(views);
  }

  /**
   * Retrieves an immutable view for a specific random reward box / pool asynchronously.
   *
   * @param poolId the reward pool identifier
   * @return future with optional reward box view
   */
  public CompletableFuture<Optional<RandomRewardBoxView>> getRewardBoxView(String poolId) {
    if (poolId == null || poolId.isBlank()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    return CompletableFuture.<Optional<RandomRewardBoxView>>supplyAsync(() -> {
      Optional<List<ItemChance>> optPool = RandomRewardActionService.getInstance().findPool(poolId);
      if (optPool.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(buildBoxView(poolId, optPool.get()));
    }, CobbleRandomReward.EXECUTOR).exceptionally(ex -> {
      CobbleUtils.LOGGER.error(CobbleRandomReward.MOD_ID, "Error building reward box view: " + ex.getMessage());
      return Optional.empty();
    });
  }

  /**
   * Retrieves the full random reward context snapshot for a player asynchronously.
   *
   * @param player the player
   * @return future with the player random reward context, or null if player is null
   */
  public CompletableFuture<PlayerRandomRewardContext> getPlayerContext(ServerPlayerEntity player) {
    if (player == null) {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.supplyAsync(() -> {
      boolean hasAdmin = PermissionApi.hasPermission(
          player.getCommandSource(),
          List.of("cobblerandomreward.admin"),
          4
      );
      List<RandomRewardBoxView> boxes = getAllRewardBoxes();
      return new PlayerRandomRewardContext(
          player.getUuid(),
          player.getName().getString(),
          hasAdmin,
          boxes.size(),
          boxes
      );
    }, CobbleRandomReward.EXECUTOR).exceptionally(ex -> {
      CobbleUtils.LOGGER.error(CobbleRandomReward.MOD_ID, "Error building player context: " + ex.getMessage());
      return new PlayerRandomRewardContext(
          player.getUuid(),
          player.getName().getString(),
          false,
          0,
          Collections.emptyList()
      );
    });
  }

  /**
   * Builds an immutable {@link RandomRewardBoxView} from a pool identifier and list of {@link ItemChance}.
   *
   * @param poolId the reward pool identifier
   * @param items  the list of reward entries
   * @return the constructed immutable view
   */
  public RandomRewardBoxView buildBoxView(String poolId, List<ItemChance> items) {
    List<ItemChance> safeItems = items != null ? items : Collections.emptyList();
    double totalWeight = 0.0;
    for (ItemChance ic : safeItems) {
      if (ic != null && ic.getChance() > 0) {
        totalWeight += ic.getChance();
      }
    }

    List<RewardEntryView> entryViews = new ArrayList<>();
    for (ItemChance ic : safeItems) {
      if (ic == null) continue;
      double weight = Math.max(0.0, ic.getChance());
      double probabilityPct = totalWeight > 0.0 ? (weight / totalWeight) * 100.0 : 0.0;
      String itemStr = ic.getItem() != null ? ic.getItem() : "";
      String displayName = ic.getDisplayname() != null && !ic.getDisplayname().isBlank()
          ? ic.getDisplayname()
          : itemStr;
      entryViews.add(new RewardEntryView(
          poolId,
          itemStr,
          displayName,
          weight,
          probabilityPct
      ));
    }

    return new RandomRewardBoxView(
        poolId,
        entryViews.size(),
        totalWeight,
        entryViews
    );
  }
}
