package com.nexuscraft.nexusarrival;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class NexusArrivalPlugin extends JavaPlugin {

    private ArrivalConfig config;
    private final VisionRegistry registry = new VisionRegistry();
    private VisionEngine engine;
    private final SessionTracker sessions = new SessionTracker();
    private final ArrivalClaimRegistry claims = new ArrivalClaimRegistry();
    private SessionVisionScheduler scheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = new ArrivalConfig(this);
        config.load(getLogger());
        registry.load(getConfig(), getLogger());
        engine = new VisionEngine(this);

        loadClaims();

        getServer().getPluginManager().registerEvents(
                new ArrivalJoinListener(this, config, registry, engine, sessions, claims, this::saveClaims), this);

        getCommand("nexusarrival").setExecutor(
                new ArrivalCommand(config, registry, engine, sessions, claims, this::reload));

        scheduler = new SessionVisionScheduler(this, config, registry, engine, sessions);
        scheduler.start();

        getLogger().info("NexusArrival enabled -- " + registry.size() + " vision(s) loaded, "
                + claims.all().size() + " arrival claim(s) on file.");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.stop();
        }
        saveClaims();
        getLogger().info("NexusArrival disabled.");
    }

    private void reload() {
        reloadConfig();
        config.load(getLogger());
        registry.load(getConfig(), getLogger());
        scheduler.start();
    }

    private void loadClaims() {
        File file = new File(getDataFolder(), "arrival_claims.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<UUID> loaded = ArrivalClaimPersistence.loadFrom(data);
        claims.loadAll(loaded);
    }

    private void saveClaims() {
        getDataFolder().mkdirs();
        YamlConfiguration data = new YamlConfiguration();
        ArrivalClaimPersistence.saveTo(data, List.copyOf(claims.all()));
        try {
            data.save(new File(getDataFolder(), "arrival_claims.yml"));
        } catch (IOException e) {
            getLogger().warning("Failed to save arrival_claims.yml: " + e.getMessage());
        }
    }
}
