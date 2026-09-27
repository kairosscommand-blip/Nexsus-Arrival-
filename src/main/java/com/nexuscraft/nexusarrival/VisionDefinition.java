package com.nexuscraft.nexusarrival;

import java.util.List;

/** One playable vision -- an id (for {@code arrival-vision.vision-id} and {@code /nexusarrival
 *  trigger}), a tier (which gates when it's eligible to fire on its own, see {@link
 *  SessionVisionScheduler}), and its ordered list of {@link VisionBeat}s. */
final class VisionDefinition {

    final String id;
    final String tier;
    final List<VisionBeat> beats;

    VisionDefinition(String id, String tier, List<VisionBeat> beats) {
        this.id = id;
        this.tier = tier;
        this.beats = List.copyOf(beats);
    }
}
