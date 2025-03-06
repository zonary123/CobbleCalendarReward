package com.kingpixel.cobblecalendar.models;

import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.Model.ItemModel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 14/08/2024 22:44
 */
@Getter
@Setter
@Data
@ToString
public class Rewards {
  private int day;
  private int slot;
  private ItemModel claimed;
  private ItemModel canClaim;
  private ItemModel notClaimed;
  private AdvancedItemChance rewards;


  public Rewards() {
    this.day = 1;
    this.slot = 0;
    this.claimed = new ItemModel("minecraft:minecart");
    this.canClaim = new ItemModel("minecraft:minecart");
    this.notClaimed = new ItemModel("minecraft:chest_minecart");
    this.rewards = new AdvancedItemChance();
  }

  public Rewards(short day) {
    this.day = day;
    this.slot = 0;
    this.claimed = new ItemModel("minecraft:minecart");
    this.canClaim = new ItemModel("minecraft:minecart");
    this.notClaimed = new ItemModel("minecraft:chest_minecart");
    this.rewards = new AdvancedItemChance();
  }

  public Rewards(int day, int slot) {
    this.day = day;
    this.slot = slot;
    this.claimed = new ItemModel("", "Day %day%", List.of(
      "You have already claimed this reward."
    ));
    this.canClaim = new ItemModel("", "Day %day%", List.of(
      "You can claim this reward."
    ));
    this.notClaimed = new ItemModel("", "Day %day%", List.of(
      "This reward will be available very soon."
    ));
    this.rewards = new AdvancedItemChance();
    this.rewards.setTitle("Day " + day);
  }

  public void check() {
    if (canClaim == null) {
      canClaim = new ItemModel("minecraft:minecart");
    }
  }
}
