package com.kingpixel.cobblecalendar.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobblecalendar.CobbleCalendar;
import com.kingpixel.cobblecalendar.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendar.models.UserInfo;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 15/08/2024 15:41
 */
public class DailyRewardUI {
  public static GooeyPage getPage(ServerPlayerEntity player) {

    ChestTemplate template = ChestTemplate
      .builder(CobbleCalendar.config.getRows())
      .build();

    UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
    CobbleCalendar.config.getRewards().forEach(rewards -> {
      if (!CobbleCalendar.config.isActive()) return;

      int day = rewards.getDay();
      boolean isClaimed = userInfo.getDay() >= rewards.getDay();
      boolean canClaimToday = userInfo.canClaim(day);

      ItemModel itemModel = isClaimed ? rewards.getClaimed() : rewards.getNotClaimed();
      ItemStack itemStack = itemModel.getItemStack().copy();
      if (canClaimToday) {
        // Todo: add enchantments to the itemStack
        itemStack = rewards.getCanClaim().getItemStack().copy();
        itemStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      }

      GooeyButton button = GooeyButton.builder()
        .display(itemStack)
        .onClick(action -> {
          if (!CobbleCalendar.config.isActive()) return;
          switch (action.getClickType()) {
            case SHIFT_RIGHT_CLICK, RIGHT_CLICK -> rewards.getRewards().openMenu(player);
            default -> {
              UIManager.closeUI(player);
              if (isClaimed) {
                action.getPlayer().sendMessage(
                  AdventureTranslator.toNative(
                    CobbleCalendar.language.getMessageClaimed()
                      .replace("%prefix%", CobbleCalendar.language.getPrefix())
                      .replace("%day%", String.valueOf(rewards.getDay()))
                  )
                );
                return; // Solo permitir si se puede reclamar y no ha sido reclamado
              }

              if (!canClaimToday) {
                action.getPlayer().sendMessage(
                  AdventureTranslator.toNative(
                    CobbleCalendar.language.getMessageVerySoon()
                      .replace("%prefix%", CobbleCalendar.language.getPrefix())
                      .replace("%day%", String.valueOf(rewards.getDay()))
                  )
                );
                return; // Solo permitir si se puede reclamar y no ha sido reclamado
              }

              userInfo.claim();
              DatabaseClientFactory.databaseClient.updateUserInfo(userInfo);
              // Recompensas basadas en permisos
              rewards.getRewards().giveRewards(player);
            }
          }
        })
        .build();
      template.set(rewards.getSlot(), button);
    });


    ItemStack itemStack = Utils.parseItemId(CobbleCalendar.language.getFill());

    GooeyButton fill = GooeyButton.of(itemStack);
    template.fill(fill);

    GooeyButton close = UIUtils.getCloseButton(action -> UIManager.closeUI(action.getPlayer()));

    template.set((CobbleCalendar.config.getRows() * 9) - 5, close);

    GooeyPage page = GooeyPage.builder()
      .title(AdventureTranslator.toNative(CobbleCalendar.language.getTitlemenu()))
      .template(template)
      .build();

    return page;
  }
}
