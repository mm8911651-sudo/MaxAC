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

package space.max.penalty.handlers;

import org.bukkit.plugin.java.JavaPlugin;
import space.max.alert.AlertManager;
import space.max.penalty.ActionHandler;
import space.max.penalty.ActionType;
import space.max.penalty.PenaltyContext;

import java.util.Set;
import java.util.UUID;

public class AlertHandler implements ActionHandler {

    public AlertHandler(JavaPlugin plugin, AlertManager alertManager) {
        // Кастомные алерты полностью отключены
    }

    public void setAlertRecipients(Set<UUID> recipients) {
        // Ничего не делаем
    }

    public void setConsoleAlerts(boolean enabled) {
        // Ничего не делаем
    }

    /**
     * Пустой метод для совместимости с PenaltyExecutor.
     * Кастомные алерты отключены, поэтому префикс не используется.
     */
    public void setAlertPrefix(String prefix) {
        // Ничего не делаем
    }

    @Override
    public void handle(String message, PenaltyContext context) {
        // Пустая реализация: кастомные алерты не отправляются
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CUSTOM_ALERT;
    }
}