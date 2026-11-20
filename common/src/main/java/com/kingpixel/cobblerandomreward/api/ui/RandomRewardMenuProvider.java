package com.kingpixel.cobblerandomreward.api.ui;

import com.kingpixel.cobbleutils.Model.ItemChance;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Service Provider Interface (SPI) for CobbleRandomReward user interfaces.
 * Allows companion mods (e.g. Rotom Phone, custom graphical screens, web dashboards)
 * to intercept and replace default chest GUI menus with custom screens.
 */
public interface RandomRewardMenuProvider {

  /**
   * Opens the main overview menu displaying all configured random reward pools.
   *
   * @param player the player opening the menu
   */
  void openMainMenu(ServerPlayerEntity player);

  /**
   * Opens the reward preview menu for a specific reward pool by its identifier.
   *
   * @param player the player viewing the pool preview
   * @param poolId the reward pool identifier
   */
  void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId);

  /**
   * Opens the reward preview menu for a specific reward pool and its item chance list.
   *
   * @param player  the player viewing the pool preview
   * @param poolId  the reward pool identifier
   * @param rewards the list of reward entries in the pool
   */
  void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId, List<ItemChance> rewards);
}
