package com.kingpixel.cobblerandomreward.api;

import com.kingpixel.cobblerandomreward.api.action.RandomRewardActionResult;
import com.kingpixel.cobblerandomreward.api.model.PlayerRandomRewardContext;
import com.kingpixel.cobblerandomreward.api.model.RandomRewardBoxView;
import com.kingpixel.cobblerandomreward.api.ui.RandomRewardMenuProvider;
import com.kingpixel.cobblerandomreward.api.ui.RandomRewardMenuRegistry;
import com.kingpixel.cobblerandomreward.application.RandomRewardActionService;
import com.kingpixel.cobblerandomreward.application.RandomRewardContextService;
import com.kingpixel.cobbleutils.Model.ItemChance;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Unified public API facade for CobbleRandomReward.
 * Provides access to UI menu opening, pluggable menu provider registration,
 * headless random reward rolling/giving, configuration reloads, and immutable context viewmodels.
 */
public final class CobbleRandomRewardAPI {

  private CobbleRandomRewardAPI() {}

  // ================= UI Menu Operations =================

  /**
   * Opens the main random reward pools overview menu for the given player.
   *
   * @param player the player
   */
  public static void openMainMenu(ServerPlayerEntity player) {
    RandomRewardMenuRegistry.openMainMenu(player);
  }

  /**
   * Opens the reward pool preview menu for a specific pool identifier.
   *
   * @param player the player
   * @param poolId the reward pool identifier
   */
  public static void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId) {
    RandomRewardMenuRegistry.openRewardPoolPreviewMenu(player, poolId);
  }

  /**
   * Opens the reward pool preview menu for a specific pool and list of reward entries.
   *
   * @param player  the player
   * @param poolId  the reward pool identifier
   * @param rewards the list of reward entries
   */
  public static void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId, List<ItemChance> rewards) {
    RandomRewardMenuRegistry.openRewardPoolPreviewMenu(player, poolId, rewards);
  }

  /**
   * Registers a custom menu provider (e.g. from a companion mod or GUI overhaul).
   *
   * @param provider the custom menu provider, or null to revert to default
   */
  public static void registerMenuProvider(RandomRewardMenuProvider provider) {
    RandomRewardMenuRegistry.register(provider);
  }

  /**
   * Unregisters any custom menu provider, reverting to the default GooeyLibs implementation.
   */
  public static void unregisterMenuProvider() {
    RandomRewardMenuRegistry.unregister();
  }

  /**
   * Checks whether a custom menu provider is active.
   *
   * @return true if a custom provider is registered
   */
  public static boolean hasCustomMenuProvider() {
    return RandomRewardMenuRegistry.hasCustomProvider();
  }

  /**
   * Returns the current active menu provider.
   *
   * @return the active provider
   */
  public static RandomRewardMenuProvider getMenuProvider() {
    return RandomRewardMenuRegistry.getProvider();
  }

  // ================= Headless Action Operations =================

  /**
   * Headlessly rolls and gives a single random reward from the specified pool to a player.
   *
   * @param player the target player
   * @param poolId the reward pool identifier
   * @return future with the action result
   */
  public static CompletableFuture<RandomRewardActionResult> rollAndGiveReward(ServerPlayerEntity player,
                                                                              String poolId) {
    return RandomRewardActionService.getInstance().rollAndGiveReward(player, poolId);
  }

  /**
   * Headlessly rolls and gives multiple random rewards from the specified pool to a player.
   *
   * @param player        the target player
   * @param poolId        the reward pool identifier
   * @param amountRewards the number of independent reward rolls to perform
   * @return future with the action result
   */
  public static CompletableFuture<RandomRewardActionResult> rollAndGiveRewards(ServerPlayerEntity player,
                                                                               String poolId,
                                                                               int amountRewards) {
    return RandomRewardActionService.getInstance().rollAndGiveRewards(player, poolId, amountRewards);
  }

  /**
   * Headlessly reloads CobbleRandomReward configurations, languages, and reward pools.
   *
   * @return future with the reload action result
   */
  public static CompletableFuture<RandomRewardActionResult> reloadConfigs() {
    return RandomRewardActionService.getInstance().reloadConfigs();
  }

  /**
   * Headlessly migrates legacy CobbleUtils random reward configurations.
   *
   * @return future with the migration action result
   */
  public static CompletableFuture<RandomRewardActionResult> migrateLegacyConfigs() {
    return RandomRewardActionService.getInstance().migrateLegacyConfigs();
  }

  // ================= Context & Query Operations =================

  /**
   * Retrieves the full random reward context snapshot for a player.
   *
   * @param player the player
   * @return future with the player context
   */
  public static CompletableFuture<PlayerRandomRewardContext> getPlayerContext(ServerPlayerEntity player) {
    return RandomRewardContextService.getInstance().getPlayerContext(player);
  }

  /**
   * Retrieves an immutable viewmodel for a specific random reward pool.
   *
   * @param poolId the reward pool identifier
   * @return future with optional reward box view
   */
  public static CompletableFuture<Optional<RandomRewardBoxView>> getRewardBoxView(String poolId) {
    return RandomRewardContextService.getInstance().getRewardBoxView(poolId);
  }

  /**
   * Retrieves immutable viewmodels for all configured random reward pools.
   *
   * @return list of reward box views
   */
  public static List<RandomRewardBoxView> getAllRewardBoxes() {
    return RandomRewardContextService.getInstance().getAllRewardBoxes();
  }

  /**
   * Retrieves all configured reward pool identifiers.
   *
   * @return unmodifiable set of pool identifiers
   */
  public static Set<String> getAllPoolIds() {
    return RandomRewardContextService.getInstance().getAllPoolIds();
  }

  /**
   * Finds the reward entries for a reward pool by its identifier.
   *
   * @param poolId the reward pool identifier
   * @return optional containing the list of reward entries if found
   */
  public static Optional<List<ItemChance>> findPool(String poolId) {
    return RandomRewardActionService.getInstance().findPool(poolId);
  }
}
