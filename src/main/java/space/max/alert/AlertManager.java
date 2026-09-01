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

package space.max.alert;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import space.max.Permissions;
import space.max.config.Config;
import space.max.scheduler.SchedulerAdapter;
import space.max.scheduler.SchedulerManager;
import space.max.util.ColorUtil;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public class AlertManager {
    private final Logger logger;
    // Tracks players who DISABLED alerts (by default all with permission receive alerts)
    private final Set<UUID> alertsDisabled;
    private final SchedulerAdapter scheduler;
    private Config config;

    public AlertManager(org.bukkit.plugin.java.JavaPlugin plugin, Config config) {
        this.config = config;
        this.logger = plugin.getLogger();
        this.alertsDisabled = new CopyOnWriteArraySet<>();
        this.scheduler = SchedulerManager.getAdapter();
    }

    private String getPrefix() {
        return ColorUtil.colorize(config.getPrefix());
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * Toggle alerts for a player. Returns true if alerts are now ENABLED, false if DISABLED.
     */
    public boolean toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (alertsDisabled.contains(uuid)) {
            // Was disabled, now enable
            alertsDisabled.remove(uuid);
            String msg = ColorUtil.colorize(config.getMessage("alerts-enabled"));
            player.sendMessage(getPrefix() + msg);
            return true;
        } else {
            // Was enabled (default), now disable
            alertsDisabled.add(uuid);
            String msg = ColorUtil.colorize(config.getMessage("alerts-disabled"));
            player.sendMessage(getPrefix() + msg);
            return false;
        }
    }

    public void enableAlerts(Player player) {
        alertsDisabled.remove(player.getUniqueId());
    }

    public void disableAlerts(Player player) {
        alertsDisabled.add(player.getUniqueId());
    }

    public boolean hasAlertsEnabled(Player player) {
        return !alertsDisabled.contains(player.getUniqueId());
    }

    private boolean canReceiveAlerts(Player player) {
        if (alertsDisabled.contains(player.getUniqueId())) {
            return false;
        }
        return player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN);
    }

    public void sendAlert(String suspectName, double probability, double buffer) {
        String message = formatAlertMessage(suspectName, probability, buffer);
        scheduler.runSync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (canReceiveAlerts(player)) {
                    player.sendMessage(message);
                }
            }
            if (config.isAiConsoleAlerts()) {
                logger.info(ColorUtil.stripColors(message));
            }
        });
    }

    public void sendAlert(String suspectName, double probability, double buffer, int vl) {
        String message = formatAlertMessage(suspectName, probability, buffer, vl);
        scheduler.runSync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (canReceiveAlerts(player)) {
                    player.sendMessage(message);
                }
            }
            if (config.isAiConsoleAlerts()) {
                logger.info(ColorUtil.stripColors(message));
            }
        });
    }

    private String formatAlertMessage(String suspectName, double probability, double buffer) {
        String template = config.getMessage("alert-format", suspectName, probability, buffer, 0);
        return getPrefix() + ColorUtil.colorize(template);
    }

    private String formatAlertMessage(String suspectName, double probability, double buffer, int vl) {
        String template = config.getMessage("alert-format-vl", suspectName, probability, buffer, vl);
        return getPrefix() + ColorUtil.colorize(template);
    }

    public void handlePlayerQuit(Player player) {
        // Don't remove from disabled set - preserve preference across sessions
    }

    public boolean shouldAlert(double probability) {
        return probability >= config.getAiAlertThreshold();
    }

    public double getAlertThreshold() {
        return config.getAiAlertThreshold();
    }
}
