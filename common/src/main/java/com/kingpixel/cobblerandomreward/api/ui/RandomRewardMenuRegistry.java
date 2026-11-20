package com.kingpixel.cobblerandomreward.api.ui;

import com.kingpixel.cobbleutils.Model.ItemChance;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe registry for CobbleRandomReward UI menu providers.
 * Allows companion mods (e.g. Rotom Phone, custom GUIs) to register custom menu providers.
 * If no custom provider is registered, it automatically falls back to {@link DefaultGooeyRandomRewardMenuProvider}.
 */
public final class RandomRewardMenuRegistry {

  private static final RandomRewardMenuProvider DEFAULT_PROVIDER = new DefaultGooeyRandomRewardMenuProvider();
  private static final AtomicReference<RandomRewardMenuProvider> CUSTOM_PROVIDER = new AtomicReference<>(null);

  private RandomRewardMenuRegistry() {}

  /**
   * Registers a custom menu provider. Passing null unregisters any custom provider.
   *
   * @param provider the custom menu provider, or null to revert to default
   */
  public static void register(RandomRewardMenuProvider provider) {
    CUSTOM_PROVIDER.set(provider);
  }

  /**
   * Unregisters the custom menu provider, reverting back to the default GooeyLibs implementation.
   */
  public static void unregister() {
    CUSTOM_PROVIDER.set(null);
  }

  /**
   * Checks whether a custom menu provider is currently registered.
   *
   * @return true if a custom provider is active
   */
  public static boolean hasCustomProvider() {
    return CUSTOM_PROVIDER.get() != null;
  }

  /**
   * Returns the active menu provider (custom if present, otherwise default).
   *
   * @return the active menu provider
   */
  public static RandomRewardMenuProvider getProvider() {
    RandomRewardMenuProvider custom = CUSTOM_PROVIDER.get();
    return custom != null ? custom : DEFAULT_PROVIDER;
  }

  /**
   * Opens the main random reward pools menu using the active provider.
   *
   * @param player the player
   */
  public static void openMainMenu(ServerPlayerEntity player) {
    if (player == null) return;
    getProvider().openMainMenu(player);
  }

  /**
   * Opens the reward pool preview menu by pool identifier using the active provider.
   *
   * @param player the player
   * @param poolId the reward pool identifier
   */
  public static void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId) {
    if (player == null || poolId == null) return;
    getProvider().openRewardPoolPreviewMenu(player, poolId);
  }

  /**
   * Opens the reward pool preview menu for a pool and its reward entries using the active provider.
   *
   * @param player  the player
   * @param poolId  the reward pool identifier
   * @param rewards the list of reward entries
   */
  public static void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId, List<ItemChance> rewards) {
    if (player == null || poolId == null || rewards == null) return;
    getProvider().openRewardPoolPreviewMenu(player, poolId, rewards);
  }
}
