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
import com.kingpixel.ultracalendar.models.Rewards;
import com.kingpixel.ultracalendar.models.User;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DailyRewardUI {

  public static CompletableFuture<Void> open(ServerPlayerEntity player) {
    return UltraCalendar.ASYNC.runAsync(() -> {

      User user = UltraCalendar.database.getUser(player);
      if (user == null) return;

      int rows = UltraCalendar.config.getRows();
      ChestTemplate template = ChestTemplate.builder(rows).build();
      new Rectangle(rows).apply(template);

      List<Button> buttons = new ArrayList<>();

      for (Rewards reward : UltraCalendar.config.getRewards()) {

        if (!UltraCalendar.config.isActive()) return;

        int day = reward.getDay();

        boolean isClaimed = user.getDay() >= day;
        boolean canClaim = user.canClaim() && user.getDay() + 1 == day;

        ItemModel model = reward.getNotClaimed();
        if (isClaimed) model = reward.getClaimed();
        if (canClaim) model = reward.getCanClaim();

        if (model == null || model.getItem().isEmpty()) {
          model = isClaimed
            ? UltraCalendar.language.getGlobalClaimed()
            : canClaim
              ? UltraCalendar.language.getGlobalCanClaim()
              : UltraCalendar.language.getGlobalNotClaimed();
        }

        RateLimitedButton button = model.getButton(
          1,
          model.getDisplayname().replace("%day%", String.valueOf(day)),
          null,
          action -> UltraCalendar.ASYNC.runAsync(() -> {

            if (!UltraCalendar.config.isActive()) return;

            switch (action.getClickType()) {

              case SHIFT_RIGHT_CLICK, RIGHT_CLICK -> reward.getRewards().openMenu(
                player,
                t -> {},
                close -> {}
              );

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

                if (!canClaim) {
                  action.getPlayer().sendMessage(
                    AdventureTranslator.toNative(
                      UltraCalendar.language.getMessageVerySoon()
                        .replace("%prefix%", UltraCalendar.language.getPrefix())
                        .replace("%day%", String.valueOf(day))
                    )
                  );
                  return;
                }

                user.claim();
                user.markDirty();
                reward.getRewards().giveRewards(player);

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
}
