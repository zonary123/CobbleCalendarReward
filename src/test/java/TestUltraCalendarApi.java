import com.kingpixel.ultracalendar.api.UltraCalendarApi;
import com.kingpixel.ultracalendar.api.action.CalendarActionResult;
import com.kingpixel.ultracalendar.api.model.CalendarRewardEntryView;
import com.kingpixel.ultracalendar.api.model.PlayerCalendarContext;
import com.kingpixel.ultracalendar.api.ui.CalendarMenuProvider;
import com.kingpixel.ultracalendar.api.ui.CalendarMenuRegistry;
import com.kingpixel.ultracalendar.application.CalendarActionService;
import com.kingpixel.ultracalendar.models.CalendarMode;
import net.minecraft.server.network.ServerPlayerEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUltraCalendarApi {

  @AfterEach
  void tearDown() {
    CalendarMenuRegistry.unregister();
  }

  @Test
  void testActionResultSuccess() {
    CalendarActionResult result = CalendarActionResult.success("Claimed day 5", 5);
    assertTrue(result.isSuccess());
    assertEquals(CalendarActionResult.Status.SUCCESS, result.getStatus());
    assertEquals("Claimed day 5", result.getMessage());
    assertEquals(5, result.getDay());
  }

  @Test
  void testActionResultFailures() {
    CalendarActionResult alreadyClaimed = CalendarActionResult.alreadyClaimed("Already claimed", 3);
    assertFalse(alreadyClaimed.isSuccess());
    assertEquals(CalendarActionResult.Status.ALREADY_CLAIMED, alreadyClaimed.getStatus());
    assertEquals(3, alreadyClaimed.getDay());

    CalendarActionResult notAvailable = CalendarActionResult.notAvailableYet("Not yet", 10);
    assertFalse(notAvailable.isSuccess());
    assertEquals(CalendarActionResult.Status.NOT_AVAILABLE_YET, notAvailable.getStatus());

    CalendarActionResult needTime = CalendarActionResult.needPlayTime("Need 100s", 1, 100, 50);
    assertFalse(needTime.isSuccess());
    assertEquals(CalendarActionResult.Status.NEED_PLAYTIME, needTime.getStatus());
    assertEquals(100, needTime.getRequiredSeconds());
    assertEquals(50, needTime.getCurrentPlayTimeSeconds());

    CalendarActionResult err = CalendarActionResult.failure(CalendarActionResult.Status.ERROR, "Boom");
    assertFalse(err.isSuccess());
    assertEquals(CalendarActionResult.Status.ERROR, err.getStatus());
  }

  @Test
  void testMenuRegistryLifecycle() {
    assertFalse(CalendarMenuRegistry.hasCustomProvider());

    CalendarMenuProvider custom = player -> {};
    CalendarMenuRegistry.register(custom);
    assertTrue(CalendarMenuRegistry.hasCustomProvider());
    assertEquals(custom, CalendarMenuRegistry.getProvider());

    CalendarMenuRegistry.register(null);
    assertFalse(CalendarMenuRegistry.hasCustomProvider());

    CalendarMenuRegistry.register(custom);
    assertTrue(CalendarMenuRegistry.hasCustomProvider());
    CalendarMenuRegistry.unregister();
    assertFalse(CalendarMenuRegistry.hasCustomProvider());
  }

  @Test
  void testRewardEntryView() {
    CalendarRewardEntryView entry = new CalendarRewardEntryView(
      7,
      false,
      true,
      true,
      true,
      300,
      0,
      3
    );

    assertEquals(7, entry.getDay());
    assertFalse(entry.isClaimed());
    assertTrue(entry.isAvailable());
    assertTrue(entry.isHasPlayTime());
    assertTrue(entry.isCanClaim());
    assertEquals(300, entry.getRequiredPlayTimeSeconds());
    assertEquals(0, entry.getRemainingPlayTimeSeconds());
    assertEquals(3, entry.getRewardsCount());
  }

  @Test
  void testPlayerCalendarContext() {
    UUID uuid = UUID.randomUUID();
    CalendarRewardEntryView day1 = new CalendarRewardEntryView(1, true, true, true, false, 60, 0, 1);
    CalendarRewardEntryView day2 = new CalendarRewardEntryView(2, false, true, true, true, 60, 0, 1);

    PlayerCalendarContext ctx = new PlayerCalendarContext(
      uuid,
      "PlayerOne",
      1,
      1000L,
      3600L,
      7200L,
      List.of(1),
      CalendarMode.STREAK,
      2,
      true,
      List.of(day1, day2),
      1
    );

    assertEquals(uuid, ctx.getPlayerUuid());
    assertEquals("PlayerOne", ctx.getPlayerName());
    assertEquals(1, ctx.getCurrentStreakDay());
    assertEquals(1000L, ctx.getDayClaimed());
    assertEquals(3600L, ctx.getDailyPlayTime());
    assertEquals(7200L, ctx.getTotalPlayTime());
    assertEquals(List.of(1), ctx.getClaimedDays());
    assertEquals(CalendarMode.STREAK, ctx.getMode());
    assertEquals(2, ctx.getTargetDay());
    assertTrue(ctx.isCanClaimToday());
    assertEquals(2, ctx.getDays().size());
    assertEquals(1, ctx.getTotalRewardsClaimable());

    Optional<CalendarRewardEntryView> foundDay = ctx.getDay(2);
    assertTrue(foundDay.isPresent());
    assertTrue(foundDay.get().isCanClaim());
  }

  @Test
  void testActionServiceNullSafety() throws ExecutionException, InterruptedException {
    CalendarActionService service = new CalendarActionService();

    CalendarActionResult r1 = service.claimReward(null).get();
    assertNotNull(r1);
    assertEquals(CalendarActionResult.Status.PLAYER_OFFLINE, r1.getStatus());

    CalendarActionResult r2 = service.claimReward(null, 1).get();
    assertNotNull(r2);
    assertEquals(CalendarActionResult.Status.PLAYER_OFFLINE, r2.getStatus());

    CalendarActionResult r3 = service.resetUser(null, false).get();
    assertNotNull(r3);
    assertEquals(CalendarActionResult.Status.PLAYER_OFFLINE, r3.getStatus());

    CalendarActionResult r4 = service.setDay(null, 5).get();
    assertNotNull(r4);
    assertEquals(CalendarActionResult.Status.PLAYER_OFFLINE, r4.getStatus());
  }

  @Test
  void testApiFacadeAccessors() {
    assertNotNull(UltraCalendarApi.getActionService());
    assertNotNull(UltraCalendarApi.getContextService());
    assertNotNull(UltraCalendarApi.getMenuProvider());
    assertFalse(UltraCalendarApi.hasCustomMenuProvider());
  }
}
