package com.nexuscraft.nexusarrival;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Loads config.yml's {@code visions:} pool into {@link VisionDefinition}s. Every malformed
 *  entry (unknown enum name, missing field, wrong shape) is skipped with a logged warning rather
 *  than crashing the whole load -- same defensive-parsing convention as NexusMenu's MenuConfig,
 *  since this file is meant to be hand-edited by the server owner to add more visions over time. */
final class VisionRegistry {

    private final Map<String, VisionDefinition> byId = new LinkedHashMap<>();

    void load(FileConfiguration config, Logger log) {
        byId.clear();
        List<Map<?, ?>> rawVisions = config.getMapList("visions");
        for (Map<?, ?> rawVision : rawVisions) {
            String id = str(rawVision, "id", null);
            String tier = str(rawVision, "tier", null);
            if (id == null || id.isBlank() || tier == null || tier.isBlank()) {
                log.warning("nexusarrival: skipped a vision entry missing 'id' or 'tier'.");
                continue;
            }
            Object rawBeats = rawVision.get("beats");
            if (!(rawBeats instanceof List<?> beatList)) {
                log.warning("nexusarrival: vision '" + id + "' has no 'beats' list -- skipped.");
                continue;
            }
            List<VisionBeat> beats = new ArrayList<>();
            for (Object rawBeat : beatList) {
                if (!(rawBeat instanceof Map<?, ?> beatMap)) {
                    log.warning("nexusarrival: vision '" + id + "' has a malformed beat entry -- skipped it.");
                    continue;
                }
                VisionBeat beat = parseBeat(id, beatMap, log);
                if (beat != null) {
                    beats.add(beat);
                }
            }
            if (beats.isEmpty()) {
                log.warning("nexusarrival: vision '" + id + "' has no valid beats -- skipped entirely.");
                continue;
            }
            byId.put(id, new VisionDefinition(id, tier, beats));
        }
    }

    private VisionBeat parseBeat(String visionId, Map<?, ?> beat, Logger log) {
        String rawType = str(beat, "type", "");
        long delay = Math.max(0, longVal(beat, "delay-ticks", 0));
        switch (rawType.toLowerCase(java.util.Locale.ROOT)) {
            case "particle" -> {
                Particle particle = VisionRegistry.<Particle>enumOf(Particle.class, str(beat, "particle", null));
                if (particle == null) {
                    log.warning("nexusarrival: vision '" + visionId + "' has a particle beat with an unknown/missing "
                            + "'particle' name -- skipped.");
                    return null;
                }
                int count = (int) longVal(beat, "count", 10);
                double ox = dbl(beat, "offset-x", 0.5);
                double oy = dbl(beat, "offset-y", 0.8);
                double oz = dbl(beat, "offset-z", 0.5);
                return VisionBeat.particle(delay, particle, count, ox, oy, oz);
            }
            case "sound" -> {
                Sound sound = VisionRegistry.<Sound>enumOf(Sound.class, str(beat, "sound", null));
                if (sound == null) {
                    log.warning("nexusarrival: vision '" + visionId + "' has a sound beat with an unknown/missing "
                            + "'sound' name -- skipped.");
                    return null;
                }
                float volume = (float) dbl(beat, "volume", 1.0);
                float pitch = (float) dbl(beat, "pitch", 1.0);
                return VisionBeat.sound(delay, sound, volume, pitch);
            }
            case "lightning" -> {
                return VisionBeat.lightning(delay);
            }
            case "message" -> {
                String text = str(beat, "text", null);
                if (text == null) {
                    log.warning("nexusarrival: vision '" + visionId + "' has a message beat with no 'text' -- skipped.");
                    return null;
                }
                return VisionBeat.message(delay, ColorCodes.translate(text));
            }
            default -> {
                log.warning("nexusarrival: vision '" + visionId + "' has a beat with unknown type '" + rawType + "' -- skipped.");
                return null;
            }
        }
    }

    /** Both call sites pass an explicit {@code <E>} type witness (e.g. {@code
     *  VisionRegistry.<Sound>enumOf(...)}) rather than letting it be inferred from the assignment
     *  target. A real Maven build hit a javac generic-inference failure ("inference variable E
     *  has incompatible bounds") on the plain, uninferred call for {@code Sound} specifically --
     *  this project's own sandbox javac (21.0.10) doesn't reproduce it even with {@code Sound}
     *  modeled as implementing {@link Keyed} (a real difference between Sound and Particle, see
     *  Keyed.java), so the exact trigger looks like a javac version difference rather than
     *  anything wrong with this method itself. The explicit witness sidesteps the whole inference
     *  path and is a standard, safe fix for this class of error either way. */
    private static <E extends Enum<E>> E enumOf(Class<E> type, String name) {
        if (name == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String str(Map<?, ?> map, String key, String def) {
        Object value = map.get(key);
        return value == null ? def : String.valueOf(value);
    }

    private static long longVal(Map<?, ?> map, String key, long def) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return def;
    }

    private static double dbl(Map<?, ?> map, String key, double def) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return def;
    }

    VisionDefinition get(String id) {
        return byId.get(id);
    }

    List<VisionDefinition> ofTier(String tier) {
        List<VisionDefinition> result = new ArrayList<>();
        for (VisionDefinition def : byId.values()) {
            if (def.tier.equalsIgnoreCase(tier)) {
                result.add(def);
            }
        }
        return result;
    }

    List<VisionDefinition> all() {
        return List.copyOf(byId.values());
    }

    int size() {
        return byId.size();
    }
}
