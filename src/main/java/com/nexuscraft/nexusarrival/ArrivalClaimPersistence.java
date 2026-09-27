package com.nexuscraft.nexusarrival;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** YAML round trip for {@link ArrivalClaimRegistry} -- a flat list of UUID strings under a
 *  {@code claimed} key. Same "small, boring, explicit" persistence shape this whole plugin
 *  family uses (compare NexusStarter's GuideItemClaimPersistence). */
final class ArrivalClaimPersistence {

    private ArrivalClaimPersistence() {
    }

    static List<UUID> loadFrom(ConfigurationSection section) {
        List<UUID> uuids = new ArrayList<>();
        if (section == null) {
            return uuids;
        }
        for (String raw : section.getStringList("claimed")) {
            try {
                uuids.add(UUID.fromString(raw));
            } catch (IllegalArgumentException malformed) {
                // Skip a corrupted/hand-edited entry rather than failing the whole load.
            }
        }
        return uuids;
    }

    static void saveTo(ConfigurationSection section, List<UUID> uuids) {
        List<String> raw = new ArrayList<>();
        for (UUID uuid : uuids) {
            raw.add(uuid.toString());
        }
        section.set("claimed", raw);
    }
}
