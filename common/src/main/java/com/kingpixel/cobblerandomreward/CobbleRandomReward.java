package com.kingpixel.cobblerandomreward;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobblerandomreward.commands.CommandTree;
import com.kingpixel.cobblerandomreward.config.Config;
import com.kingpixel.cobblerandomreward.config.Language;
import com.kingpixel.cobblerandomreward.config.RewardsConfig;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import java.util.concurrent.TimeUnit;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Carlos Varas Alonso - 23/07/2024 9:24
 */
public class CobbleRandomReward {
  public static final String MOD_ID = "cobblerandomreward";
  public static final String PATH = "/config/cobblerandomreward/";
  public static final String PATH_LANGUAGE = PATH + "lang/";
  public static final String PATH_REWARDS = PATH + "rewards/";
  public static Config config = new Config();
  public static Language language = new Language();
  public static RewardsConfig rewardsConfig = new RewardsConfig();
  private static final AsyncContext asyncContext = new AsyncContext(MOD_ID, 1, 4, 200, 30, TimeUnit.SECONDS);

  public static AsyncContext getAsyncContext() {
    return asyncContext;
  }

  public static ExecutorService EXECUTOR = asyncContext.getExecutor();


  public static void init() {
    load();
    events();
  }

  public static void load() {
    files();
  }

  private static void files() {
    config.init();
    language.init();
    rewardsConfig.init();
  }

  private static void events() {
    files();

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CommandTree.register(dispatcher, registry);
    });

    LifecycleEvent.SERVER_STARTED.register(instance -> {
      load();
    });

    LifecycleEvent.SERVER_STOPPING.register(instance -> {

    });


  }

}
