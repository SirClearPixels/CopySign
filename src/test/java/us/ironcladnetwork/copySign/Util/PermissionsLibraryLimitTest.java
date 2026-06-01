package us.ironcladnetwork.copySign.Util;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Permissions#getMaxLibrarySigns(Player, int)} — the permission-based
 * resolution of a player's library size limit. This is exactly the kind of
 * string-parsing precedence logic that is easy to get subtly wrong, so it is locked
 * down here.
 * <p>
 * Player is a Mockito mock; unstubbed {@code hasPermission(...)} calls default to
 * {@code false}, so {@code copysign.admin} / {@code copysign.library.unlimited} are
 * absent unless explicitly granted.
 */
class PermissionsLibraryLimitTest {

    private static final int CONFIG_DEFAULT = 50;

    @Test
    void noPermissionsFallsBackToConfigDefault() {
        Player player = mock(Player.class);
        assertEquals(CONFIG_DEFAULT, Permissions.getMaxLibrarySigns(player, CONFIG_DEFAULT));
    }

    @Test
    void unlimitedPermissionReturnsMinusOne() {
        Player player = mock(Player.class);
        when(player.hasPermission("copysign.library.unlimited")).thenReturn(true);
        assertEquals(-1, Permissions.getMaxLibrarySigns(player, CONFIG_DEFAULT));
    }

    @Test
    void adminPermissionGrantsUnlimited() {
        Player player = mock(Player.class);
        when(player.hasPermission("copysign.admin")).thenReturn(true);
        assertEquals(-1, Permissions.getMaxLibrarySigns(player, CONFIG_DEFAULT));
    }

    @Test
    void singleLimitPermissionApplies() {
        Player player = mock(Player.class);
        when(player.hasPermission("copysign.library.limit.10")).thenReturn(true);
        assertEquals(10, Permissions.getMaxLibrarySigns(player, CONFIG_DEFAULT));
    }

    @Test
    void highestLimitWinsWhenMultipleGranted() {
        // A player with both limit.10 and limit.50 should get the larger allowance.
        Player player = mock(Player.class);
        when(player.hasPermission("copysign.library.limit.10")).thenReturn(true);
        when(player.hasPermission("copysign.library.limit.50")).thenReturn(true);
        assertEquals(50, Permissions.getMaxLibrarySigns(player, CONFIG_DEFAULT));
    }

    @Test
    void unlimitedBeatsNumericLimits() {
        Player player = mock(Player.class);
        when(player.hasPermission("copysign.library.unlimited")).thenReturn(true);
        when(player.hasPermission("copysign.library.limit.5")).thenReturn(true);
        assertEquals(-1, Permissions.getMaxLibrarySigns(player, CONFIG_DEFAULT));
    }

    @Test
    void topTierLimitIsRespected() {
        Player player = mock(Player.class);
        when(player.hasPermission("copysign.library.limit.100")).thenReturn(true);
        assertEquals(100, Permissions.getMaxLibrarySigns(player, CONFIG_DEFAULT));
    }
}
