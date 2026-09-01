/*
 * This file is part of MaxAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MaxAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file contains code derived from:
 *   - SlothAC © 2025 KaelusMC, https://github.com/KaelusMC/SlothAC
 *   - Grim © 2025 GrimAnticheat, https://github.com/GrimAnticheat/Grim
 *   - client-side © 2025 MLSAC, https://github.com/MLSAC/client-side/
 *   - x4yr ©  X4yrAC https://github.com/x4yr/X4yrAC
 * All derived code is licensed under GPL-3.0.
 */

package space.max;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import space.max.alert.AlertManager;
import space.max.checks.AICheck;
import space.max.checks.CheckHistoryManager;
import space.max.commands.CommandHandler;
import space.max.compat.VersionAdapter;
import space.max.config.Config;
import space.max.datacollector.DataCollectorFactory;
import space.max.listeners.ChecksGUIListener;
import space.max.listeners.HitListener;
import space.max.listeners.ListGUIListener;
import space.max.listeners.PlayerListener;
import space.max.listeners.RotationListener;
import space.max.listeners.SpectatorListener;
import space.max.listeners.TeleportListener;
import space.max.listeners.TickListener;
import space.max.placeholder.MaxACExpansion;
import space.max.scheduler.SchedulerManager;
import space.max.server.AIClientProvider;
import space.max.session.ISessionManager;
import space.max.session.SessionManager;
import space.max.util.FeatureCalculator;
import space.max.util.UpdateChecker;
import space.max.violation.ViolationManager;

import java.io.File;

public final class Main extends JavaPlugin {

    private Config config;
    private ISessionManager sessionManager;
    private FeatureCalculator featureCalculator;
    private TickListener tickListener;
    private HitListener hitListener;
    private RotationListener rotationListener;
    private PlayerListener playerListener;
    private TeleportListener teleportListener;
    private CommandHandler commandHandler;
    private AIClientProvider aiClientProvider;
    private AlertManager alertManager;
    private ViolationManager violationManager;
    private AICheck aiCheck;
    private UpdateChecker updateChecker;
    private CheckHistoryManager checkHistoryManager;
    private MaxACExpansion maxACExpansion;

    @Override
    public void onLoad() {
        VersionAdapter.init(getLogger());
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false)
                .bStats(false)
                .debug(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        try {
            SchedulerManager.initialize(this);
            getLogger().info("SchedulerManager initialized for " + SchedulerManager.getServerType());
        } catch (Exception e) {
            getLogger().severe("Failed to initialize SchedulerManager: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PacketEvents.getAPI().init();
        VersionAdapter.get().logCompatibilityInfo();

        saveDefaultConfig();
        this.config = new Config(this, getLogger());

        String outDir = config.getOutputDirectory();
        if (outDir == null || outDir.isEmpty()) outDir = "data";
        File outputDir = (new File(outDir).isAbsolute())
                ? new File(outDir)
                : new File(getDataFolder(), outDir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        this.featureCalculator = new FeatureCalculator();
        this.sessionManager = DataCollectorFactory.createSessionManager(this);

        this.checkHistoryManager = new CheckHistoryManager();

        this.aiClientProvider = new AIClientProvider(this, config);
        this.alertManager = new AlertManager(this, config);
        this.violationManager = new ViolationManager(this, config, alertManager);
        this.aiCheck = new AICheck(this, config, aiClientProvider, alertManager, violationManager, checkHistoryManager);
        this.violationManager.setAICheck(aiCheck);

        if (config.isAiEnabled()) {
            aiClientProvider.initialize().thenAccept(success -> {
                if (success) {
                    getLogger().info("[AI] Connected to " + config.getServerAddress());
                } else {
                    getLogger().warning("[AI] Failed to connect to inference server");
                }
            });
        }

        this.tickListener = new TickListener(this, sessionManager, aiCheck);
        this.hitListener = new HitListener(sessionManager, aiCheck);
        this.rotationListener = new RotationListener(sessionManager, aiCheck);
        this.teleportListener = new TeleportListener(aiCheck);

        this.commandHandler = new CommandHandler(sessionManager, alertManager, aiCheck, this, checkHistoryManager);
        this.commandHandler.loadSpectatorStates();

        this.playerListener = new PlayerListener(
                this,
                aiCheck,
                alertManager,
                violationManager,
                sessionManager instanceof SessionManager ? (SessionManager) sessionManager : null,
                commandHandler
        );

        this.tickListener.setHitListener(hitListener);
        this.playerListener.setHitListener(hitListener);
        this.hitListener.cacheOnlinePlayers();
        this.tickListener.start();

        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        PacketEvents.getAPI().getEventManager().registerListener(hitListener);
        PacketEvents.getAPI().getEventManager().registerListener(rotationListener);

        getServer().getPluginManager().registerEvents(new ChecksGUIListener(commandHandler), this);
        getServer().getPluginManager().registerEvents(new ListGUIListener(commandHandler), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(commandHandler), this);

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.maxACExpansion = new MaxACExpansion(this);
            this.maxACExpansion.register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        PluginCommand command = getCommand("mac");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }

        getLogger().info("MaxAC enabled successfully!");
        getLogger().info("Data collector: ENABLED (output: " + config.getOutputDirectory() + ")");
        if (config.isAiEnabled()) {
            getLogger().info("AI detection: ENABLED (threshold: " + config.getAiAlertThreshold() + ")");
        } else {
            getLogger().info("AI detection: DISABLED");
        }

        this.updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates().thenAccept(available -> {
            if (available) {
                getLogger().warning("=================================================");
                getLogger().warning("A NEW UPDATE IS AVAILABLE: " + updateChecker.getLatestVersion());
                getLogger().warning("Get it from GitHub: " + updateChecker.getReleasesUrl());
                getLogger().warning("=================================================");
            }
        });
    }

    @Override
    public void onDisable() {
        if (maxACExpansion != null) {
            maxACExpansion.unregister();
        }

        if (commandHandler != null) {
            commandHandler.saveSpectatorStates();
            commandHandler.cleanup();
        }

        if (tickListener != null) {
            tickListener.stop();
        }
        if (sessionManager != null) {
            getLogger().info("Stopping all active sessions...");
            sessionManager.stopAllSessions();
        }
        if (aiCheck != null) {
            aiCheck.clearAll();
        }
        if (violationManager != null) {
            violationManager.shutdown();
        }
        if (aiClientProvider != null && aiClientProvider.isAvailable()) {
            getLogger().info("Shutting down SignalR client...");
            try {
                aiClientProvider.shutdown().get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                getLogger().warning("Error shutting down SignalR client: " + e.getMessage());
            }
        }
        PacketEvents.getAPI().terminate();
        getLogger().info("MaxAC disabled successfully!");
    }

    public void reloadPluginConfig() {
        SchedulerManager.getAdapter().runSync(() -> {
            try {
                reloadConfig();
                this.config = new Config(this, getLogger());
                alertManager.setConfig(config);
                violationManager.setConfig(config);
                aiCheck.setConfig(config);
                if (aiClientProvider != null) {
                    aiClientProvider.setConfig(config);
                    if (config.isAiEnabled()) {
                        aiClientProvider.reload().thenAccept(success -> {
                            if (success) {
                                getLogger().info("[AI] Reconnected to " + config.getServerAddress());
                            }
                        });
                    } else {
                        aiClientProvider.shutdown();
                    }
                }
                getLogger().info("Configuration reloaded!");
            } catch (Exception e) {
                getLogger().severe("Failed to reload configuration: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public Config getPluginConfig() {
        return config;
    }

    public ISessionManager getSessionManager() {
        return sessionManager;
    }

    public FeatureCalculator getFeatureCalculator() {
        return featureCalculator;
    }

    public AICheck getAiCheck() {
        return aiCheck;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public ViolationManager getViolationManager() {
        return violationManager;
    }

    public AIClientProvider getAiClientProvider() {
        return aiClientProvider;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public CheckHistoryManager getCheckHistoryManager() {
        return checkHistoryManager;
    }

    public void debug(String message) {
        if (config != null && config.isDebug()) {
            getLogger().info("[Debug] " + message);
        }
    }
}