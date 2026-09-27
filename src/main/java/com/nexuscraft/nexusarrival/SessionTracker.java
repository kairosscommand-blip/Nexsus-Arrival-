package com.nexuscraft.nexusarrival;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks how long THIS session has run for each online player -- purely in memory, on purpose.
 *  There is deliberately no persistence here: the whole point of "gets crazier the longer you've
 *  been on" is that it resets back to calm every time a player logs back in, so a restart or a
 *  relog is a clean slate, not something to carry across. */
final class SessionTracker {

    private final Map<UUID, Long> joinedAtMillis = new HashMap<>();
    private final Map<UUID, Long> lastVisionAtMillis = new HashMap<>();

    void onJoin(UUID uuid) {
        joinedAtMillis.put(uuid, System.currentTimeMillis());
        lastVisionAtMillis.remove(uuid);
    }

    void onQuit(UUID uuid) {
        joinedAtMillis.remove(uuid);
        lastVisionAtMillis.remove(uuid);
    }

    /** Minutes since this player's most recent join, or 0 if they're not tracked (shouldn't
     *  normally happen for an online player, but never worth a NPE over). */
    double sessionMinutes(UUID uuid) {
        Long joined = joinedAtMillis.get(uuid);
        if (joined == null) {
            return 0;
        }
        return (System.currentTimeMillis() - joined) / 60000.0;
    }

    boolean isPastCooldown(UUID uuid, int minSecondsBetweenVisions) {
        Long last = lastVisionAtMillis.get(uuid);
        if (last == null) {
            return true;
        }
        return (System.currentTimeMillis() - last) >= minSecondsBetweenVisions * 1000L;
    }

    void markVisionPlayed(UUID uuid) {
        lastVisionAtMillis.put(uuid, System.currentTimeMillis());
    }
}
