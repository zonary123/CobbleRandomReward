package com.kingpixel.cobblerandomreward.config;

import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 30/01/2025 23:47
 */
@Getter
@Setter
public class Config {
  private boolean debug;
  private String lang;
  private List<String> commands;

  public Config() {
    this.debug = false;
    this.lang = "en";
    this.commands = List.of(
      "randomreward"
    );
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(
      CobbleRandomReward.PATH, "config.json", call -> {
        CobbleRandomReward.config = Utils.newGson().fromJson(call, Config.class);

        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
          CobbleRandomReward.PATH, "config.json", Utils.newGson().toJson(CobbleRandomReward.config)
        );
        if (futureWrite.join()) {
          CobbleUtils.LOGGER.info(CobbleRandomReward.MOD_ID, "Config file created");
        } else {
          CobbleUtils.LOGGER.error("Error creating config file");
        }
      }
    );

    if (futureRead.join()) {
      CobbleUtils.LOGGER.info("Config file loaded");
    } else {
      CobbleRandomReward.config = this;
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleRandomReward.PATH, "config.json", Utils.newGson().toJson(this)
      );
      if (futureWrite.join()) {
        CobbleUtils.LOGGER.info("Config file created");
      } else {
        CobbleUtils.LOGGER.error("Error creating config file");
      }
    }
  }
}
