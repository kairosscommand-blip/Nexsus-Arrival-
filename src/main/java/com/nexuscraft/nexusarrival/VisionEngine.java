package com.nexuscraft.nexusarrival;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Plays a {@link VisionDefinition} to one specific player -- every beat targets that player
 *  alone (Player#spawnParticle / Player#playSound are per-client; nothing here is visible or
 *  audible to anyone standing next to them, and nothing here is a real entity, so it can safely
 *  play in a 3-block spawn room just as well as an open field). Each beat is scheduled as its
 *  own {@code runTaskLater} relative to when the vision starts, and every beat re-checks that
 *  the player is still online before touching them, since a vision can span several seconds and
 *  a player is always free to log out mid-illusion. */
final class VisionEngine {

    private final JavaPlugin plugin;

    VisionEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void play(Player player, VisionDefinition vision) {
        for (VisionBeat beat : vision.beats) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> runBeat(player, beat), beat.delayTicks);
        }
    }

    private void runBeat(Player player, VisionBeat beat) {
        if (!player.isOnline()) {
            return;
        }
        Location at = player.getLocation();
        switch (beat.type) {
            case PARTICLE -> player.spawnParticle(beat.particle, at, beat.count, beat.offsetX, beat.offsetY, beat.offsetZ);
            case SOUND -> player.playSound(at, beat.sound, beat.volume, beat.pitch);
            case LIGHTNING -> {
                // Deliberately NOT World#strikeLightningEffect: that's a real-world visual/sound
                // effect broadcast to every nearby player, which would break this whole system's
                // "private, only-this-player" promise. A per-player flash (Player#spawnParticle /
                // Player#playSound both send to one client only) fakes the same beat honestly.
                player.spawnParticle(Particle.FLASH, at.clone().add(0, 2.0, 0), 1);
                player.spawnParticle(Particle.ELECTRIC_SPARK, at, 40, 1.0, 1.5, 1.0);
                player.playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
            }
            case MESSAGE -> player.sendMessage(beat.text);
        }
    }
}
