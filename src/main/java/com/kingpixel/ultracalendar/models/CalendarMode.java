package com.kingpixel.ultracalendar.models;

/**
 * Operating mode of the calendar system.
 *
 * @author Carlos Varas Alonso
 */
public enum CalendarMode {
  /**
   * Day rewards correspond strictly to the current day of the month (1-31).
   * Unclaimed past days within the catchup margin can also be claimed.
   */
  MONTH_DAY,

  /**
   * Cumulative consecutive login streak (Day 1, Day 2, ...).
   */
  STREAK
}

