package com.github._255_ping.rpg.trade;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Holds the mutable state of a single active trade between two players.
 * Both players get their own {@link Inventory} view:
 * <ul>
 *   <li>Their own offered items on the left (interactive)</li>
 *   <li>The other player's offered items on the right (read-only display)</li>
 * </ul>
 */
public final class TradeSession {

    public enum State {
        /** Both GUIs open, players setting up offers. */
        OPEN,
        /** Player A clicked Confirm; waiting for B. */
        CONFIRMED_A,
        /** Player B clicked Confirm; waiting for A. */
        CONFIRMED_B,
        /** Both confirmed; countdown is running. */
        COUNTDOWN,
        /** Terminal state — trade completed or cancelled. */
        DONE
    }

    /** Number of item slots on each side. */
    static final int OFFER_SIZE = 9;

    final UUID uuidA;
    final UUID uuidB;
    final String nameA;
    final String nameB;

    /** Offered items — indices match the OWN_SLOTS / THEIR_SLOTS arrays in TradeGui. */
    final ItemStack[] slotsA = new ItemStack[OFFER_SIZE];
    final ItemStack[] slotsB = new ItemStack[OFFER_SIZE];

    long coinsA = 0;
    long coinsB = 0;

    State state = State.OPEN;

    /** A's inventory: A's items on the left, B's on the right. */
    Inventory invA;
    /** B's inventory: B's items on the left, A's on the right. */
    Inventory invB;

    int countdownTaskId = -1;
    int countdownLeft;

    TradeSession(UUID uuidA, String nameA, UUID uuidB, String nameB) {
        this.uuidA = uuidA;
        this.nameA = nameA;
        this.uuidB = uuidB;
        this.nameB = nameB;
    }

    // ── Perspective helpers ─────────────────────────────────────────────────

    boolean isA(UUID uuid) { return uuid.equals(uuidA); }

    ItemStack[] mySlots(UUID uuid)    { return isA(uuid) ? slotsA : slotsB; }
    ItemStack[] theirSlots(UUID uuid) { return isA(uuid) ? slotsB : slotsA; }

    long myCoins(UUID uuid)    { return isA(uuid) ? coinsA : coinsB; }
    long theirCoins(UUID uuid) { return isA(uuid) ? coinsB : coinsA; }

    void setMyCoins(UUID uuid, long amount) {
        if (isA(uuid)) coinsA = amount; else coinsB = amount;
    }

    String myName(UUID uuid)    { return isA(uuid) ? nameA : nameB; }
    String theirName(UUID uuid) { return isA(uuid) ? nameB : nameA; }

    UUID otherUuid(UUID uuid) { return isA(uuid) ? uuidB : uuidA; }

    Inventory myInv(UUID uuid)    { return isA(uuid) ? invA : invB; }
    Inventory theirInv(UUID uuid) { return isA(uuid) ? invB : invA; }

    /** True if this player has already clicked Confirm (including during countdown). */
    boolean hasConfirmed(UUID uuid) {
        return switch (state) {
            case CONFIRMED_A -> isA(uuid);
            case CONFIRMED_B -> !isA(uuid);
            case COUNTDOWN   -> true;
            default          -> false;
        };
    }
}
