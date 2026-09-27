package com.nexuscraft.nexusarrival;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** The ongoing "gets crazier the longer you've been on" system: one global repeating task rolls,
 *  per online player, whether a random vision fires right now. Both halves of the escalation
 *  live here:
 *  <ul>
 *    <li>the ROLL ITSELF gets more likely the longer this session has run
 *        ({@code base-chance + chance-per-minute * sessionMinutes}, capped at {@code max-chance}),</li>
 *    <li>and which TIER is even eligible to be picked keeps expanding as session time passes
 *        (each {@link TierCurve} only enters the pool once {@code minSessionMinutes} is reached),
 *        so the wildest visions are literally unreachable early in a session no matter how lucky
 *        the roll is.</li>
 *  </ul>
 *  A per-player cooldown ({@code min-seconds-between-visions}) keeps two visions from ever
 *  stacking back to back. */
final class SessionVisionScheduler {

    private final JavaPlugin plugin;
    private final ArrivalConfig config;
    private final VisionRegistry registry;
    private final VisionEngine engine;
    private final SessionTracker sessions;

    private BukkitTask task;

    SessionVisionScheduler(JavaPlugin plugin, ArrivalConfig config, VisionRegistry registry,
                            VisionEngine engine, SessionTracker sessions) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.engine = engine;
        this.sessions = sessions;
    }

    void start() {
        stop();
        if (!config.sessionEnabled) {
            return;
        }
        long periodTicks = config.checkIntervalSeconds * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, periodTicks, periodTicks);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            considerFor(player);
        }
    }

    private void considerFor(Player player) {
        if (!sessions.isPastCooldown(player.getUniqueId(), config.minSecondsBetweenVisions)) {
            return;
        }
        double minutes = sessions.sessionMinutes(player.getUniqueId());
        double chance = Math.min(config.maxChance, config.baseChance + config.chancePerMinute * minutes);
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        TierCurve tier = pickTier(minutes);
        if (tier == null) {
            return;
        }
        List<VisionDefinition> candidates = registry.ofTier(tier.id);
        if (candidates.isEmpty()) {
            return;
        }
        VisionDefinition vision = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        sessions.markVisionPlayed(player.getUniqueId());
        engine.play(player, vision);
    }

    /** Weighted-random pick among every tier whose {@code minSessionMinutes} threshold this
     *  session has already crossed. Null only if no tier is configured or none is eligible yet
     *  (i.e. every configured tier requires more session time than the player currently has). */
    private TierCurve pickTier(double sessionMinutes) {
        List<TierCurve> eligible = config.tiers.stream()
                .filter(t -> sessionMinutes >= t.minSessionMinutes)
                .toList();
        if (eligible.isEmpty()) {
            return null;
        }
        double totalWeight = eligible.stream().mapToDouble(t -> t.weight).sum();
        double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0;
        for (TierCurve tier : eligible) {
            cumulative += tier.weight;
            if (roll < cumulative) {
                return tier;
            }
        }
        return eligible.get(eligible.size() - 1);
    }
}
