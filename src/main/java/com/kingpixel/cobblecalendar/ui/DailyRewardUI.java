package com.kingpixel.cobblecalendar.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.RateLimitedButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.models.Rewards;
import com.kingpixel.cobblecalendar.models.User;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DailyRewardUI {

  public static CompletableFuture<Void> open(ServerPlayerEntity player) {
    return CobbleCalendar.ASYNC.runAsync(() -> {

      User user = CobbleCalendar.database.getUser(player);
      if (user == null) return;

      ChestTemplate template = ChestTemplate.builder(CobbleCalendar.config.getRows()).build();
      new Rectangle(CobbleCalendar.config.getRows()).apply(template);

      List<Button> buttons = new ArrayList<>();

      for (Rewards reward : CobbleCalendar.config.getRewards()) {

        if (!CobbleCalendar.config.isActive()) return;

        int day = reward.getDay();

        boolean isClaimed = user.getDay() >= day;
        boolean canClaim = user.canClaim() && user.getDay() + 1 == day;

        ItemModel model = reward.getNotClaimed();
        if (isClaimed) model = reward.getClaimed();
        if (canClaim) model = reward.getCanClaim();

        if (model.getItem().isEmpty()) {
          model = isClaimed
            ? CobbleCalendar.language.getGlobalClaimed()
            : canClaim
              ? CobbleCalendar.language.getGlobalCanClaim()
              : CobbleCalendar.language.getGlobalNotClaimed();
        }

        RateLimitedButton button = model.getButton(
          1,
          model.getDisplayname().replace("%day%", String.valueOf(day)),
          null,
          action -> CobbleCalendar.ASYNC.runAsync(() -> {

            if (!CobbleCalendar.config.isActive()) return;

            switch (action.getClickType()) {

              case SHIFT_RIGHT_CLICK, RIGHT_CLICK -> reward.getRewards().openMenu(
                player,
                t -> {
                },
                close -> open(close.getPlayer())
              );

              default -> {

                if (isClaimed) {
                  action.getPlayer().sendMessage(
                    AdventureTranslator.toNative(
                      CobbleCalendar.language.getMessageClaimed()
                        .replace("%prefix%", CobbleCalendar.language.getPrefix())
                        .replace("%day%", String.valueOf(day))
                    )
                  );
                  return;
                }

                if (!canClaim) {
                  action.getPlayer().sendMessage(
                    AdventureTranslator.toNative(
                      CobbleCalendar.language.getMessageVerySoon()
                        .replace("%prefix%", CobbleCalendar.language.getPrefix())
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

        if (CobbleCalendar.config.isAutoPlace()) {
          buttons.add(button);
        } else {
          template.set(reward.getSlot(), button);
        }
      }

      ItemStack fillItem = Utils.parseItemId(CobbleCalendar.language.getFill());
      template.fill(GooeyButton.of(fillItem));

      template.set((CobbleCalendar.config.getRows() * 9) - 5,
        UIUtils.getCloseButton(action -> UIManager.closeUI(action.getPlayer()))
      );

      if (CobbleCalendar.config.isAutoPlace()) {
        template.set((CobbleCalendar.config.getRows() * 9) - 9,
          UIUtils.getPreviousButton(action -> {
          })
        );

        template.set((CobbleCalendar.config.getRows() * 9) - 1,
          UIUtils.getNextButton(action -> {
          })
        );
      }

      GooeyPage page;

      if (CobbleCalendar.config.isAutoPlace()) {
        page = PaginationHelper.createPagesFromPlaceholders(template, buttons, null);
        page.setTitle(AdventureTranslator.toNative(CobbleCalendar.language.getTitlemenu()));
      } else {
        page = GooeyPage.builder()
          .title(AdventureTranslator.toNative(CobbleCalendar.language.getTitlemenu()))
          .template(template)
          .build();
      }

      CobbleUtils.server.execute(() ->
        UIManager.openUIForcefully(player, page)
      );
    });
  }
}