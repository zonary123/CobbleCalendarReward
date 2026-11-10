package com.kingpixel.ultracalendar.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ItemUtils;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DailyRewardUI {

  private record RewardStatus(boolean isClaimed, boolean isAvailable, boolean hasPlayTime) {}

  private DailyRewardUI() {}

  public static CompletableFuture<Void> open(@NotNull ServerPlayerEntity player) {
    return UltraCalendar.database.getUser(player).thenAcceptAsync(user -> {
      if (user == null || !UltraCalendar.config.isActive()) return;

      int rows = UltraCalendar.config.getRows();
      ChestTemplate template = ChestTemplate.builder(rows).build();
      new Rectangle(rows).apply(template);

      List<Button> buttons = populateRewardButtons(player, user, template);
      setupDecorationsAndControls(template, rows);

      GooeyPage page = buildPage(template, buttons);

      if (CobbleUtils.server != null && !CobbleUtils.server.isStopping()) {
        CobbleUtils.server.execute(() -> {
          if (player.isDisconnected()) return;
          UIManager.openUIForcefully(player, page);
        });
      }
    }, UltraCalendar.ASYNC::runAsync).exceptionally(throwable -> {
      UltraCalendar.LOGGER.error("Failed to open DailyRewardUI for {}", player.getGameProfile().getName(), throwable);
      return null;
    });
  }

  private static List<Button> populateRewardButtons(
    ServerPlayerEntity player,
    User user,
    ChestTemplate template
  ) {
    List<Button> buttons = new ArrayList<>();
    for (Rewards reward : UltraCalendar.config.getRewards()) {
      if (reward == null || !UltraCalendar.config.isActive()) continue;

      Button button = createRewardButton(player, user, reward);
      if (button == null) continue;

      if (UltraCalendar.config.isAutoPlace()) {
        buttons.add(button);
      } else {
        template.set(reward.getSlot(), button);
      }
    }
    return buttons;
  }

  private static @Nullable Button createRewardButton(
    ServerPlayerEntity player,
    User user,
    Rewards reward
  ) {
    int day = reward.getDay();
    boolean isClaimed = user.isClaimed(day);
    boolean isAvailable = user.isAvailableDay(day);

    long requiredSeconds = reward.getRequiredTime() != null ? reward.getRequiredTime().toSeconds() : 0;
    long currentPlayTime = reward.isDailyTime() ? user.getDailyPlayTime() : user.getTotalPlayTime();
    boolean hasPlayTime = currentPlayTime >= requiredSeconds;

    RewardStatus status = new RewardStatus(isClaimed, isAvailable, hasPlayTime);
    boolean canClaim = isAvailable && !isClaimed && hasPlayTime;

    ItemModel model = resolveItemModel(reward, isClaimed, canClaim);
    if (model == null) return null;

    long remainingSeconds = Math.max(0, requiredSeconds - currentPlayTime);
    Map<String, String> placeholders = buildPlaceholders(day, requiredSeconds, remainingSeconds, currentPlayTime, user);

    List<String> lore = model.getLore() != null ? applyLorePlaceholders(model.getLore(), placeholders) : null;
    String rawName = model.getDisplayname() != null ? model.getDisplayname() : "";
    String displayName = applyPlaceholders(rawName, placeholders);

    return model.getButton(
      Math.min(day, 64),
      displayName,
      lore,
      action -> UltraCalendar.ASYNC.runAsync(() ->
        handleRewardClick(action, player, user, reward, status, placeholders)
      ),
      1,
      TimeUnit.SECONDS,
      1
    );
  }

  private static @Nullable ItemModel resolveItemModel(Rewards reward, boolean isClaimed, boolean canClaim) {
    ItemModel model = reward.getNotClaimed();
    if (isClaimed) {
      model = reward.getClaimed();
    } else if (canClaim) {
      model = reward.getCanClaim();
    }

    if (model == null || model.getItem() == null || model.getItem().isEmpty()) {
      if (isClaimed) {
        model = UltraCalendar.language.getGlobalClaimed();
      } else if (canClaim) {
        model = UltraCalendar.language.getGlobalCanClaim();
      } else {
        model = UltraCalendar.language.getGlobalNotClaimed();
      }
    }
    return model;
  }

  private static void handleRewardClick(
    ButtonAction action,
    ServerPlayerEntity player,
    User user,
    Rewards reward,
    RewardStatus status,
    Map<String, String> placeholders
  ) {
    if (!UltraCalendar.config.isActive()) return;

    switch (action.getClickType()) {
      case SHIFT_RIGHT_CLICK, RIGHT_CLICK -> {
        if (reward.getRewards() != null) {
          reward.getRewards().openMenu(player, t -> {}, close -> {});
        }
      }
      default -> processClaim(action.getPlayer(), player, user, reward, status, placeholders);
    }
  }

  private static void processClaim(
    ServerPlayerEntity clicker,
    ServerPlayerEntity player,
    User user,
    Rewards reward,
    RewardStatus status,
    Map<String, String> placeholders
  ) {
    if (status.isClaimed()) {
      sendFeedback(clicker, UltraCalendar.language.getMessageClaimed(), placeholders);
      return;
    }

    if (!status.isAvailable()) {
      sendFeedback(clicker, UltraCalendar.language.getMessageVerySoon(), placeholders);
      return;
    }

    if (!status.hasPlayTime()) {
      sendFeedback(clicker, UltraCalendar.language.getMessageNeedPlayTime(), placeholders);
      return;
    }

    boolean claimed = user.claim(reward);
    if (!claimed) return;

    user.markDirty();
    user.save();

    if (CobbleUtils.server != null && !CobbleUtils.server.isStopping()) {
      CobbleUtils.server.execute(() -> {
        if (player.isDisconnected()) return;
        if (reward.getRewards() != null) {
          reward.getRewards().giveRewards(player);
        }
        open(player);
      });
    }
  }

  private static void sendFeedback(ServerPlayerEntity player, String template, Map<String, String> placeholders) {
    String formatted = applyPlaceholders(template, placeholders);
    player.sendMessage(AdventureTranslator.toNative(formatted));
  }

  private static void setupDecorationsAndControls(ChestTemplate template, int rows) {
    ItemStack fillItem = ItemUtils.parseItemId(UltraCalendar.language.getFill());
    template.fill(GooeyButton.of(fillItem));

    ItemModel closeModel = UltraCalendar.language.getCloseButton();
    int defaultCloseSlot = (rows * 9) - 5;
    int closeSlot = (closeModel != null && closeModel.getSlot() > 0) ? closeModel.getSlot() : defaultCloseSlot;

    if (closeModel != null && !closeModel.getItem().isEmpty()) {
      GooeyButton closeBtn = GooeyButton.builder()
        .display(closeModel.getItemStack())
        .onClick(action -> UIManager.closeUI(action.getPlayer()))
        .build();
      if (closeModel.getSlot() > 0) {
        closeModel.applyTemplate(template, closeBtn);
      } else {
        template.set(closeSlot, closeBtn);
      }
    } else {
      template.set(closeSlot, UIUtils.getCloseButton(action -> UIManager.closeUI(action.getPlayer())));
    }

    if (UltraCalendar.config.isAutoPlace()) {
      template.set((rows * 9) - 9, UIUtils.getPreviousButton(action -> {}));
      template.set((rows * 9) - 1, UIUtils.getNextButton(action -> {}));
    }
  }

  private static GooeyPage buildPage(ChestTemplate template, List<Button> buttons) {
    String title = UltraCalendar.language.getTitlemenu();
    if (UltraCalendar.config.isAutoPlace()) {
      GooeyPage page = PaginationHelper.createPagesFromPlaceholders(template, buttons, null);
      page.setTitle(AdventureTranslator.toNative(title));
      return page;
    }
    return GooeyPage.builder()
      .title(AdventureTranslator.toNative(title))
      .template(template)
      .build();
  }

  private static Map<String, String> buildPlaceholders(
    int day,
    long requiredSeconds,
    long remainingSeconds,
    long currentPlayTime,
    User user
  ) {
    Map<String, String> map = new HashMap<>();
    map.put("%prefix%", UltraCalendar.language.getPrefix());
    map.put("%day%", String.valueOf(day));
    map.put("%required%", formatDuration(requiredSeconds));
    map.put("%remaining%", formatDuration(remainingSeconds));
    map.put("%playtime%", formatDuration(currentPlayTime));
    map.put("%daily_playtime%", formatDuration(user.getDailyPlayTime()));
    map.put("%total_playtime%", formatDuration(user.getTotalPlayTime()));
    return map;
  }

  private static String applyPlaceholders(String text, Map<String, String> placeholders) {
    if (text == null || text.isEmpty()) return "";
    String result = text;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }
    return result;
  }

  private static List<String> applyLorePlaceholders(List<String> lore, Map<String, String> placeholders) {
    return lore.stream()
      .map(line -> line == null ? "" : applyPlaceholders(line, placeholders))
      .toList();
  }

  public static String formatDuration(long seconds) {
    if (seconds <= 0) return "0s";
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    if (hours > 0) {
      return String.format("%dh %dm %ds", hours, minutes, secs);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, secs);
    } else {
      return String.format("%ds", secs);
    }
  }
}