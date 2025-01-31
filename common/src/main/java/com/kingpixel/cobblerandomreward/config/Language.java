package com.kingpixel.cobblerandomreward.config;

import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 30/01/2025 23:47
 */
@Getter
@Setter
public class Language {
  private String prefix;
  private String reload;
  private String invalidType;

  public Language() {
    this.prefix = "&7[&6CobbleRandomReward&7] ";
    this.reload = "&aLanguage reloaded";
    this.invalidType = "&cInvalid type";
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(
      CobbleRandomReward.PATH_LANGUAGE, CobbleRandomReward.config.getLang() + ".json", call -> {
        CobbleRandomReward.language = Utils.newGson().fromJson(call, Language.class);

        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
          CobbleRandomReward.PATH_LANGUAGE, CobbleRandomReward.config.getLang() + ".json", Utils.newGson().toJson(CobbleRandomReward.language)
        );
        if (futureWrite.join()) {
          CobbleUtils.LOGGER.info(CobbleRandomReward.MOD_ID, "Language file created");
        } else {
          CobbleUtils.LOGGER.error("Error creating language file");
        }
      }
    );

    if (futureRead.join()) {
      CobbleUtils.LOGGER.info("Language file loaded");
    } else {
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleRandomReward.PATH_LANGUAGE, CobbleRandomReward.config.getLang() + ".json", Utils.newGson().toJson(CobbleRandomReward.language)
      );
      if (futureWrite.join()) {
        CobbleUtils.LOGGER.info("Language file created");
      } else {
        CobbleUtils.LOGGER.error("Error creating language file");
      }
    }
  }
}
