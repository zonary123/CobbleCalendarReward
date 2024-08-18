package com.kingpixel.cobblecalendarreward;

import com.kingpixel.cobblecalendarreward.command.CommandTree;
import com.kingpixel.cobblecalendarreward.config.Config;
import com.kingpixel.cobblecalendarreward.config.Lang;
import com.kingpixel.cobblecalendarreward.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendarreward.managers.DailyRewardsManager;
import com.kingpixel.cobblecalendarreward.utils.UtilsLogger;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

public class CobbleCalendarReward {
  public static final String MOD_ID = "cobblecalendarreward";
  public static final String MOD_NAME = "CobbleCalendarReward";
  public static final String PATH = "/config/cobblecalendarreward";
  public static final String PATH_LANG = "/config/cobblecalendarreward/lang/";
  public static final UtilsLogger LOGGER = new UtilsLogger();
  public static MinecraftServer server;

  // Config and Lang
  public static Config config = new Config();
  public static Lang language = new Lang();

  // Manager
  public static DailyRewardsManager manager = new DailyRewardsManager();

  // Tasks
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static final List<ScheduledFuture<?>> scheduledTasks = new CopyOnWriteArrayList<>();

  public static void init() {
    events();
  }

  public static void load() {
    files();
    sign();
    tasks();
  }


  private static void files() {
    config.init();
    language.init();
    DatabaseClientFactory.createDatabaseClient(
      config.getDatabase()
    );
  }

  private static void sign() {
    LOGGER.info("§e+-------------------------------+");
    LOGGER.info("§e| §6CobbleCalendarReward");
    LOGGER.info("§e+-------------------------------+");
    LOGGER.info("§e| §6Version: §e" + "1.0.0");
    LOGGER.info("§e| §6Author: §eZonary123");
    LOGGER.info("§e| §6Website: §9https://github.com/Zonary123/CobbleCalendarReward");
    LOGGER.info("§e| §6Discord: §9https://discord.com/invite/fKNc7FnXpa");
    LOGGER.info("§e| §6Support: §9https://github.com/Zonary123/CobbleCalendarReward/issues");
    LOGGER.info("§e| &dDonate: §9https://ko-fi.com/zonary123");
    LOGGER.info("§e+-------------------------------+");
  }

  private static void events() {
    files();

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> CommandTree.register(dispatcher, registry));

    LifecycleEvent.SERVER_STARTED.register(server -> load());

    LifecycleEvent.SERVER_STOPPING.register(server -> {
      scheduledTasks.forEach(task -> task.cancel(true));
      scheduledTasks.clear();
      LOGGER.info("CobbleCalendarReward has been stopped.");
    });

    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> server = level.getServer());

    PlayerEvent.PLAYER_JOIN.register(player -> {
      manager.init(player);
      DatabaseClientFactory.databaseClient.updateUserInfoLastJoin(player, LocalDate.now());
      sendAlert(player);
    });

    PlayerEvent.PLAYER_QUIT.register(player -> {
      DatabaseClientFactory.databaseClient.updateUserInfoLastJoin(player, LocalDate.now());
    });
  }


  private static void tasks() {
    for (ScheduledFuture<?> task : scheduledTasks) {
      task.cancel(false);
    }
    scheduledTasks.clear();

    ScheduledFuture<?> alertreward =
      scheduler.scheduleAtFixedRate(() ->
          server.getPlayerManager().getPlayerList().forEach(CobbleCalendarReward::sendAlert),
        0, CobbleCalendarReward.config.getCheckReward(), TimeUnit.MINUTES);


    scheduledTasks.add(alertreward);
  }

  private static void sendAlert(ServerPlayerEntity player) {
    if (DatabaseClientFactory.databaseClient.canClaim(player)) {
      player.sendMessage(
        AdventureTranslator.toNative(
          language.getMessageCanClaim()
            .replace("%prefix%", language.getPrefix())
        )
      );
    }
  }
}
