package com.kingpixel.ultracalendar.models;

import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.ultracalendar.UltraCalendar;
import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class User implements Serializable {

  private UUID uuid;
  private String name;

  private int day;
  private long dayClaimed;
  private long lastJoin;

  private transient AtomicBoolean dirty = new AtomicBoolean(false);

  public User(ServerPlayerEntity player) {
    this.uuid = player.getUuid();
    this.name = player.getGameProfile().getName();

    long today = today();
    this.day = 0;
    this.dayClaimed = today - 1;
    this.lastJoin = today;
  }

  public void setLastJoin(long lastJoin) {
    this.lastJoin = lastJoin;
    markDirty();
  }

  private long today() {
    return LocalDate.now().toEpochDay();
  }

  public AtomicBoolean getDirty() {
    if (dirty == null) dirty = new AtomicBoolean(false);
    return dirty;
  }

  public void markDirty() {
    getDirty().set(true);
  }

  public boolean canClaim() {
    return today() > dayClaimed;
  }

  public boolean claim() {
    long today = today();

    if (today <= dayClaimed) return false;

    day++;
    dayClaimed = today;
    lastJoin = today;

    markDirty();
    return true;
  }

  public boolean hasMissedDays() {
    return today() - dayClaimed > 1;
  }

  public void onJoin(ServerPlayerEntity player) {
    fix();

    long today = today();

    if (hasMissedDays() && UltraCalendar.config.isAutoReset()) {
      reset(player, false);
      return;
    }

    if (day >= UltraCalendar.config.maxDay()) {
      reset(player, true);
      return;
    }

    if (lastJoin != today) {
      lastJoin = today;
      markDirty();
    }
  }

  public void reset(ServerPlayerEntity player, boolean completed) {
    long today = today();

    String message = completed
      ? UltraCalendar.language.getMessageCompleted()
      : UltraCalendar.language.getMessageReset();

    if (UltraCalendar.config.isSendMessageReset()) {
      message = message.replace("%day%", String.valueOf(day));

      PlayerUtils.sendMessage(
        player,
        message,
        UltraCalendar.language.getPrefix(),
        TypeMessage.CHAT
      );
    }

    day = 0;
    dayClaimed = completed ? today : today - 1;
    lastJoin = today;

    markDirty();
  }

  public void fix() {
    long today = today();

    boolean changed = false;

    if (day < 0) {
      day = 0;
      changed = true;
    }

    if (dayClaimed > today) {
      dayClaimed = today;
      changed = true;
    }

    if (lastJoin > today) {
      lastJoin = today;
      changed = true;
    }

    if (hasMissedDays() && UltraCalendar.config.isAutoReset()) {
      day = 0;
      dayClaimed = today - 1;
      changed = true;
    }

    if (day > UltraCalendar.config.maxDay()) {
      day = UltraCalendar.config.maxDay();
      changed = true;
    }

    if (changed) {
      markDirty();
    }
  }

  public CompletableFuture<Void> save() {
    if (getDirty().compareAndSet(true, false)) return UltraCalendar.database.saveOrUpdateUser(this);
    return CompletableFuture.completedFuture(null);
  }
}