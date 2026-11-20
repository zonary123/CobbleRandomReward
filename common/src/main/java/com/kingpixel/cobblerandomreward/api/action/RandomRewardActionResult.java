package com.kingpixel.cobblerandomreward.api.action;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result representation for CobbleRandomReward headless reward distribution
 * and administrative actions.
 */
@Getter
public class RandomRewardActionResult {

  public enum Status {
    SUCCESS,
    POOL_NOT_FOUND,
    POOL_EMPTY,
    INVALID_AMOUNT,
    PLAYER_OFFLINE,
    NO_PERMISSION,
    ERROR
  }

  private final boolean success;
  private final Status status;
  private final String message;
  private final String poolId;
  private final int requestedAmount;
  private final int grantedCount;
  private final List<String> grantedRewards;

  private RandomRewardActionResult(boolean success,
                                   Status status,
                                   String message,
                                   String poolId,
                                   int requestedAmount,
                                   int grantedCount,
                                   List<String> grantedRewards) {
    this.success = success;
    this.status = status;
    this.message = message != null ? message : "";
    this.poolId = poolId != null ? poolId : "";
    this.requestedAmount = requestedAmount;
    this.grantedCount = grantedCount;
    this.grantedRewards = grantedRewards != null
        ? Collections.unmodifiableList(grantedRewards)
        : Collections.emptyList();
  }

  public static RandomRewardActionResult success(String message,
                                                 String poolId,
                                                 int requestedAmount,
                                                 List<String> grantedRewards) {
    List<String> safeRewards = grantedRewards != null ? grantedRewards : Collections.emptyList();
    return new RandomRewardActionResult(
        true,
        Status.SUCCESS,
        message,
        poolId,
        requestedAmount,
        safeRewards.size(),
        safeRewards
    );
  }

  public static RandomRewardActionResult success(String message, String poolId, int count) {
    return new RandomRewardActionResult(
        true,
        Status.SUCCESS,
        message,
        poolId,
        count,
        count,
        Collections.emptyList()
    );
  }

  public static RandomRewardActionResult poolNotFound(String message, String poolId) {
    return new RandomRewardActionResult(
        false,
        Status.POOL_NOT_FOUND,
        message,
        poolId,
        0,
        0,
        Collections.emptyList()
    );
  }

  public static RandomRewardActionResult poolEmpty(String message, String poolId) {
    return new RandomRewardActionResult(
        false,
        Status.POOL_EMPTY,
        message,
        poolId,
        0,
        0,
        Collections.emptyList()
    );
  }

  public static RandomRewardActionResult invalidAmount(String message, String poolId, int requestedAmount) {
    return new RandomRewardActionResult(
        false,
        Status.INVALID_AMOUNT,
        message,
        poolId,
        requestedAmount,
        0,
        Collections.emptyList()
    );
  }

  public static RandomRewardActionResult offline(String message) {
    return new RandomRewardActionResult(
        false,
        Status.PLAYER_OFFLINE,
        message,
        "",
        0,
        0,
        Collections.emptyList()
    );
  }

  public static RandomRewardActionResult noPermission(String message) {
    return new RandomRewardActionResult(
        false,
        Status.NO_PERMISSION,
        message,
        "",
        0,
        0,
        Collections.emptyList()
    );
  }

  public static RandomRewardActionResult failure(Status status, String message) {
    return new RandomRewardActionResult(
        false,
        status,
        message,
        "",
        0,
        0,
        Collections.emptyList()
    );
  }

  public static RandomRewardActionResult failure(Status status, String message, String poolId) {
    return new RandomRewardActionResult(
        false,
        status,
        message,
        poolId,
        0,
        0,
        Collections.emptyList()
    );
  }
}
