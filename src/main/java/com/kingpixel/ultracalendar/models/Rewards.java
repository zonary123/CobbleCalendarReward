package com.kingpixel.ultracalendar.models;


import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.ultracalendar.UltraCalendar;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  private boolean dailyTime = true;
  private @Nullable DurationValue requiredTime;
  private @Nullable ItemModel claimed;
  private @Nullable ItemModel canClaim;
  private @Nullable ItemModel notClaimed;
  private @NotNull AdvancedItemChance rewards;


  public Rewards() {
    this.day = 1;
    this.slot = 0;
    this.requiredTime = DurationValue.ofMillis(0);
    this.claimed = new ItemModel("minecraft:minecart");
    this.canClaim = new ItemModel("minecraft:minecart");
    this.notClaimed = new ItemModel("minecraft:chest_minecart");
    this.rewards = new AdvancedItemChance();
  }

  public Rewards(short day) {
    this.day = day;
    this.slot = 0;
    this.requiredTime = DurationValue.ofMillis(0);
    this.claimed = new ItemModel("minecraft:minecart");
    this.canClaim = new ItemModel("minecraft:minecart");
    this.notClaimed = new ItemModel("minecraft:chest_minecart");
    this.rewards = new AdvancedItemChance();
  }

  public Rewards(int day, int slot) {
    this.day = day;
    this.slot = slot;
    this.requiredTime = DurationValue.ofMillis(0);
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

  public @NotNull DurationValue getRequiredTime() {
    if (requiredTime != null) {
      return requiredTime;
    }
    if (UltraCalendar.config != null && UltraCalendar.config.getDefaultPlayTime() != null) {
      return UltraCalendar.config.getDefaultPlayTime();
    }
    return DurationValue.ofMillis(0);
  }

  public void check() {
    if (canClaim == null) {
      canClaim = new ItemModel("minecraft:minecart");
    }
    if (claimed == null) {
      claimed = new ItemModel("minecraft:minecart");
    }
    if (notClaimed == null) {
      notClaimed = new ItemModel("minecraft:chest_minecart");
    }
    if (rewards == null) {
      rewards = new AdvancedItemChance();
    }
    if (requiredTime == null) {
      requiredTime = DurationValue.ofMillis(0);
    }
  }
}
