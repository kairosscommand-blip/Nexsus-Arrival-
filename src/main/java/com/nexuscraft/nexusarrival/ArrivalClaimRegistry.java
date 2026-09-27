package com.nexuscraft.nexusarrival;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tracks which players have already received the guaranteed, once-ever arrival vision --
 *  persisted to {@code arrival_claims.yml}. Same shape and same guarantee as NexusStarter's
 *  GuideItemClaimRegistry / NexusMenu's MenuItemClaimRegistry: {@link #claim} only ever
 *  succeeds once per UUID, so a brand-new player gets it automatically on first join, and a
 *  player who joined before this plugin existed gets it exactly once on their very next join --
 *  never zero times, never twice. */
final class ArrivalClaimRegistry {

    private final Set<UUID> claimed = new HashSet<>();

    boolean claim(UUID uuid) {
        return claimed.add(uuid);
    }

    boolean hasClaimed(UUID uuid) {
        return claimed.contains(uuid);
    }

    boolean reset(UUID uuid) {
        return claimed.remove(uuid);
    }

    Set<UUID> all() {
        return Set.copyOf(claimed);
    }

    void loadAll(Iterable<UUID> uuids) {
        claimed.clear();
        for (UUID uuid : uuids) {
            claimed.add(uuid);
        }
    }
}
