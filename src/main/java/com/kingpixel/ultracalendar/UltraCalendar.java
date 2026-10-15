package com.kingpixel.ultracalendar;

import com.google.gson.JsonSyntaxException;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.kingpixel.ultracalendar.config.Config;
import com.kingpixel.ultracalendar.config.Lang;
import com.kingpixel.ultracalendar.database.DatabaseClient;
import com.kingpixel.ultracalendar.database.DatabaseClientFactory;
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import com.kingpixel.ultracalendar.ui.DailyRewardUI;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UltraCalendar implements ModInitializer {
  public static final String MOD_ID = "ultracalendar";
  public static final String MOD_NAME = "UltraCalendar";
  public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

  public static DatabaseClient database;
  public static Config config = new Config();
  public static Lang language = new Lang();

  public static final AsyncContext ASYNC = UtilsAsync.createContext(MOD_ID, MOD_NAME);

  private static ScheduledFuture<?> taskAlerts;
  private static ScheduledFuture<?> taskAutoSave;
  private static final AtomicBoolean serverStopping = new AtomicBoolean(false);

  @Getter
  private static final Path path = CobbleUtils.getPath().resolve(MOD_ID);

  @Override
  public void onInitialize() {
    events();
  }

  public static void load() {
    files();
    startTasks();
  }

  private static void files() {
    UtilsFile.registerAdapter(DurationValue.class, DurationValue.INSTANCE);
    config.init();
    language.init();
    DatabaseClientFactory.createDatabaseClient(config.getDatabase());
  }

  private static void events() {
    load();
    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> CommandTree.register(dispatcher, registry));
    LifecycleEvent.SERVER_STOPPING.register(server -> handleServerStopping());
    CobbleUtilsEvents.PLAY_TIME_EVENT.subscribe(UltraCalendar::handlePlayTime);
    PlayerEvent.PLAYER_JOIN.register(UltraCalendar::handlePlayerJoin);
    PlayerEvent.PLAYER_QUIT.register(UltraCalendar::handlePlayerQuit);
  }

  private static void handleServerStopping() {
    serverStopping.set(true);
    stopTasks();
    if (database != null) {
      database.disconnect();
    }
  }

  private static void handlePlayTime(@NotNull ServerPlayerEntity player) {
    if (!config.isActive() || serverStopping.get()) return;
    User user = database.getUserDirect(player);
    if (user == null) return;

    boolean canClaimBefore = user.canClaimNextReward();
    user.addPlayTime(1);
    boolean canClaimAfter = user.canClaimNextReward();
    if (!canClaimBefore && canClaimAfter) {
      sendPlayTimeCompletedAlert(player, user);
    }
  }

  private static void handlePlayerJoin(@NotNull ServerPlayerEntity player) {
    database.findUser(player)
      .handle((user, throwable) -> {
        if (throwable != null) {
          if (throwable.getCause() instanceof JsonSyntaxException) {
            return new User(player);
          }
          LOGGER.error("Error reading user data on join for: {}", player.getGameProfile().getName(), throwable);
          return null;
        }
        return user != null ? user : new User(player);
      })
      .thenAccept(user -> {
        if (user == null) return;
        DatabaseClient.USERS.put(player.getUuid(), user);
        user.onJoin(player);
        sendAlert(player, user);
      });
  }

  private static void handlePlayerQuit(@NotNull ServerPlayerEntity player) {
    if (serverStopping.get() || (CobbleUtils.server != null && CobbleUtils.server.isStopping())) {
      return;
    }
    User user = database.getUserDirect(player);
    if (user == null) return;

    user.setLastJoin(User.getTodayEpoch());
    user.markDirty();
    user.save().whenComplete((v, ex) -> {
      if (ex != null) {
        LOGGER.error("Error saving user data on disconnect for: {}", player.getGameProfile().getName(), ex);
      }
      DatabaseClient.USERS.invalidate(player.getUuid());
    });
  }

  public static synchronized void startTasks() {
    stopTasks();

    taskAutoSave = ASYNC.scheduleAtFixedRateWithFuture(() -> {
      if (serverStopping.get() || database == null) return;
      database.saveAll();
    }, 1, 1, TimeUnit.MINUTES);

    int alertInterval = config.getCheckReward();
    if (alertInterval > 0) {
      taskAlerts = ASYNC.scheduleAtFixedRateWithFuture(
        UltraCalendar::runPeriodicAlerts,
        alertInterval,
        alertInterval,
        TimeUnit.MINUTES
      );
    }
  }

  public static synchronized void stopTasks() {
    if (taskAutoSave != null && !taskAutoSave.isCancelled()) {
      taskAutoSave.cancel(false);
      taskAutoSave = null;
    }
    if (taskAlerts != null && !taskAlerts.isCancelled()) {
      taskAlerts.cancel(false);
      taskAlerts = null;
    }
  }

  private static void runPeriodicAlerts() {
    if (serverStopping.get() || CobbleUtils.server == null || CobbleUtils.server.isStopping() || !config.isActive()) {
      return;
    }
    CobbleUtils.server.execute(() -> {
      if (serverStopping.get() || CobbleUtils.server == null || CobbleUtils.server.isStopping() || !config.isActive()) {
        return;
      }
      for (ServerPlayerEntity player : CobbleUtils.server.getPlayerManager().getPlayerList()) {
        if (player.isDisconnected()) continue;
        User user = database.getUserDirect(player);
        if (user != null) {
          sendAlert(player, user);
        }
      }
    });
  }

  public static void sendAlert(@NotNull ServerPlayerEntity player, @NotNull User user) {
    if (user.canClaimNextReward()) {
      PlayerUtils.sendMessage(
        player,
        language.getMessageCanClaim(),
        language.getPrefix(),
        TypeMessage.CHAT
      );
    } else if (user.canClaim() && config.isSendPlayTimeAlertOnJoin()) {
      Rewards nextReward = config.getReward(user.getDay() + 1);
      if (nextReward != null) {
        long requiredSeconds = nextReward.getRequiredTime().toSeconds();
        long currentPlayTime = nextReward.isDailyTime() ? user.getDailyPlayTime() : user.getTotalPlayTime();
        if (currentPlayTime < requiredSeconds) {
          long remainingSeconds = Math.max(0, requiredSeconds - currentPlayTime);
          String msg = language.getMessageJoinNeedPlayTime()
            .replace("%prefix%", language.getPrefix())
            .replace("%day%", String.valueOf(user.getDay() + 1))
            .replace("%required%", DailyRewardUI.formatDuration(requiredSeconds))
            .replace("%remaining%", DailyRewardUI.formatDuration(remainingSeconds))
            .replace("%playtime%", DailyRewardUI.formatDuration(currentPlayTime));
          PlayerUtils.sendMessage(player, msg, language.getPrefix(), TypeMessage.CHAT);
        }
      }
    }
  }

  public static void sendAlert(@NotNull User user) {
    if (CobbleUtils.server == null || CobbleUtils.server.isStopping() || serverStopping.get()) return;
    CobbleUtils.server.execute(() -> {
      ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(user.getUuid());
      if (player != null && !player.isDisconnected()) {
        sendAlert(player, user);
      }
    });
  }

  private static void sendPlayTimeCompletedAlert(@NotNull ServerPlayerEntity player, @NotNull User user) {
    if (CobbleUtils.server == null || CobbleUtils.server.isStopping() || serverStopping.get()) return;
    CobbleUtils.server.execute(() -> {
      if (player.isDisconnected()) return;
      int nextDay = user.getDay() + 1;
      Rewards reward = config.getReward(nextDay);
      long requiredSeconds = reward != null ? reward.getRequiredTime().toSeconds() : 0;
      long currentPlayTime = reward != null && !reward.isDailyTime() ? user.getTotalPlayTime() : user.getDailyPlayTime();

      String message = language.getMessagePlayTimeCompleted()
        .replace("%prefix%", language.getPrefix())
        .replace("%day%", String.valueOf(nextDay))
        .replace("%required%", DailyRewardUI.formatDuration(requiredSeconds))
        .replace("%playtime%", DailyRewardUI.formatDuration(currentPlayTime));

      PlayerUtils.sendMessage(
        player,
        message,
        language.getPrefix(),
        TypeMessage.CHAT
      );
    });
  }
}
