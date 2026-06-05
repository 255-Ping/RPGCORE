package com.github._255_ping.rpg.api.input;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Opens a virtual sign editor to collect a single line of text from a player.
 *
 * <p>The sign exists only on the prompting player's client — no real block is placed.
 * Only one prompt can be active per player; starting a new prompt cancels the old one first.
 *
 * <p>Obtain via {@code RpgServices.signInput()}.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * RpgServices.signInput().ask(player, "Enter price:", input -> {
 *     if (input == null) return; // cancelled or timed out
 *     try {
 *         long price = Long.parseLong(input);
 *         // ... proceed
 *     } catch (NumberFormatException e) {
 *         player.sendMessage("Invalid number.");
 *     }
 * });
 * }</pre>
 *
 * <h3>Rules</h3>
 * <ul>
 *   <li>The callback is always called on the main server thread.
 *   <li>The callback receives {@code null} on timeout, cancel, or disconnect.
 *   <li>The callback receives a non-empty trimmed string on success.
 * </ul>
 */
public interface SignInputService {

    /**
     * Opens a virtual sign editor for the player. The label is shown on the sign as
     * the prompt (truncated to 15 characters). The callback fires on the main thread
     * with the typed text, or {@code null} if the prompt was cancelled/timed out.
     *
     * @param player   the player to prompt
     * @param label    short description of what to enter (shown on the sign)
     * @param callback receives the input string, or {@code null} on cancel/timeout
     */
    void ask(Player player, String label, Consumer<String> callback);

    /**
     * Cancels any active prompt for this player. Fires the pending callback with
     * {@code null} and restores the player's view of the block at the prompt location.
     * No-op if no prompt is currently active for the player.
     *
     * @param playerId the UUID of the player whose prompt to cancel
     */
    void cancel(UUID playerId);
}
