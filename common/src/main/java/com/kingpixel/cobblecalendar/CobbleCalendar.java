package com.kingpixel.cobblecalendar;

import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobblecalendar.command.CommandTree;
import com.kingpixel.cobblecalendar.config.Config;
import com.kingpixel.cobblecalendar.config.Lang;
import com.kingpixel.cobblecalendar.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendar.models.UserInfo;
import com.kingpixel.cobblecalendar.utils.UtilsLogger;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CobbleCalendar {
  public static final String MOD_ID = "cobblecalendar";
  public static final String MOD_NAME = "CobbleCalendar";
  public static final String PATH = "/config/cobblecalendar";
  public static final String PATH_LANG = "/config/cobblecalendar/lang/";
  public static final String PATH_REWARDS = "/config/cobblecalendar/rewards/";
  public static final UtilsLogger LOGGER = new UtilsLogger();
  public static MinecraftServer server;
  public static Map<UUID, UserInfo> userInfoMap = new HashMap<>();
  // Config and Lang
  public static Config config = new Config();
  public static Lang language = new Lang();
  // Tasks
  private static Task alertReward;

  public static ExecutorService EXECUTOR_CALENDAR = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
    .setDaemon(true)
    .setNameFormat("CobbleCalendar-Executor-%d")
    .build());

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
    LOGGER.info("§e| §6CobbleCalendar");
    LOGGER.info("§e+-------------------------------+");
    LOGGER.info("§e| §6Version: §e" + "1.0.9");
    LOGGER.info("§e| §6Author: §eZonary123");
    LOGGER.info("§e| §6Website: §9https://github.com/Zonary123/CobbleCalendar");
    LOGGER.info("§e| §6Discord: §9https://discord.com/invite/fKNc7FnXpa");
    LOGGER.info("§e| §6Support: §9https://github.com/Zonary123/CobbleCalendar/issues");
    LOGGER.info("§e| &dDonate: §9https://ko-fi.com/zonary123");
    LOGGER.info("§e+-------------------------------+");
  }

  private static void events() {
    files();

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> CommandTree.register(dispatcher, registry));

    LifecycleEvent.SERVER_STARTED.register(server -> load());

    LifecycleEvent.SERVER_STOPPING.register(server -> {
      LOGGER.info("CobbleCalendar has been stopped.");
    });

    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> server = level.getServer());

    PlayerEvent.PLAYER_JOIN.register(player -> {
      CompletableFuture.runAsync(() -> {
          UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
          if (userInfo == null) return;
          userInfo.computeDay(player);
          DatabaseClientFactory.databaseClient.updateUserInfo(player, userInfo);
          sendAlert(player);
        }, EXECUTOR_CALENDAR)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        });
    });

    PlayerEvent.PLAYER_QUIT.register(player -> {
      userInfoMap.remove(player.getUuid());
    });
  }


  private static void tasks() {
    if (alertReward != null) alertReward.setExpired();

    long interval = 20L * 60 * config.getCheckReward();
    alertReward = Task.builder()
      .execute(() -> CompletableFuture.runAsync(() -> {
          var players = server.getPlayerManager().getPlayerList();
          players.forEach(CobbleCalendar::sendAlert);
        }, EXECUTOR_CALENDAR)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        }))
      .interval(interval)
      .infinite()
      .build();

  }

  private static void sendAlert(ServerPlayerEntity player) {
    UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
    if (userInfo == null) {
      CobbleCalendar.LOGGER.warn("UserInfo is null for player: " + player.getGameProfile().getName());
      return;
    }
    if (userInfo.canClaim()) {
      PlayerUtils.sendMessage(
        player,
        language.getMessageCanClaim(),
        language.getPrefix(),
        TypeMessage.CHAT
      );
    }
  }
}
