package com.nexuscraft.nexusarrival;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Starts session tracking on every join (so the escalation curve always has a real join time to
 *  measure from) and, separately, hands out the guaranteed once-ever arrival vision the first
 *  time a given player is ever seen -- whether that's their very first join ever, or their first
 *  join after this plugin was installed on an already-populated server. Either way {@link
 *  ArrivalClaimRegistry#claim} only ever lets it through once. */
final class ArrivalJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final ArrivalConfig config;
    private final VisionRegistry registry;
    private final VisionEngine engine;
    private final SessionTracker sessions;
    private final ArrivalClaimRegistry claims;
    private final Runnable onClaimsChanged;

    ArrivalJoinListener(JavaPlugin plugin, ArrivalConfig config, VisionRegistry registry, VisionEngine engine,
                         SessionTracker sessions, ArrivalClaimRegistry claims, Runnable onClaimsChanged) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.engine = engine;
        this.sessions = sessions;
        this.claims = claims;
        this.onClaimsChanged = onClaimsChanged;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessions.onJoin(player.getUniqueId());

        if (!config.arrivalEnabled || claims.hasClaimed(player.getUniqueId())) {
            return;
        }
        long delayTicks = Math.max(0, config.arrivalDelaySeconds) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> giveArrivalVision(player), delayTicks);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.onQuit(event.getPlayer().getUniqueId());
    }

    private void giveArrivalVision(Player player) {
        if (!player.isOnline() || claims.hasClaimed(player.getUniqueId())) {
            return;
        }
        VisionDefinition vision = registry.get(config.arrivalVisionId);
        if (vision == null) {
            plugin.getLogger().warning("nexusarrival: arrival-vision.vision-id '" + config.arrivalVisionId
                    + "' doesn't match any vision in config.yml -- nothing played, but the claim is still "
                    + "recorded so this player won't get stuck retrying it every join.");
            claims.claim(player.getUniqueId());
            onClaimsChanged.run();
            return;
        }
        if (config.arrivalMessage != null && !config.arrivalMessage.isBlank()) {
            player.sendMessage(ColorCodes.translate(config.arrivalMessage));
        }
        engine.play(player, vision);
        claims.claim(player.getUniqueId());
        onClaimsChanged.run();
    }
}
