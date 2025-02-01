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
  private short day;
  private short slot;
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
    this.day = (short) day;
    this.slot = (short) slot;
    this.claimed = new ItemModel("minecraft:minecart", "Day " + day, List.of());
    this.canClaim = new ItemModel("minecraft:minecart");
    this.notClaimed = new ItemModel("minecraft:chest_minecart", "Day " + day, List.of());
    this.rewards = new AdvancedItemChance();
  }

  public void check() {
    if (canClaim == null) {
      canClaim = new ItemModel("minecraft:minecart");
    }
  }
}
