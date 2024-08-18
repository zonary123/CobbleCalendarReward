package com.kingpixel.cobblecalendarreward.models;

import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 14/08/2024 22:44
 */
@Getter
@Setter
@Data
@ToString
public class Rewards {
  private short day;
  private short slot;
  private ItemModel claimed;
  private ItemModel notClaimed;
  private List<PermissionRewards> permissionRewards;

  @Getter
  @Setter
  @Data
  @ToString
  public static class PermissionRewards {
    private String permission;
    private List<ItemChance> itemChances;

    public PermissionRewards() {
      this.permission = "";
      this.itemChances = new ArrayList<>();
      this.itemChances.addAll(ItemChance.defaultItemChances());
    }

    public PermissionRewards(String permission, List<ItemChance> itemChances) {
      this.permission = permission;
      this.itemChances = itemChances;
    }
  }


  public Rewards() {
    this.day = 1;
    this.slot = 0;
    this.claimed = new ItemModel("minecraft:minecart");
    this.notClaimed = new ItemModel("minecraft:chest_minecart");
    this.permissionRewards = new ArrayList<>();
    permissionRewards.add(new PermissionRewards());
  }

  public Rewards(short day) {
    this.day = day;
    this.slot = 0;
    this.claimed = new ItemModel("minecraft:minecart");
    this.notClaimed = new ItemModel("minecraft:chest_minecart");
    this.permissionRewards = new ArrayList<>();
    permissionRewards.add(new PermissionRewards());
  }

  public Rewards(int day, int slot) {
    this.day = (short) day;
    this.slot = (short) slot;
    this.claimed = new ItemModel("minecraft:minecart", "Day " + day, List.of());
    this.notClaimed = new ItemModel("minecraft:chest_minecart", "Day " + day, List.of());
    this.permissionRewards = new ArrayList<>();
    permissionRewards.add(new PermissionRewards());
  }

  public void giveReward(ServerPlayerEntity player) {
    permissionRewards.forEach(reward -> {
      if (reward.getPermission().isEmpty() || LuckPermsUtil.checkPermission(player, reward.getPermission())) {
        ItemChance.getAllRewards(reward.getItemChances(), player);
      }
    });
  }

}
