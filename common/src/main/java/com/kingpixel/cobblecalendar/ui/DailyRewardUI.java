package com.kingpixel.cobblecalendar.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendar.models.Rewards;
import com.kingpixel.cobblecalendar.models.UserInfo;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.features.shops.Shop;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 15/08/2024 15:41
 */
public class DailyRewardUI {
  public static void open(ServerPlayerEntity player) {

    ChestTemplate template = ChestTemplate
      .builder(CobbleCalendar.config.getRows())
      .build();

    new Shop.Rectangle(CobbleCalendar.config.getRows()).apply(template);

    List<Button> buttons = new ArrayList<>();

    UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
    for (Rewards reward : CobbleCalendar.config.getRewards()) {
      if (!CobbleCalendar.config.isActive()) return;

      int day = reward.getDay();
      boolean isClaimed = userInfo.getDay() >= reward.getDay();
      boolean canClaimToday = userInfo.canClaim(day);

      ItemModel itemModel;
      if (CobbleCalendar.language.isGlobal()) {
        itemModel = isClaimed ? CobbleCalendar.language.getGlobalClaimed() : CobbleCalendar.language.getGlobalNotClaimed();
      } else {
        itemModel = isClaimed ? reward.getClaimed() : reward.getNotClaimed();
      }
      ItemStack itemStack = itemModel.getItemStack().copy();
      if (canClaimToday) {
        // Todo: add enchantments to the itemStack
        if (CobbleCalendar.language.isGlobal()) {
          itemStack = CobbleCalendar.language.getGlobalCanClaim().getItemStack().copy();
        } else {
          itemStack = reward.getCanClaim().getItemStack().copy();
        }
        itemStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      }

      GooeyButton button = GooeyButton.builder()
        .display(itemStack)
        .onClick(action -> {
          if (!CobbleCalendar.config.isActive()) return;
          switch (action.getClickType()) {
            case SHIFT_RIGHT_CLICK, RIGHT_CLICK -> reward.getRewards().openMenu(player);
            default -> {
              UIManager.closeUI(player);
              if (isClaimed) {
                action.getPlayer().sendMessage(
                  AdventureTranslator.toNative(
                    CobbleCalendar.language.getMessageClaimed()
                      .replace("%prefix%", CobbleCalendar.language.getPrefix())
                      .replace("%day%", String.valueOf(reward.getDay()))
                  )
                );
                return; // Solo permitir si se puede reclamar y no ha sido reclamado
              }

              if (!canClaimToday) {
                action.getPlayer().sendMessage(
                  AdventureTranslator.toNative(
                    CobbleCalendar.language.getMessageVerySoon()
                      .replace("%prefix%", CobbleCalendar.language.getPrefix())
                      .replace("%day%", String.valueOf(reward.getDay()))
                  )
                );
                return; // Solo permitir si se puede reclamar y no ha sido reclamado
              }

              userInfo.claim();
              DatabaseClientFactory.databaseClient.updateUserInfo(userInfo);
              // Recompensas basadas en permisos
              reward.getRewards().giveRewards(player);
            }
          }
        })
        .build();
      if (CobbleCalendar.config.isAutoPlace()) {
        buttons.add(button);
      } else {
        template.set(reward.getSlot(), button);
      }
    }


    ItemStack itemStack = Utils.parseItemId(CobbleCalendar.language.getFill());

    GooeyButton fill = GooeyButton.of(itemStack);
    template.fill(fill);

    GooeyButton close = UIUtils.getCloseButton(action -> UIManager.closeUI(action.getPlayer()));

    if (CobbleCalendar.config.isAutoPlace()) {
      LinkedPageButton previous = UIUtils.getPreviousButton(action -> {
      });
      LinkedPageButton next = UIUtils.getNextButton(action -> {
      });
      template.set((CobbleCalendar.config.getRows() * 9) - 9, previous);
      template.set((CobbleCalendar.config.getRows() * 9) - 1, next);
    }

    template.set((CobbleCalendar.config.getRows() * 9) - 5, close);
    GooeyPage page;
    if (CobbleCalendar.config.isAutoPlace()) {
      page = PaginationHelper.createPagesFromPlaceholders(
        template,
        buttons,
        null
      );
      page.setTitle(AdventureTranslator.toNative(CobbleCalendar.language.getTitlemenu()));
    } else {
      page = GooeyPage.builder()
        .title(AdventureTranslator.toNative(CobbleCalendar.language.getTitlemenu()))
        .template(template)
        .build();
    }

    UIManager.openUIForcefully(player, page);
  }
}
