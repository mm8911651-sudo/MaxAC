/*
 * This file is part of MaxAC - AI powered Anti-Cheat
 * Copyright (C) 2026 maxson10
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

package space.max.server;
import org.bukkit.Bukkit;
import space.max.Main;
import space.max.config.Config;
import space.max.config.ServerType;
import space.max.signalr.SignalRClient;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
public class AIClientProvider {
    private final Main plugin;
    private final Logger logger;
    private IAIClient currentClient;
    private Config config;
    private volatile boolean connecting = false;
    private volatile String clientType = "none";
    public AIClientProvider(Main plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }
    public CompletableFuture<Boolean> initialize() {
        if (!config.isAiEnabled()) {
            plugin.debug("[AI] AI is disabled, skipping client initialization");
            return CompletableFuture.completedFuture(false);
        }
        ServerType serverType = config.getServerType();
        String serverAddress = config.getServerAddress();
        String apiKey = normalizeApiKey(config.getAiApiKey());
        if (serverAddress == null || serverAddress.isEmpty()) {
            logger.warning("[AI] Endpoint / server address is not configured!");
            return CompletableFuture.completedFuture(false);
        }
        if (serverType == ServerType.SIGNALR && (apiKey == null || apiKey.isEmpty())) {
            logger.warning("[AI] API key is required for SignalR server!");
            return CompletableFuture.completedFuture(false);
        }
        logger.info("[AI] Server type: " + serverType + ", endpoint: " + serverAddress);
        connecting = true;
        if (serverType == ServerType.HUGGINGFACE) {
            return initializeHuggingFace(serverAddress, apiKey);
        }
        return initializeSignalR(serverAddress, apiKey);
    }

    private CompletableFuture<Boolean> initializeHuggingFace(String modelId, String token) {
        HuggingFaceClient hfClient = new HuggingFaceClient(modelId, token);
        this.currentClient = hfClient;
        this.clientType = "HuggingFace";
        logger.info("[HuggingFace] Using model " + modelId + (token != null && !token.isEmpty() ? " (private)" : " (public)"));
        return hfClient.connect()
            .thenApply(success -> {
                connecting = false;
                if (success) {
                    logger.info("[HuggingFace] Ready for inference");
                } else {
                    currentClient = null;
                    clientType = "none";
                }
                return success;
            })
            .exceptionally(e -> {
                connecting = false;
                logger.severe("[HuggingFace] Init error: " + e.getMessage());
                currentClient = null;
                clientType = "none";
                return false;
            });
    }
    private CompletableFuture<Boolean> initializeSignalR(String serverAddress, String apiKey) {
        SignalRClient signalRClient = new SignalRClient(
            plugin,
            serverAddress,
            apiKey,
            config.getReportStatsIntervalSeconds(),
            () -> Bukkit.getOnlinePlayers().size(),
            config.isDebug()
        );
        this.currentClient = signalRClient;
        this.clientType = "SignalR";
        logger.info("[SignalR] Connecting to " + serverAddress + "...");
        return signalRClient.connectWithRetry()
            .thenApply(success -> {
                connecting = false;
                if (success) {
                    logger.info("[SignalR] Successfully connected to InferenceServer");
                } else {
                    logger.warning("[SignalR] Failed to connect to InferenceServer");
                    currentClient = null;
                    clientType = "none";
                }
                return success;
            })
            .exceptionally(e -> {
                connecting = false;
                logger.severe("[SignalR] Connection error: " + e.getMessage());
                currentClient = null;
                clientType = "none";
                return false;
            });
    }
    public CompletableFuture<Void> shutdown() {
        if (currentClient != null) {
            logger.info("[AI] Shutting down " + clientType + " client...");
            return currentClient.disconnect()
                .thenRun(() -> {
                    currentClient = null;
                    clientType = "none";
                    logger.info("[AI] Client shutdown complete");
                });
        }
        return CompletableFuture.completedFuture(null);
    }
    public CompletableFuture<Boolean> reload() {
        return shutdown().thenCompose(v -> initialize());
    }
    public void setConfig(Config config) {
        this.config = config;
    }
    public IAIClient get() {
        return currentClient;
    }
    public boolean isAvailable() {
        return currentClient != null && currentClient.isConnected();
    }
    public boolean isEnabled() {
        return config.isAiEnabled();
    }
    public boolean isConnecting() {
        return connecting;
    }
    public boolean isLimitExceeded() {
        return currentClient != null && currentClient.isLimitExceeded();
    }
    public String getClientType() {
        return clientType;
    }

    private static String normalizeApiKey(String key) {
        if (key == null) return "";
        String t = key.trim();
        if (t.isEmpty() || "your-api-key".equalsIgnoreCase(t)) return "";
        return t;
    }
}