package com.kingpixel.cobblerandomreward.api.model;

import lombok.Getter;

/**
 * Immutable viewmodel representing a single weighted reward entry inside a random reward pool.
 */
@Getter
public class RewardEntryView {

  private final String poolId;
  private final String item;
  private final String displayName;
  private final double chance;
  private final double probabilityPercentage;

  public RewardEntryView(String poolId,
                         String item,
                         String displayName,
                         double chance,
                         double probabilityPercentage) {
    this.poolId = poolId != null ? poolId : "";
    this.item = item != null ? item : "";
    this.displayName = displayName != null ? displayName : "";
    this.chance = chance;
    this.probabilityPercentage = probabilityPercentage;
  }
}
