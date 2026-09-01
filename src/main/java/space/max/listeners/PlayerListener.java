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

package space.max.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import space.max.Main;
import space.max.Permissions;
import space.max.alert.AlertManager;
import space.max.checks.AICheck;
import space.max.commands.CommandHandler;
import space.max.scheduler.SchedulerManager;
import space.max.session.SessionManager;
import space.max.violation.ViolationManager;

public class PlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final AICheck aiCheck;
    private final AlertManager alertManager;
    private final ViolationManager violationManager;
    private final SessionManager sessionManager;
    private HitListener hitListener;
    private final CommandHandler commandHandler;

    public PlayerListener(JavaPlugin plugin, AICheck aiCheck, AlertManager alertManager,
                          ViolationManager violationManager, SessionManager sessionManager,
                          CommandHandler commandHandler) {
        this.plugin = plugin;
        this.aiCheck = aiCheck;
        this.alertManager = alertManager;
        this.violationManager = violationManager;
        this.sessionManager = sessionManager;
        this.commandHandler = commandHandler;
    }

    public void setHitListener(HitListener hitListener) {
        this.hitListener = hitListener;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hitListener != null) {
            hitListener.cacheEntity(player);
        }

        // Восстановление состояния наблюдателя, если игрок вышел во время наблюдения
        if (commandHandler != null) {
            commandHandler.handlePlayerJoin(player);
        }

        // Проверка обновлений
        try {
            SchedulerManager.getAdapter().runSyncDelayed(() -> {
                if (player.isOnline()) {
                    if (player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN)) {
                        if (plugin instanceof Main) {
                            Main main = (Main) plugin;
                            if (main.getUpdateChecker() != null && main.getUpdateChecker().isUpdateAvailable()) {
                                player.sendMessage(ChatColor.GOLD + "=================================================");
                                player.sendMessage(ChatColor.YELLOW + "A NEW MaxAC UPDATE IS AVAILABLE: " + ChatColor.WHITE + main.getUpdateChecker().getLatestVersion());
                                player.sendMessage(ChatColor.YELLOW + "Get it from GitHub: " + ChatColor.AQUA + main.getUpdateChecker().getReleasesUrl());
                                player.sendMessage(ChatColor.GOLD + "=================================================");
                            }
                        }
                    }
                }
            }, 20L);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule player join task: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    private void handlePlayerLeave(Player player) {
        if (hitListener != null) {
            hitListener.uncachePlayer(player);
        }
        if (aiCheck != null) {
            aiCheck.handlePlayerQuit(player);
        }
        if (alertManager != null) {
            alertManager.handlePlayerQuit(player);
        }
        if (violationManager != null) {
            violationManager.handlePlayerQuit(player);
        }
        if (sessionManager != null) {
            sessionManager.removeAimProcessor(player.getUniqueId());
        }
        // Останавливаем трекинг и удаляем голограммы
        if (commandHandler != null) {
            commandHandler.handlePlayerQuit(player);
        }
    }
}