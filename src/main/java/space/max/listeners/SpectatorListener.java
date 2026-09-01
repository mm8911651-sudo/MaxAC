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

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import space.max.commands.CommandHandler;

import java.util.UUID;

public class SpectatorListener implements Listener {

    private static final double MAX_DISTANCE = 30.0;

    private final CommandHandler commandHandler;

    public SpectatorListener(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player spectator = event.getPlayer();
        if (spectator.getGameMode() != GameMode.SPECTATOR) {
            return;
        }

        UUID targetId = commandHandler.getSpectatorTarget(spectator.getUniqueId());
        if (targetId == null) {
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            return;
        }

        if (spectator.getWorld() != target.getWorld()) {
            return;
        }

        double distance = spectator.getLocation().distance(target.getLocation());
        if (distance > MAX_DISTANCE) {
            spectator.teleport(target);
        }
    }
}