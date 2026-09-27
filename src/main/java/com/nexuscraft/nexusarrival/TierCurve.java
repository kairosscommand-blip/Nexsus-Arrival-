package com.nexuscraft.nexusarrival;

/** One entry of {@code session.tiers} -- a tier only becomes eligible to fire on its own once a
 *  player's current session has run for at least {@code minSessionMinutes}, and among eligible
 *  tiers one is picked by weighted random using {@code weight}. This is the whole "gets crazier
 *  the longer you've been on" mechanic: early in a session only the low-threshold tiers qualify,
 *  and the higher, wilder tiers only enter the pool once enough session time has passed. */
final class TierCurve {

    final String id;
    final double minSessionMinutes;
    final double weight;

    TierCurve(String id, double minSessionMinutes, double weight) {
        this.id = id;
        this.minSessionMinutes = minSessionMinutes;
        this.weight = weight;
    }
}
