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

package space.max.penalty;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import space.max.compat.ParticleCompat;
import space.max.scheduler.ScheduledTask;
import space.max.scheduler.SchedulerManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BanAnimation implements Listener {

    // Animation timing (in ticks, 20 ticks = 1 second)
    private static final int LEVITATION_DURATION = 90;  // ~4.5 seconds
    private static final int ANIMATION_END_TICK = 80;   // When to finish animation
    private static final int STROBE_INTERVAL = 4;       // Flash every N ticks

    private final JavaPlugin plugin;
    private final Set<UUID> animatingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> pendingBans = new ConcurrentHashMap<>();

    public BanAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void playAnimation(Player player, String banCommand, PenaltyContext context) {
        if (player == null) return;
        if (!Bukkit.isPrimaryThread()) {
            SchedulerManager.getAdapter().runSync(() -> playAnimation(player, banCommand, context));
            return;
        }
        if (!player.isOnline()) {
            executeBanCommand(banCommand);
            return;
        }

        UUID playerId = player.getUniqueId();
        if (animatingPlayers.contains(playerId)) return;

        animatingPlayers.add(playerId);
        pendingBans.put(playerId, banCommand);

        Location origin = player.getLocation().clone();

        // Apply levitation
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, LEVITATION_DURATION, 1, false, false, false));
        player.setAllowFlight(false);
        player.setGliding(false);
        player.setFlying(false);
        player.setSwimming(false);

        final int[] ticks = {0};
        final ScheduledTask[] taskRef = new ScheduledTask[1];

        taskRef[0] = SchedulerManager.getAdapter().runSyncRepeating(() -> {
            try {
                // Player quit during animation - handled by event listener
                if (!player.isOnline()) {
                    taskRef[0].cancel();
                    return;
                }

                // Animation finished
                if (ticks[0] >= ANIMATION_END_TICK) {
                    finishAnimation(player, banCommand);
                    taskRef[0].cancel();
                    return;
                }

                // Keep player in place (X/Z locked)
                Location loc = player.getLocation();
                loc.setX(origin.getX());
                loc.setZ(origin.getZ());
                player.teleport(loc);

                // Strobe effect - white flash
                if (ticks[0] % STROBE_INTERVAL == 0) {
                    spawnStrobe(player);
                }

                ticks[0]++;
            } catch (Exception e) {
                plugin.getLogger().severe("Ban animation error: " + e.getMessage());
                taskRef[0].cancel();
                cleanup(player, banCommand);
            }
        }, 0L, 1L);
    }

    private void spawnStrobe(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Try FLASH particle (bright white flash)
        Particle flash = ParticleCompat.getParticle("FLASH");
        if (flash != null) {
            ParticleCompat.spawnParticle(player.getWorld(), flash, loc, 1, 0, 0, 0, 0);
            return;
        }
        
        // Fallback to END_ROD (white sparkles)
        Particle endRod = ParticleCompat.getParticle("END_ROD");
        if (endRod != null) {
            ParticleCompat.spawnParticle(player.getWorld(), endRod, loc, 15, 0.5, 0.5, 0.5, 0.05);
        }
    }

    private void finishAnimation(Player player, String banCommand) {
        UUID playerId = player.getUniqueId();
        
        animatingPlayers.remove(playerId);
        pendingBans.remove(playerId);
        player.removePotionEffect(PotionEffectType.LEVITATION);

        // Teleport slightly up
        Location loc = player.getLocation().add(0, 2, 0);
        player.teleport(loc);

        // Explosion particles
        spawnExplosion(player.getWorld(), loc);

        // Explosion sound
        try {
            player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
        } catch (Exception ignored) {}

        // Kill the player
        player.setHealth(0);

        // Execute ban command
        SchedulerManager.getAdapter().runSyncDelayed(() -> {
            executeBanCommand(banCommand);
        }, 5L);
    }

    private void spawnExplosion(org.bukkit.World world, Location center) {
        // Large explosion particles
        Particle explosion = ParticleCompat.getParticle("EXPLOSION_LARGE");
        if (explosion == null) {
            explosion = ParticleCompat.getParticle("EXPLOSION");
        }
        if (explosion != null) {
            ParticleCompat.spawnParticle(world, explosion, center, 3, 0.5, 0.5, 0.5, 0);
        }

        // Flash effect
        Particle flash = ParticleCompat.getParticle("FLASH");
        if (flash != null) {
            // Cross pattern flash
            for (int i = -2; i <= 2; i++) {
                ParticleCompat.spawnParticle(world, flash, center.clone().add(i, 0, 0), 3, 0, 0, 0, 0);
                ParticleCompat.spawnParticle(world, flash, center.clone().add(0, 0, i), 3, 0, 0, 0, 0);
            }
        }

        // Smoke/cloud
        Particle smoke = ParticleCompat.getParticle("SMOKE_LARGE");
        if (smoke == null) {
            smoke = ParticleCompat.getParticle("SMOKE");
        }
        if (smoke != null) {
            ParticleCompat.spawnParticle(world, smoke, center, 30, 1, 1, 1, 0.1);
        }
    }

    private void cleanup(Player player, String banCommand) {
        UUID playerId = player.getUniqueId();
        animatingPlayers.remove(playerId);
        pendingBans.remove(playerId);
        
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.LEVITATION);
            player.setHealth(0);
        }
        
        executeBanCommand(banCommand);
    }

    private void executeBanCommand(String command) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            SchedulerManager.getAdapter().runSync(() -> 
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }

    public boolean isAnimating(UUID playerId) {
        return animatingPlayers.contains(playerId);
    }

    public boolean isAnimating(Player player) {
        return player != null && animatingPlayers.contains(player.getUniqueId());
    }

    // ==================== Event Handlers ====================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (animatingPlayers.contains(playerId)) {
            String banCommand = pendingBans.get(playerId);
            animatingPlayers.remove(playerId);
            pendingBans.remove(playerId);
            
            // Player quit during animation - still ban them
            if (banCommand != null) {
                SchedulerManager.getAdapter().runSyncDelayed(() -> {
                    executeBanCommand(banCommand);
                }, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isAnimating(event.getPlayer())) return;
        if (event.getTo() == null) return;
        // Lock X/Z movement
        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
            Location to = event.getTo().clone();
            to.setX(event.getFrom().getX());
            to.setZ(event.getFrom().getZ());
            event.setTo(to);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (isAnimating(event.getPlayer()) && event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isAnimating(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isAnimating(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isAnimating(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && isAnimating((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player && isAnimating((Player) event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isAnimating(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isAnimating(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && isAnimating((Player) event.getDamager())) {
            event.setCancelled(true);
        }
        // Don't cancel damage TO player - we want them to die
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && isAnimating((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        animatingPlayers.clear();
        pendingBans.clear();
    }
}
