package com.kingpixel.cobblerandomreward.api.model;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Snapshot context viewmodel for a player's random reward pool access and catalog overview.
 */
@Getter
public class PlayerRandomRewardContext {

  private final UUID playerUuid;
  private final String playerName;
  private final boolean hasAdminPermission;
  private final int totalPools;
  private final List<RandomRewardBoxView> availablePools;

  public PlayerRandomRewardContext(UUID playerUuid,
                                   String playerName,
                                   boolean hasAdminPermission,
                                   int totalPools,
                                   List<RandomRewardBoxView> availablePools) {
    this.playerUuid = playerUuid;
    this.playerName = playerName != null ? playerName : "";
    this.hasAdminPermission = hasAdminPermission;
    this.totalPools = totalPools;
    this.availablePools = availablePools != null
        ? Collections.unmodifiableList(availablePools)
        : Collections.emptyList();
  }

  public Optional<RandomRewardBoxView> getPool(String poolId) {
    if (poolId == null) return Optional.empty();
    return availablePools.stream()
        .filter(pool -> pool.getPoolId().equalsIgnoreCase(poolId))
        .findFirst();
  }
}
