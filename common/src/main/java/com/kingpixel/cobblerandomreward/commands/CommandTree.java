package com.kingpixel.cobblerandomreward.commands;

import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import com.kingpixel.cobblerandomreward.api.ui.RandomRewardMenuRegistry;
import com.kingpixel.cobblerandomreward.application.RandomRewardActionService;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:12
 */
public class CommandTree {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    for (String command : CobbleRandomReward.config.getCommands()) {
      var base = CommandManager.literal(command)
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobblerandomreward.admin"),
          4))
        .executes(context -> {
          if (context.getSource().isExecutedByPlayer()) {
            RandomRewardMenuRegistry.openMainMenu(context.getSource().getPlayer());
          }
          return 1;
        });

      dispatcher.register(base
        .then(
          CommandManager.literal("reload")
            .executes(context -> {
              CobbleRandomReward.load();
              if (context.getSource().isExecutedByPlayer()) {
                PlayerUtils.sendMessage(
                  context.getSource().getPlayer(),
                  CobbleRandomReward.language.getReload(),
                  CobbleRandomReward.language.getPrefix(),
                  TypeMessage.CHAT
                );
              } else {
                CobbleUtils.LOGGER.info(CobbleRandomReward.MOD_ID, CobbleRandomReward.language.getReload());
              }
              return 1;
            })
        )
        .then(
          CommandManager.literal("migrate")
            .executes(context -> {
              RandomRewardActionService.getInstance().migrateLegacyConfigs();
              return 1;
            })
        )
        .then(
          CommandManager.literal("preview")
            .then(
              CommandManager.argument("type", StringArgumentType.string())
                .suggests((context, builder) -> {
                  for (String type : CobbleRandomReward.rewardsConfig.getRewards().keySet()) {
                    builder.suggest(type);
                  }
                  return builder.buildFuture();
                })
                .executes(context -> {
                  if (context.getSource().isExecutedByPlayer()) {
                    String type = StringArgumentType.getString(context, "type");
                    RandomRewardMenuRegistry.openRewardPoolPreviewMenu(context.getSource().getPlayer(), type);
                  }
                  return 1;
                })
            )
        )
        .then(
          CommandManager.literal("give")
            .then(
              CommandManager.argument("player", EntityArgumentType.player())
                .then(
                  CommandManager.argument("amountRewards", IntegerArgumentType.integer())
                    .then(
                      CommandManager.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                          for (String type : CobbleRandomReward.rewardsConfig.getRewards().keySet()) {
                            builder.suggest(type);
                          }
                          return builder.buildFuture();
                        })
                        .executes(context -> {
                          ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                          int amount = IntegerArgumentType.getInteger(context, "amountRewards");
                          String type = StringArgumentType.getString(context, "type");
                          give(player, amount, type);
                          return 1;
                        })
                    )
                )
            )
        )
      );
    }
  }

  private static void give(ServerPlayerEntity player, int amountRewards, String type) {
    RandomRewardActionService.getInstance().rollAndGiveRewards(player, type, amountRewards, true);
  }
}
