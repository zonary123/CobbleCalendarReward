package com.kingpixel.ultracalendar.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.RateLimitedButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.kingpixel.cobbleutils.util.Utils;
import com.kingpixel.ultracalendar.UltraCalendar;
import com.kingpixel.ultracalendar.database.DatabaseClient;
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DailyRewardUI {

  public static CompletableFuture<Void> open(@NotNull ServerPlayerEntity player) {
    return UltraCalendar.ASYNC.runAsync(() -> {

      User loadedUser = UltraCalendar.database.getUser(player);
      if (loadedUser == null) {
        try {
          loadedUser = UltraCalendar.database.findUser(player).get(3, TimeUnit.SECONDS);
          if (loadedUser != null) {
            DatabaseClient.USERS.put(player.getUuid(), loadedUser);
          }
        } catch (Exception ignored) {
          // Timeout or interruption reading user
        }
      }
      if (loadedUser == null) return;
      final User user = loadedUser;

      int rows = UltraCalendar.config.getRows();
      ChestTemplate template = ChestTemplate.builder(rows).build();
      new Rectangle(rows).apply(template);

      List<Button> buttons = new ArrayList<>();

      for (Rewards reward : UltraCalendar.config.getRewards()) {
        if (reward == null) continue;
        if (!UltraCalendar.config.isActive()) return;

        int day = reward.getDay();

        boolean isClaimed = user.getDay() >= day;
        boolean isNextDay = user.canClaim() && user.getDay() + 1 == day;
        long requiredSeconds = reward.getRequiredTime() != null ? reward.getRequiredTime().toSeconds() : 0;
        long currentPlayTime = reward.isDailyTime() ? user.getDailyPlayTime() : user.getTotalPlayTime();
        boolean hasPlayTime = currentPlayTime >= requiredSeconds;
        boolean canClaim = isNextDay && hasPlayTime;

        ItemModel model = reward.getNotClaimed();
        if (isClaimed) model = reward.getClaimed();
        if (canClaim) model = reward.getCanClaim();

        if (model == null || model.getItem() == null || model.getItem().isEmpty()) {
          model = isClaimed
            ? UltraCalendar.language.getGlobalClaimed()
            : canClaim
              ? UltraCalendar.language.getGlobalCanClaim()
              : UltraCalendar.language.getGlobalNotClaimed();
        }

        if (model == null) continue;

        long remainingSeconds = Math.max(0, requiredSeconds - currentPlayTime);
        String reqStr = formatDuration(requiredSeconds);
        String remStr = formatDuration(remainingSeconds);
        String playStr = formatDuration(currentPlayTime);
        String dailyPlayStr = formatDuration(user.getDailyPlayTime());
        String totalPlayStr = formatDuration(user.getTotalPlayTime());

        List<String> lore = model.getLore();
        if (lore != null) {
          lore = lore.stream()
            .map(line -> line == null ? "" : line
              .replace("%day%", String.valueOf(day))
              .replace("%required%", reqStr)
              .replace("%remaining%", remStr)
              .replace("%playtime%", playStr)
              .replace("%daily_playtime%", dailyPlayStr)
              .replace("%total_playtime%", totalPlayStr)
            ).toList();
        }

        String rawDisplayName = model.getDisplayname() != null ? model.getDisplayname() : "";
        String displayName = rawDisplayName
          .replace("%day%", String.valueOf(day))
          .replace("%required%", reqStr)
          .replace("%remaining%", remStr)
          .replace("%playtime%", playStr)
          .replace("%daily_playtime%", dailyPlayStr)
          .replace("%total_playtime%", totalPlayStr);

        RateLimitedButton button = model.getButton(
          1,
          displayName,
          lore,
          action -> UltraCalendar.ASYNC.runAsync(() -> {

            if (!UltraCalendar.config.isActive()) return;

            switch (action.getClickType()) {

              case SHIFT_RIGHT_CLICK, RIGHT_CLICK -> {
                if (reward.getRewards() != null) {
                  reward.getRewards().openMenu(
                    player,
                    t -> {},
                    close -> {}
                  );
                }
              }

              default -> {

                if (isClaimed) {
                  action.getPlayer().sendMessage(
                    AdventureTranslator.toNative(
                      UltraCalendar.language.getMessageClaimed()
                        .replace("%prefix%", UltraCalendar.language.getPrefix())
                        .replace("%day%", String.valueOf(day))
                    )
                  );
                  return;
                }

                if (!isNextDay) {
                  action.getPlayer().sendMessage(
                    AdventureTranslator.toNative(
                      UltraCalendar.language.getMessageVerySoon()
                        .replace("%prefix%", UltraCalendar.language.getPrefix())
                        .replace("%day%", String.valueOf(day))
                    )
                  );
                  return;
                }

                if (!hasPlayTime) {
                  action.getPlayer().sendMessage(
                    AdventureTranslator.toNative(
                      UltraCalendar.language.getMessageNeedPlayTime()
                        .replace("%prefix%", UltraCalendar.language.getPrefix())
                        .replace("%day%", String.valueOf(day))
                        .replace("%required%", reqStr)
                        .replace("%remaining%", remStr)
                        .replace("%playtime%", playStr)
                        .replace("%daily_playtime%", dailyPlayStr)
                        .replace("%total_playtime%", totalPlayStr)
                    )
                  );
                  return;
                }

                user.claim();
                user.markDirty();
                user.save();
                UltraCalendar.publishRedisInvalidation(player.getUuid());

                if (reward.getRewards() != null) {
                  reward.getRewards().giveRewards(player);
                }

                open(player);
              }
            }
          }),
          1,
          TimeUnit.SECONDS,
          1
        );

        if (UltraCalendar.config.isAutoPlace()) {
          buttons.add(button);
        } else {
          template.set(reward.getSlot(), button);
        }
      }

      ItemStack fillItem = Utils.parseItemId(UltraCalendar.language.getFill());
      template.fill(GooeyButton.of(fillItem));

      ItemModel closeModel = UltraCalendar.language.getCloseButton();
      int defaultCloseSlot = (rows * 9) - 5;
      int closeSlot = (closeModel != null && closeModel.getSlot() > 0) ? closeModel.getSlot() : defaultCloseSlot;

      if (closeModel != null && !closeModel.getItem().isEmpty()) {
        GooeyButton closeBtn = closeModel.getButton(action -> UIManager.closeUI(action.getPlayer()));
        if (closeModel.getSlot() > 0) {
          closeModel.applyTemplate(template, closeBtn);
        } else {
          template.set(closeSlot, closeBtn);
        }
      } else {
        template.set(closeSlot, UIUtils.getCloseButton(action -> UIManager.closeUI(action.getPlayer())));
      }

      if (UltraCalendar.config.isAutoPlace()) {
        template.set((rows * 9) - 9,
          UIUtils.getPreviousButton(action -> {})
        );

        template.set((rows * 9) - 1,
          UIUtils.getNextButton(action -> {})
        );
      }

      GooeyPage page;

      if (UltraCalendar.config.isAutoPlace()) {
        page = PaginationHelper.createPagesFromPlaceholders(template, buttons, null);
        page.setTitle(AdventureTranslator.toNative(UltraCalendar.language.getTitlemenu()));
      } else {
        page = GooeyPage.builder()
          .title(AdventureTranslator.toNative(UltraCalendar.language.getTitlemenu()))
          .template(template)
          .build();
      }

      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
    });
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
