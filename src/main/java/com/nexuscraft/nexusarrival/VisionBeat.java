package com.nexuscraft.nexusarrival;

import org.bukkit.Particle;
import org.bukkit.Sound;

/** One scheduled moment inside a {@link VisionDefinition} -- a particle burst, a sound, a
 *  cosmetic lightning flash, or a chat line, fired {@code delayTicks} after the vision starts.
 *  Deliberately one flat class rather than a type hierarchy: config.yml's beats are small,
 *  uniform-shaped maps and a single defensively-parsed record of nullable fields is far easier
 *  to load and validate than juggling five separate POJOs (same reasoning NexusMenu's MenuEntry
 *  used for its five entry types). */
final class VisionBeat {

    enum Type { PARTICLE, SOUND, LIGHTNING, MESSAGE }

    final Type type;
    final long delayTicks;

    // PARTICLE
    final Particle particle;
    final int count;
    final double offsetX;
    final double offsetY;
    final double offsetZ;

    // SOUND
    final Sound sound;
    final float volume;
    final float pitch;

    // MESSAGE
    final String text;

    private VisionBeat(Type type, long delayTicks, Particle particle, int count, double offsetX, double offsetY,
                        double offsetZ, Sound sound, float volume, float pitch, String text) {
        this.type = type;
        this.delayTicks = delayTicks;
        this.particle = particle;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.text = text;
    }

    static VisionBeat particle(long delayTicks, Particle particle, int count, double offsetX, double offsetY, double offsetZ) {
        return new VisionBeat(Type.PARTICLE, delayTicks, particle, count, offsetX, offsetY, offsetZ,
                null, 0f, 0f, null);
    }

    static VisionBeat sound(long delayTicks, Sound sound, float volume, float pitch) {
        return new VisionBeat(Type.SOUND, delayTicks, null, 0, 0, 0, 0, sound, volume, pitch, null);
    }

    static VisionBeat lightning(long delayTicks) {
        return new VisionBeat(Type.LIGHTNING, delayTicks, null, 0, 0, 0, 0, null, 0f, 0f, null);
    }

    static VisionBeat message(long delayTicks, String text) {
        return new VisionBeat(Type.MESSAGE, delayTicks, null, 0, 0, 0, 0, null, 0f, 0f, text);
    }
}
