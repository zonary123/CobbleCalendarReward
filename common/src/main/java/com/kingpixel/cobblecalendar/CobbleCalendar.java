package com.kingpixel.cobblecalendar;

import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.kingpixel.cobblecalendar.command.CommandTree;
import com.kingpixel.cobblecalendar.config.Config;
import com.kingpixel.cobblecalendar.config.Lang;
import com.kingpixel.cobblecalendar.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendar.managers.DailyRewardsManager;
import com.kingpixel.cobblecalendar.models.UserInfo;
import com.kingpixel.cobblecalendar.utils.UtilsLogger;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class CobbleCalendar {
  public static final String MOD_ID = "cobblecalendar";
  public static final String MOD_NAME = "CobbleCalendar";
  public static final String PATH = "/config/cobblecalendar";
  public static final String PATH_LANG = "/config/cobblecalendar/lang/";
  public static final String PATH_REWARDS = "/config/cobblecalendar/rewards/";
  public static final UtilsLogger LOGGER = new UtilsLogger();
  public static MinecraftServer server;

  // Config and Lang
  public static Config config = new Config();
  public static Lang language = new Lang();

  // Manager
  public static DailyRewardsManager manager = new DailyRewardsManager();

  // Tasks
  private static Task alertReward;

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
    LOGGER.info("§e| §6Version: §e" + "1.0.8");
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
      UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
      userInfo.computeDay();
      DatabaseClientFactory.databaseClient.updateUserInfo(userInfo);
      sendAlert(player);
    });

    PlayerEvent.PLAYER_QUIT.register(player -> {
      UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
      userInfo.computeDay();
      DatabaseClientFactory.databaseClient.updateUserInfo(userInfo);
    });
  }


  private static void tasks() {
    if (alertReward != null) alertReward.setExpired();

    long interval = 20L * 60 * config.getCheckReward();
    alertReward = Task.builder()
      .execute(() -> server.getPlayerManager().getPlayerList().forEach(CobbleCalendar::sendAlert))
      .interval(interval)
      .infinite()
      .build();

  }

  private static void sendAlert(ServerPlayerEntity player) {
    UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
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
