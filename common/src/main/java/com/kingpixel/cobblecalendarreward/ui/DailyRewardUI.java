package com.kingpixel.cobblecalendarreward.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobblecalendarreward.CobbleCalendarReward;
import com.kingpixel.cobblecalendarreward.database.DatabaseClientFactory;
import com.kingpixel.cobblecalendarreward.models.UserInfo;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDateTime;

/**
 * @author Carlos Varas Alonso - 15/08/2024 15:41
 */
public class DailyRewardUI {
  public static GooeyPage getPage(ServerPlayerEntity player) {

    ChestTemplate template = ChestTemplate
      .builder(CobbleCalendarReward.config.getRows())
      .build();

    UserInfo userInfo = DatabaseClientFactory.databaseClient.getUserInfo(player);
    CobbleCalendarReward.config.getRewards().forEach(rewards -> {
      if (!CobbleCalendarReward.config.isActive()) return;

      int day = rewards.getDay();
      boolean isClaimed = userInfo.getDay() >= rewards.getDay();
      boolean canClaimToday = userInfo.canClaim(day);

      ItemModel itemModel = isClaimed ? rewards.getClaimed() : rewards.getNotClaimed();

      GooeyButton button = itemModel.getButton(action -> {
        if (isClaimed) {
          action.getPlayer().sendMessage(
            AdventureTranslator.toNative(
              CobbleCalendarReward.language.getMessageClaimed()
                .replace("%prefix%", CobbleCalendarReward.language.getPrefix())
                .replace("%day%", String.valueOf(rewards.getDay()))
            )
          );
          return; // Solo permitir si se puede reclamar y no ha sido reclamado
        }

        if (!canClaimToday) {
          action.getPlayer().sendMessage(
            AdventureTranslator.toNative(
              CobbleCalendarReward.language.getMessageVerySoon()
                .replace("%prefix%", CobbleCalendarReward.language.getPrefix())
                .replace("%day%", String.valueOf(rewards.getDay()))
            )
          );
          return; // Solo permitir si se puede reclamar y no ha sido reclamado
        }

        // Actualizar la información del usuario
        DatabaseClientFactory.databaseClient.updateUserInfo(player);

        // Recompensas basadas en permisos
        rewards.giveReward(player);

        UIManager.openUIForcefully(action.getPlayer(), getPage(player));
      });
      template.set(rewards.getSlot(), button);
    });


    ItemStack itemStack = Utils.parseItemId(CobbleCalendarReward.language.getFill());

    GooeyButton fill = GooeyButton.of(itemStack);
    template.fill(fill);

    GooeyButton close = UIUtils.getCloseButton(action -> UIManager.closeUI(action.getPlayer()));

    template.set((CobbleCalendarReward.config.getRows() * 9) - 5, close);

    GooeyPage page = GooeyPage.builder()
      .title(AdventureTranslator.toNative(CobbleCalendarReward.language.getTitlemenu()))
      .template(template)
      .build();

    page.subscribe(LocalDateTime.now(), page1 -> UIManager.openUIForcefully(player, page1));

    return page;
  }
}
