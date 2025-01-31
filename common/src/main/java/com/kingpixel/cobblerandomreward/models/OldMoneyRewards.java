package com.kingpixel.cobblerandomreward.models;

import com.kingpixel.cobbleutils.Model.ItemChance;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Carlos Varas Alonso - 31/01/2025 6:46
 */
@Getter
public class OldMoneyRewards {
  private Map<String, List<ItemChance>> randomMoney;
}
