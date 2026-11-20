package com.kingpixel.cobblerandomreward.api.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default GooeyLibs-based menu provider implementation for CobbleRandomReward.
 */
public class DefaultGooeyRandomRewardMenuProvider implements RandomRewardMenuProvider {

  @Override
  public void openMainMenu(ServerPlayerEntity player) {
    if (player == null) return;
    if (CobbleRandomReward.rewardsConfig == null || CobbleRandomReward.rewardsConfig.getRewards() == null) return;

    Map<String, List<ItemChance>> pools = CobbleRandomReward.rewardsConfig.getRewards();
    List<Button> buttons = new ArrayList<>();

    for (Map.Entry<String, List<ItemChance>> entry : pools.entrySet()) {
      String poolId = entry.getKey();
      List<ItemChance> items = entry.getValue() != null ? entry.getValue() : List.of();
      double totalWeight = items.stream().mapToDouble(ItemChance::getChance).sum();

      List<String> loreLines = CobbleRandomReward.language.getLorePoolEntry().stream()
          .map(line -> line
              .replace("%type%", poolId)
              .replace("%count%", String.valueOf(items.size()))
              .replace("%weight%", String.format("%.1f", totalWeight)))
          .toList();

      ItemStack icon = new ItemStack(Items.CHEST);
      icon.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(poolId));
      icon.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(loreLines)));

      GooeyButton button = GooeyButton.builder()
          .display(icon)
          .onClick(action -> RandomRewardMenuRegistry.openRewardPoolPreviewMenu(player, poolId, items))
          .build();
      buttons.add(button);
    }

    ChestTemplate template = ChestTemplate.builder(6)
        .rectangle(0, 0, 5, 9, new PlaceholderButton())
        .set(45, UIUtils.getLinkedPageButton(CobbleUtils.language.getItemPrevious(), LinkType.Previous))
        .set(49, UIUtils.getCloseButton(action -> UIManager.closeUI(action.getPlayer())))
        .set(53, UIUtils.getLinkedPageButton(CobbleUtils.language.getItemNext(), LinkType.Next))
        .build();

    LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
        template,
        buttons,
        LinkedPage.builder().title(AdventureTranslator.toNative(CobbleRandomReward.language.getTitlePoolsMenu()))
    );
    if (CobbleUtils.server != null) {
      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
    } else {
      UIManager.openUIForcefully(player, page);
    }
  }

  @Override
  public void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId) {
    if (player == null || poolId == null) return;
    if (CobbleRandomReward.rewardsConfig == null || CobbleRandomReward.rewardsConfig.getRewards() == null) return;

    List<ItemChance> rewards = CobbleRandomReward.rewardsConfig.getRewards().get(poolId);
    if (rewards == null) {
      PlayerUtils.sendMessage(
          player,
          CobbleRandomReward.language.getInvalidType(),
          CobbleRandomReward.language.getPrefix(),
          TypeMessage.CHAT
      );
      return;
    }
    openRewardPoolPreviewMenu(player, poolId, rewards);
  }

  @Override
  public void openRewardPoolPreviewMenu(ServerPlayerEntity player, String poolId, List<ItemChance> rewards) {
    if (player == null || poolId == null || rewards == null) return;

    List<GooeyButton> buttons = ItemChance.getButtons(rewards);
    ChestTemplate template = ChestTemplate.builder(6)
        .rectangle(0, 0, 5, 9, new PlaceholderButton())
        .set(45, UIUtils.getLinkedPageButton(CobbleUtils.language.getItemPrevious(), LinkType.Previous))
        .set(49, UIUtils.getCloseButton(action -> RandomRewardMenuRegistry.openMainMenu(action.getPlayer())))
        .set(53, UIUtils.getLinkedPageButton(CobbleUtils.language.getItemNext(), LinkType.Next))
        .build();

    String title = CobbleRandomReward.language.getTitlePoolPreviewMenu()
        .replace("%type%", poolId);

    LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
        template,
        new ArrayList<>(buttons),
        LinkedPage.builder().title(AdventureTranslator.toNative(title))
    );
    if (CobbleUtils.server != null) {
      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
    } else {
      UIManager.openUIForcefully(player, page);
    }
  }
}
