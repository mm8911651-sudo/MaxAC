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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import space.max.penalty.ActionHandler;
import space.max.penalty.ActionType;
import space.max.penalty.BanAnimation;
import space.max.penalty.PenaltyContext;
import space.max.scheduler.SchedulerManager;

public class BanHandler implements ActionHandler {
    private final BanAnimation animation;
    private boolean animationEnabled = true;

    public BanHandler(JavaPlugin plugin) {
        this.animation = new BanAnimation(plugin);
    }

    @Override
    public void handle(String command, PenaltyContext context) {
        if (command == null || command.isEmpty()) {
            return;
        }
        Player player = null;
        if (context != null && context.getPlayerName() != null) {
            player = Bukkit.getPlayer(context.getPlayerName());
        }
        if (!animationEnabled || player == null || !player.isOnline()) {
            SchedulerManager.getAdapter().runSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
            return;
        }
        animation.playAnimation(player, command, context);
    }

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public BanAnimation getAnimation() {
        return animation;
    }

    public void shutdown() {
        animation.shutdown();
    }

    @Override
    public ActionType getActionType() {
        return ActionType.BAN;
    }
}
