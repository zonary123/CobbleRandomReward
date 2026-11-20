package com.kingpixel.cobblerandomreward;

import com.kingpixel.cobblerandomreward.api.CobbleRandomRewardAPI;
import com.kingpixel.cobblerandomreward.api.action.RandomRewardActionResult;
import com.kingpixel.cobblerandomreward.api.model.PlayerRandomRewardContext;
import com.kingpixel.cobblerandomreward.api.model.RandomRewardBoxView;
import com.kingpixel.cobblerandomreward.api.model.RewardEntryView;
import com.kingpixel.cobblerandomreward.api.ui.DefaultGooeyRandomRewardMenuProvider;
import com.kingpixel.cobblerandomreward.api.ui.RandomRewardMenuProvider;
import com.kingpixel.cobblerandomreward.api.ui.RandomRewardMenuRegistry;
import com.kingpixel.cobblerandomreward.application.RandomRewardActionService;
import com.kingpixel.cobblerandomreward.application.RandomRewardContextService;
import com.kingpixel.cobbleutils.Model.ItemChance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCobbleRandomRewardApi {

  @BeforeEach
  @AfterEach
  void resetRegistry() {
    RandomRewardMenuRegistry.unregister();
  }

  @Test
  void testDefaultMenuProvider() {
    assertFalse(RandomRewardMenuRegistry.hasCustomProvider());
    assertNotNull(RandomRewardMenuRegistry.getProvider());
    assertTrue(RandomRewardMenuRegistry.getProvider() instanceof DefaultGooeyRandomRewardMenuProvider);
  }

  @Test
  void testCustomMenuProviderRegistrationAndFallback() {
    AtomicBoolean mainOpened = new AtomicBoolean(false);
    AtomicBoolean previewOpened = new AtomicBoolean(false);

    RandomRewardMenuProvider custom = new RandomRewardMenuProvider() {
      @Override
      public void openMainMenu(ServerPlayerEntity player) {
        mainOpened.set(true);
      }

      @Override
      public void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId) {
        previewOpened.set(true);
      }

      @Override
      public void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId, List<ItemChance> rewards) {
        previewOpened.set(true);
      }
    };

    CobbleRandomRewardAPI.registerMenuProvider(custom);
    assertTrue(CobbleRandomRewardAPI.hasCustomMenuProvider());
    assertSame(custom, CobbleRandomRewardAPI.getMenuProvider());

    // Null player guard should not invoke provider
    CobbleRandomRewardAPI.openMainMenu(null);
    assertFalse(mainOpened.get());
    CobbleRandomRewardAPI.openRewardPoolPreviewMenu(null, "daily_crate");
    assertFalse(previewOpened.get());

    CobbleRandomRewardAPI.unregisterMenuProvider();
    assertFalse(CobbleRandomRewardAPI.hasCustomMenuProvider());
    assertTrue(CobbleRandomRewardAPI.getMenuProvider() instanceof DefaultGooeyRandomRewardMenuProvider);
  }

  @Test
  void testRandomRewardActionResultStatuses() {
    RandomRewardActionResult success = RandomRewardActionResult.success(
        "Granted 2 rewards",
        "daily_crate",
        2,
        List.of("cobblemon:poke_ball", "cobblemon:ultra_ball")
    );
    assertTrue(success.isSuccess());
    assertEquals(RandomRewardActionResult.Status.SUCCESS, success.getStatus());
    assertEquals("daily_crate", success.getPoolId());
    assertEquals(2, success.getRequestedAmount());
    assertEquals(2, success.getGrantedCount());
    assertEquals(2, success.getGrantedRewards().size());
    assertThrows(UnsupportedOperationException.class, () -> success.getGrantedRewards().add("new"));

    RandomRewardActionResult notFound = RandomRewardActionResult.poolNotFound("Missing pool", "unknown_pool");
    assertFalse(notFound.isSuccess());
    assertEquals(RandomRewardActionResult.Status.POOL_NOT_FOUND, notFound.getStatus());
    assertEquals("unknown_pool", notFound.getPoolId());

    RandomRewardActionResult empty = RandomRewardActionResult.poolEmpty("Empty pool", "empty_pool");
    assertFalse(empty.isSuccess());
    assertEquals(RandomRewardActionResult.Status.POOL_EMPTY, empty.getStatus());
    assertEquals("empty_pool", empty.getPoolId());

    RandomRewardActionResult invalidAmount = RandomRewardActionResult.invalidAmount("Invalid amount", "daily_crate", -1);
    assertFalse(invalidAmount.isSuccess());
    assertEquals(RandomRewardActionResult.Status.INVALID_AMOUNT, invalidAmount.getStatus());
    assertEquals(-1, invalidAmount.getRequestedAmount());

    RandomRewardActionResult offline = RandomRewardActionResult.offline("Player is offline");
    assertFalse(offline.isSuccess());
    assertEquals(RandomRewardActionResult.Status.PLAYER_OFFLINE, offline.getStatus());

    RandomRewardActionResult noPerm = RandomRewardActionResult.noPermission("No permission");
    assertFalse(noPerm.isSuccess());
    assertEquals(RandomRewardActionResult.Status.NO_PERMISSION, noPerm.getStatus());

    RandomRewardActionResult error = RandomRewardActionResult.failure(
        RandomRewardActionResult.Status.ERROR,
        "Unexpected failure",
        "boss_loot"
    );
    assertFalse(error.isSuccess());
    assertEquals(RandomRewardActionResult.Status.ERROR, error.getStatus());
    assertEquals("boss_loot", error.getPoolId());
  }

  @Test
  void testRewardEntryAndBoxViews() {
    RewardEntryView entry1 = new RewardEntryView(
        "daily_crate",
        "cobblemon:poke_ball",
        "Poké Ball Bundle",
        75.0,
        75.0
    );
    RewardEntryView entry2 = new RewardEntryView(
        "daily_crate",
        "cobblemon:master_ball",
        "Master Ball",
        25.0,
        25.0
    );

    assertEquals("daily_crate", entry1.getPoolId());
    assertEquals("cobblemon:poke_ball", entry1.getItem());
    assertEquals("Poké Ball Bundle", entry1.getDisplayName());
    assertEquals(75.0, entry1.getChance());
    assertEquals(75.0, entry1.getProbabilityPercentage());

    RandomRewardBoxView boxView = new RandomRewardBoxView(
        "daily_crate",
        2,
        100.0,
        List.of(entry1, entry2)
    );

    assertEquals("daily_crate", boxView.getPoolId());
    assertEquals(2, boxView.getTotalEntries());
    assertEquals(100.0, boxView.getTotalWeight());
    assertEquals(2, boxView.getEntries().size());
    assertThrows(UnsupportedOperationException.class, () -> boxView.getEntries().add(entry1));
  }

  @Test
  void testPlayerRandomRewardContext() {
    UUID uuid = UUID.randomUUID();
    RandomRewardBoxView boxView = new RandomRewardBoxView("boss_loot", 1, 50.0, List.of());
    PlayerRandomRewardContext ctx = new PlayerRandomRewardContext(
        uuid,
        "AshKetchum",
        true,
        1,
        List.of(boxView)
    );

    assertEquals(uuid, ctx.getPlayerUuid());
    assertEquals("AshKetchum", ctx.getPlayerName());
    assertTrue(ctx.isHasAdminPermission());
    assertEquals(1, ctx.getTotalPools());
    assertTrue(ctx.getPool("boss_loot").isPresent());
    assertTrue(ctx.getPool("BOSS_LOOT").isPresent());
    assertFalse(ctx.getPool("non_existent").isPresent());
    assertFalse(ctx.getPool(null).isPresent());
    assertThrows(UnsupportedOperationException.class, () -> ctx.getAvailablePools().add(boxView));
  }

  @Test
  void testOfflineAndInvalidGuardsInActionService() {
    RandomRewardActionService service = RandomRewardActionService.getInstance();

    RandomRewardActionResult r1 = service.rollAndGiveReward(null, "default").join();
    assertFalse(r1.isSuccess());
    assertEquals(RandomRewardActionResult.Status.PLAYER_OFFLINE, r1.getStatus());

    RandomRewardActionResult r2 = service.rollAndGiveRewards(null, "default", 3).join();
    assertFalse(r2.isSuccess());
    assertEquals(RandomRewardActionResult.Status.PLAYER_OFFLINE, r2.getStatus());

    assertFalse(service.findPool(null).isPresent());
    assertFalse(service.findPool("").isPresent());
    assertFalse(service.findPool("non_existent_pool_xyz").isPresent());
  }

  @Test
  void testOfflineAndNullGuardsInContextService() {
    RandomRewardContextService service = RandomRewardContextService.getInstance();

    assertNull(service.getPlayerContext(null).join());
    assertEquals(Optional.empty(), service.getRewardBoxView(null).join());
    assertEquals(Optional.empty(), service.getRewardBoxView("   ").join());

    RandomRewardBoxView emptyBox = service.buildBoxView("test_pool", List.of());
    assertEquals("test_pool", emptyBox.getPoolId());
    assertEquals(0, emptyBox.getTotalEntries());
    assertEquals(0.0, emptyBox.getTotalWeight());
    assertTrue(emptyBox.getEntries().isEmpty());
  }
}
