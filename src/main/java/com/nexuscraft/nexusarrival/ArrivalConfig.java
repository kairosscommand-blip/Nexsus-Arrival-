package com.nexuscraft.nexusarrival;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Parses config.yml's behavior toggles -- everything except the vision pool itself, which
 *  {@link VisionRegistry} owns. Same "config drives defaults, code never hardcodes them" split
 *  this whole Nexus family uses (compare NexusStarter's StarterConfig, NexusMenu's MenuConfig). */
final class ArrivalConfig {

    private final Plugin plugin;

    boolean arrivalEnabled = true;
    int arrivalDelaySeconds = 3;
    String arrivalVisionId = "mosh_pit";
    String arrivalMessage = "&d&lSomething stirs as you step through...";

    boolean sessionEnabled = true;
    int checkIntervalSeconds = 30;
    int minSecondsBetweenVisions = 75;
    double baseChance = 0.10;
    double chancePerMinute = 0.012;
    double maxChance = 0.55;
    List<TierCurve> tiers = new ArrayList<>();

    ArrivalConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    void load(Logger log) {
        FileConfiguration c = plugin.getConfig();

        ConfigurationSection arrival = c.getConfigurationSection("arrival-vision");
        if (arrival != null) {
            arrivalEnabled = arrival.getBoolean("enabled", arrivalEnabled);
            arrivalDelaySeconds = Math.max(0, arrival.getInt("delay-seconds", arrivalDelaySeconds));
            arrivalVisionId = arrival.getString("vision-id", arrivalVisionId);
            arrivalMessage = arrival.getString("message", arrivalMessage);
        }

        ConfigurationSection session = c.getConfigurationSection("session");
        if (session != null) {
            sessionEnabled = session.getBoolean("enabled", sessionEnabled);
            checkIntervalSeconds = Math.max(5, session.getInt("check-interval-seconds", checkIntervalSeconds));
            minSecondsBetweenVisions = Math.max(0, session.getInt("min-seconds-between-visions", minSecondsBetweenVisions));
            baseChance = clamp01(session.getDouble("base-chance", baseChance));
            chancePerMinute = Math.max(0, session.getDouble("chance-per-minute", chancePerMinute));
            maxChance = clamp01(session.getDouble("max-chance", maxChance));

            tiers = new ArrayList<>();
            ConfigurationSection tierSection = session.getConfigurationSection("tiers");
            if (tierSection != null) {
                for (String tierId : tierSection.getKeys(false)) {
                    ConfigurationSection t = tierSection.getConfigurationSection(tierId);
                    if (t == null) {
                        continue;
                    }
                    double minMinutes = t.getDouble("min-session-minutes", 0);
                    double weight = t.getDouble("weight", 0);
                    if (weight <= 0) {
                        log.warning("nexusarrival: tier '" + tierId + "' has a non-positive weight -- skipped.");
                        continue;
                    }
                    tiers.add(new TierCurve(tierId, minMinutes, weight));
                }
            }
            if (tiers.isEmpty()) {
                log.warning("nexusarrival: no valid session.tiers configured -- the ongoing vision system will "
                        + "never fire anything (arrival-vision is unaffected).");
            }
        }
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
