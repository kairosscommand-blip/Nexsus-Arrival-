package com.nexuscraft.nexusarrival;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /nexusarrival trigger|status|reload} -- admin-only (also double-checked here, not just
 *  left to plugin.yml's permission default, same belt-and-suspenders convention this whole
 *  family follows). Mainly a testing/ops tool: a server owner previewing a new vision they just
 *  added to config.yml doesn't want to have to actually wait out the session-time curve to see it. */
final class ArrivalCommand implements CommandExecutor {

    private final ArrivalConfig config;
    private final VisionRegistry registry;
    private final VisionEngine engine;
    private final SessionTracker sessions;
    private final ArrivalClaimRegistry claims;
    private final Runnable reload;

    ArrivalCommand(ArrivalConfig config, VisionRegistry registry, VisionEngine engine, SessionTracker sessions,
                   ArrivalClaimRegistry claims, Runnable reload) {
        this.config = config;
        this.registry = registry;
        this.engine = engine;
        this.sessions = sessions;
        this.claims = claims;
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexusarrival.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§7Usage: /nexusarrival <trigger <visionId> [player]|status [player]|reload>");
            return true;
        }

        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "reload" -> {
                reload.run();
                sender.sendMessage("§aNexusArrival reloaded -- " + registry.size() + " vision(s) loaded.");
            }
            case "trigger" -> handleTrigger(sender, args);
            case "status" -> handleStatus(sender, args);
            default -> sender.sendMessage("§7Usage: /nexusarrival <trigger <visionId> [player]|status [player]|reload>");
        }
        return true;
    }

    private void handleTrigger(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nexusarrival trigger <visionId> [player]");
            return;
        }
        VisionDefinition vision = registry.get(args[1]);
        if (vision == null) {
            sender.sendMessage("§cNo vision named '" + args[1] + "' -- check config.yml.");
            return;
        }
        Player target = resolveTarget(sender, args, 2);
        if (target == null) {
            sender.sendMessage("§cCouldn't find that player online.");
            return;
        }
        engine.play(target, vision);
        sender.sendMessage("§aPlayed vision '" + vision.id + "' (tier " + vision.tier + ") to " + target.getName() + ".");
    }

    private void handleStatus(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 1);
        if (target == null) {
            sender.sendMessage("§cCouldn't find that player online.");
            return;
        }
        double minutes = sessions.sessionMinutes(target.getUniqueId());
        double chance = Math.min(config.maxChance, config.baseChance + config.chancePerMinute * minutes);
        boolean claimedArrival = claims.hasClaimed(target.getUniqueId());
        sender.sendMessage(String.format(java.util.Locale.ROOT,
                "§7%s -- session: §f%.1f min§7, current roll chance: §f%.0f%%§7, arrival vision claimed: §f%s",
                target.getName(), minutes, chance * 100, claimedArrival));
    }

    private Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            return Bukkit.getPlayerExact(args[index]);
        }
        return sender instanceof Player player ? player : null;
    }
}
