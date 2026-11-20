package com.kingpixel.cobblerandomreward.api.model;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Immutable viewmodel representing a configured random reward box / pool and its weighted entries.
 */
@Getter
public class RandomRewardBoxView {

  private final String poolId;
  private final int totalEntries;
  private final double totalWeight;
  private final List<RewardEntryView> entries;

  public RandomRewardBoxView(String poolId,
                             int totalEntries,
                             double totalWeight,
                             List<RewardEntryView> entries) {
    this.poolId = poolId != null ? poolId : "";
    this.totalEntries = totalEntries;
    this.totalWeight = totalWeight;
    this.entries = entries != null ? Collections.unmodifiableList(entries) : Collections.emptyList();
  }
}
