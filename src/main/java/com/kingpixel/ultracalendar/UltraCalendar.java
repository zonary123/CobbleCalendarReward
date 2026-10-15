package com.kingpixel.ultracalendar;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
import com.kingpixel.cobbleutils.util.redis.RedisManager;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisHandler;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UltraCalendar implements ModInitializer {
  public static final String MOD_ID = "ultracalendar";
  public static final String MOD_NAME = "UltraCalendar";
  public static final String PATH = "/config/ultracalendar";
  public static final String PATH_LANG = "/config/ultracalendar/lang/";
  public static final String PATH_REWARDS = "/config/ultracalendar/rewards/";
  public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
  public static DatabaseClient database;
  // Config and Lang
  public static Config config = new Config();
  public static Lang language = new Lang();

  public static final AsyncContext ASYNC = UtilsAsync.createContext(MOD_ID, MOD_NAME);

  @Override
  public void onInitialize() {
    events();
  }


  public static void load() {
    files();
  }


  private static void files() {
    UtilsFile.registerAdapter(DurationValue.class, DurationValue.INSTANCE);
    config.init();
    language.init();
    DatabaseClientFactory.createDatabaseClient(config.getDatabase());
  }

  private static void events() {
    load();
    tasks();
    registerRedis();
    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> CommandTree.register(dispatcher, registry));

    LifecycleEvent.SERVER_STOPPING.register(server -> database.disconnect());

    CobbleUtilsEvents.PLAY_TIME_EVENT.subscribe(player -> {
      if (!config.isActive()) return;
      User user = database.getUser(player);
      if (user != null) {
        boolean canClaimBefore = user.canClaimNextReward();
        user.addPlayTime(1);
        boolean canClaimAfter = user.canClaimNextReward();
        if (!canClaimBefore && canClaimAfter) {
          sendPlayTimeCompletedAlert(player, user);
        }
      }
    });

    PlayerEvent.PLAYER_JOIN.register(player ->
      database.findUser(player)
        .handle((user, throwable) -> {
          if (throwable != null) {
            Throwable cause = throwable.getCause();
            if (cause instanceof JsonSyntaxException) {
              return new User(player);
            }
            throwable.printStackTrace();
            return null;
          }
          return user != null ? user : new User(player);
        })
        .thenAccept(user -> {
          if (user == null) return;
          DatabaseClient.USERS.put(player.getUuid(), user);
          user.onJoin(player);
          sendAlert(user);
        })
    );

    PlayerEvent.PLAYER_QUIT.register(player -> {
      User user = database.getUser(player);
      if (user == null) return;
      user.setLastJoin(User.getTodayEpoch());
      user.markDirty();
      try {
        user.save().get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        LOGGER.error("Error saving user data on disconnect for: " + player.getGameProfile().getName(), e);
      } finally {
        DatabaseClient.USERS.invalidate(player.getUuid());
        publishRedisInvalidation(player.getUuid());
      }
    });
  }


  private static void tasks() {
    // Alert players
    ASYNC.scheduleAtFixedRate(() -> DatabaseClient.USERS.asMap().values()
      .forEach(UltraCalendar::sendAlert), 0, 15, TimeUnit.MINUTES);
    // Save all players
    ASYNC.scheduleAtFixedRate(() -> {
      if (database == null) return;
      database.saveAll();
    }, 1, 1, TimeUnit.MINUTES);
  }

  private static void sendAlert(@NotNull User user) {
    CobbleUtils.server.execute(() -> {
      ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(user.getUuid());
      if (player == null) return;

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
    });
  }

  private static void sendPlayTimeCompletedAlert(@NotNull ServerPlayerEntity player, @NotNull User user) {
    CobbleUtils.server.execute(() -> {
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

  public static void publishRedisInvalidation(@NotNull UUID uuid) {
    try {
      if (CobbleUtils.config != null && CobbleUtils.config.isRedisMessaging()) {
        RedisManager mgr = CobbleUtils.redisManager;
        if (mgr != null && mgr.getConnected() != null && mgr.getConnected().get()) {
          JsonObject json = new JsonObject();
          json.addProperty("action", "invalidate");
          json.addProperty("uuid", uuid.toString());
          mgr.publish("ultracalendar:user_sync", json);
        }
      }
    } catch (Throwable ignored) {
      // Redis messaging disabled or unavailable
    }
  }

  private static void registerRedis() {
    try {
      if (CobbleUtils.config != null && CobbleUtils.config.isRedisMessaging()) {
        RedisManager mgr = CobbleUtils.redisManager;
        if (mgr != null && mgr.getConnected() != null && mgr.getConnected().get()) {
          mgr.registerHandler(new RedisHandler() {
            @Override
            public String getIdentifier() {
              return "ultracalendar:user_sync";
            }

            @Override
            public void handle(JsonObject json) {
              try {
                if (json != null && json.has("uuid")) {
                  UUID uuid = UUID.fromString(json.get("uuid").getAsString());
                  if (CobbleUtils.server != null) {
                    ServerPlayerEntity p = CobbleUtils.server.getPlayerManager().getPlayer(uuid);
                    if (p != null) {
                      database.findUser(uuid).thenAccept(refreshed -> {
                        if (refreshed != null) {
                          DatabaseClient.USERS.put(uuid, refreshed);
                        }
                      });
                      return;
                    }
                  }
                  DatabaseClient.USERS.invalidate(uuid);
                }
              } catch (Exception ignored) {
                // Ignore malformed message
              }
            }
          });
        }
      }
    } catch (Throwable e) {
      LOGGER.warn("Failed to register Redis handler for UltraCalendar", e);
    }
  }

  @Getter
  private static final Path path = CobbleUtils.getPath().resolve(MOD_ID);

}

