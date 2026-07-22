package com.kingpixel.ultracalendar;

import com.google.gson.JsonSyntaxException;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
import com.kingpixel.ultracalendar.command.CommandTree;
import com.kingpixel.ultracalendar.config.Config;
import com.kingpixel.ultracalendar.config.Lang;
import com.kingpixel.ultracalendar.database.DatabaseClient;
import com.kingpixel.ultracalendar.database.DatabaseClientFactory;
import com.kingpixel.ultracalendar.models.User;
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
import java.time.LocalDate;
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
    config.init();
    language.init();
    DatabaseClientFactory.createDatabaseClient(config.getDatabase());
  }

  private static void events() {
    load();
    tasks();
    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> CommandTree.register(dispatcher, registry));

    LifecycleEvent.SERVER_STOPPING.register(server -> database.disconnect());

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
      user.setLastJoin(LocalDate.now().toEpochDay());
      user.markDirty();
      user.save().thenRun(() ->
        DatabaseClient.USERS.invalidate(player.getUuid())
      );
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
    if (user.canClaim()) {
      CobbleUtils.server.execute(() -> {
        ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(user.getUuid());
        if (player == null) return;
        PlayerUtils.sendMessage(
          player,
          language.getMessageCanClaim(),
          language.getPrefix(),
          TypeMessage.CHAT
        );
      });
    }
  }

  @Getter
  private static final Path path = CobbleUtils.getPath().resolve(MOD_ID);

}

