package com.kingpixel.ultracalendar.api.action;

import lombok.Getter;

/**
 * Result representation for UltraCalendar headless claim and administrative actions.
 */
@Getter
public class CalendarActionResult {

  public enum Status {
    SUCCESS,
    ALREADY_CLAIMED,
    NOT_AVAILABLE_YET,
    NEED_PLAYTIME,
    NO_REWARD_FOUND,
    CALENDAR_DISABLED,
    PLAYER_OFFLINE,
    ERROR
  }

  private final boolean success;
  private final Status status;
  private final String message;
  private final int day;
  private final long requiredSeconds;
  private final long currentPlayTimeSeconds;

  private CalendarActionResult(boolean success,
                               Status status,
                               String message,
                               int day,
                               long requiredSeconds,
                               long currentPlayTimeSeconds) {
    this.success = success;
    this.status = status;
    this.message = message != null ? message : "";
    this.day = day;
    this.requiredSeconds = Math.max(0, requiredSeconds);
    this.currentPlayTimeSeconds = Math.max(0, currentPlayTimeSeconds);
  }

  public static CalendarActionResult success(String message, int day) {
    return new CalendarActionResult(true, Status.SUCCESS, message, day, 0, 0);
  }

  public static CalendarActionResult alreadyClaimed(String message, int day) {
    return new CalendarActionResult(false, Status.ALREADY_CLAIMED, message, day, 0, 0);
  }

  public static CalendarActionResult notAvailableYet(String message, int day) {
    return new CalendarActionResult(false, Status.NOT_AVAILABLE_YET, message, day, 0, 0);
  }

  public static CalendarActionResult needPlayTime(String message, int day, long required, long current) {
    return new CalendarActionResult(false, Status.NEED_PLAYTIME, message, day, required, current);
  }

  public static CalendarActionResult failure(Status status, String message) {
    return new CalendarActionResult(false, status, message, 0, 0, 0);
  }

  public static CalendarActionResult failure(Status status, String message, int day) {
    return new CalendarActionResult(false, status, message, day, 0, 0);
  }
}
