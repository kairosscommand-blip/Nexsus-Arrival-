package com.nexuscraft.nexusarrival;

/** Translates the '&amp;' color-code shorthand config.yml uses into real section-sign codes.
 *  Same convention as every other plugin in this family (see NexusStarter's ColorCodes). */
final class ColorCodes {

    private ColorCodes() {
    }

    static String translate(String raw) {
        return raw == null ? null : raw.replace('&', '§');
    }
}
