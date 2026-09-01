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

package space.max.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import space.max.Main;
import space.max.Permissions;
import space.max.alert.AlertManager;
import space.max.checks.AICheck;
import space.max.checks.CheckHistoryManager;
import space.max.config.Config;
import space.max.config.Label;
import space.max.data.AIPlayerData;
import space.max.data.DataSession;
import space.max.scheduler.ScheduledTask;
import space.max.scheduler.SchedulerManager;
import space.max.session.ISessionManager;
import space.max.util.ColorUtil;
import space.max.violation.ViolationManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final ISessionManager sessionManager;
    private final AlertManager alertManager;
    private final AICheck aiCheck;
    private final Main plugin;
    private final CheckHistoryManager checkHistoryManager;
    private final Map<UUID, UUID> probTracking = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> probTasks = new ConcurrentHashMap<>();
    private final Map<UUID, SavedPlayerState> spectatorStates = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> guiUpdateTasks = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> spectatorTargets = new ConcurrentHashMap<>();

    private static final String SPECTATORS_FILE = "spectators.yml";

    private static class SavedPlayerState {
        final GameMode previousGameMode;
        final Location previousLocation;
        SavedPlayerState(GameMode mode, Location loc) {
            this.previousGameMode = mode;
            this.previousLocation = loc;
        }
    }

    public CommandHandler(ISessionManager sessionManager, AlertManager alertManager,
                          AICheck aiCheck, Main plugin, CheckHistoryManager checkHistoryManager) {
        this.sessionManager = sessionManager;
        this.alertManager = alertManager;
        this.aiCheck = aiCheck;
        this.plugin = plugin;
        this.checkHistoryManager = checkHistoryManager;
    }

    private Config getConfig() {
        return plugin.getPluginConfig();
    }

    private String getPrefix() {
        return ColorUtil.colorize(getConfig().getPrefix());
    }

    private String msg(String key) {
        return ColorUtil.colorize(getConfig().getMessage(key));
    }

    private String msg(String key, String... replacements) {
        return ColorUtil.colorize(getConfig().getMessage(key, replacements));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "alerts":
                return handleAlerts(sender);
            case "prob":
                return handleProb(sender, args);
            case "reload":
                return handleReload(sender);
            case "datastatus":
                return handleDataStatus(sender);
            case "kicklist":
                return handleKickList(sender);
            case "list":
                return handleList(sender);
            case "leave":
                return handleLeave(sender);
            default:
                sender.sendMessage(getPrefix() + msg("unknown-command", "{ARGS}", args[0]));
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ALERTS)) {
            player.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        alertManager.toggleAlerts(player);
        return true;
    }

    private boolean handleProb(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player admin = (Player) sender;
        if (!admin.hasPermission(Permissions.PROB)) {
            admin.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        if (probTracking.containsKey(admin.getUniqueId())) {
            stopTracking(admin.getUniqueId());
            admin.sendMessage(getPrefix() + msg("tracking-stopped"));
            return true;
        }
        if (args.length < 2) {
            admin.sendMessage(getPrefix() + msg("prob-usage"));
            return true;
        }
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            admin.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
            return true;
        }
        startTracking(admin, target);
        admin.sendMessage(getPrefix() + msg("tracking-started", "{PLAYER}", target.getName()));
        return true;
    }

    private void startTracking(Player admin, Player target) {
        UUID adminId = admin.getUniqueId();
        UUID targetId = target.getUniqueId();
        stopTracking(adminId);

        probTracking.put(adminId, targetId);

        ScheduledTask task = SchedulerManager.getAdapter().runSyncRepeating(() -> {
            Player adminPlayer = Bukkit.getPlayer(adminId);
            Player targetPlayer = Bukkit.getPlayer(targetId);
            if (adminPlayer == null || !adminPlayer.isOnline() || targetPlayer == null || !targetPlayer.isOnline()) {
                stopTracking(adminId);
                return;
            }

            AIPlayerData data = aiCheck.getPlayerData(targetId);
            String message;
            if (data == null) {
                message = msg("prob-no-data", "{PLAYER}", targetPlayer.getName());
            } else {
                double prob = data.getLastProbability();
                double buffer = data.getBuffer();
                int vl = plugin.getViolationManager().getViolationLevel(targetId);
                message = getConfig().getMessage("actionbar-format",
                        targetPlayer.getName(), prob, buffer, vl);
                message = ColorUtil.colorize(message);
            }
            sendActionBar(adminPlayer, message);
        }, 0L, 10L);

        probTasks.put(adminId, task);
    }

    private void stopTracking(UUID adminId) {
        UUID targetId = probTracking.remove(adminId);
        ScheduledTask task = probTasks.remove(adminId);
        if (task != null) task.cancel();
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message));
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(Permissions.RELOAD)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage(getPrefix() + msg("config-reloaded"));
        return true;
    }

    private boolean handleKickList(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        List<ViolationManager.KickRecord> kicks = plugin.getViolationManager().getKickHistory();
        if (kicks.isEmpty()) {
            sender.sendMessage(getPrefix() + msg("kicklist-empty"));
            return true;
        }
        sender.sendMessage(getPrefix() + msg("kicklist-header"));
        sender.sendMessage(msg("kicklist-separator"));
        int index = 1;
        for (ViolationManager.KickRecord kick : kicks) {
            String formatted = msg("kicklist-format",
                    "{INDEX}", String.valueOf(index++),
                    "{PLAYER}", kick.getPlayerName(),
                    "{TIME}", kick.getFormattedTime(),
                    "{PROBABILITY}", String.format("%.2f", kick.getProbability()),
                    "{BUFFER}", String.format("%.1f", kick.getBuffer()),
                    "{VL}", String.valueOf(kick.getVl()));
            sender.sendMessage(formatted);
        }
        sender.sendMessage(msg("kicklist-separator"));
        return true;
    }

    private boolean handleDataStatus(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        int activeSessions = sessionManager.getActiveSessionCount();
        sender.sendMessage(getPrefix() + msg("data-status-header"));
        sender.sendMessage(msg("active-sessions", "{COUNT}", String.valueOf(activeSessions)));
        if (activeSessions > 0) {
            sender.sendMessage(msg("datastatus-players"));
            for (DataSession session : sessionManager.getActiveSessions()) {
                Player player = Bukkit.getPlayer(session.getUuid());
                String playerName = player != null ? player.getName() : session.getPlayerName();
                String sessionLabel = session.getLabel().name();
                String comment = session.getComment();
                boolean inCombat = session.isInCombat();
                int tickCount = session.getTickCount();
                sender.sendMessage(msg("datastatus-player-format",
                        "{PLAYER}", playerName,
                        "{LABEL}", sessionLabel,
                        "{COMMENT}", comment));
                sender.sendMessage(msg("datastatus-ticks-format",
                        "{TICKS}", String.valueOf(tickCount),
                        "{IN_COMBAT}", inCombat ? "Да" : "Нет"));
            }
        } else {
            sender.sendMessage(msg("no-active-sessions"));
            sender.sendMessage(msg("start-hint"));
        }
        return true;
    }

    // ================== ОБРАБОТКА КОМАНДЫ /mac list ==================
    private boolean handleList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player admin = (Player) sender;
        if (!admin.hasPermission(Permissions.LIST)) {
            admin.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        openListGUI(admin);
        return true;
    }

    private void openListGUI(Player viewer) {
        cancelGuiUpdate(viewer);
        Inventory inv = Bukkit.createInventory(null, 54, msg("list-title"));
        updateListInventory(inv);
        viewer.openInventory(inv);

        // Обновление раз в секунду (20 тиков)
        ScheduledTask updateTask = SchedulerManager.getAdapter().runSyncRepeating(() -> {
            if (!viewer.isOnline()) {
                cancelGuiUpdate(viewer);
                return;
            }
            Inventory openInv = viewer.getOpenInventory().getTopInventory();
            if (openInv == null || !openInv.equals(inv)) {
                cancelGuiUpdate(viewer);
                return;
            }
            updateListInventory(openInv);
            viewer.updateInventory();
        }, 20L, 20L);
        guiUpdateTasks.put(viewer.getUniqueId(), updateTask);
    }

    private void updateListInventory(Inventory inv) {
        inv.clear();

        // Оранжевая рамка
        int[] frameSlots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int slot : frameSlots) {
            inv.setItem(slot, createColoredPane("#FF7200", ColorUtil.colorize("&0")));
        }

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort((p1, p2) -> Double.compare(
                checkHistoryManager.getOverallAverage(p2.getUniqueId()),
                checkHistoryManager.getOverallAverage(p1.getUniqueId())
        ));

        int[] headSlots = {10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43};

        for (int i = 0; i < headSlots.length && i < players.size(); i++) {
            Player p = players.get(i);
            UUID uuid = p.getUniqueId();
            double overallAvg = checkHistoryManager.getOverallAverage(uuid);
            List<Double> lastChecks = checkHistoryManager.getLastChecks(uuid, 15);
            long lastCheckTime = checkHistoryManager.getLastCheckTime(uuid);

            long playTimeTicks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
            String playTime = formatPlayTime(playTimeTicks);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(p);

                String color = getColorForProbability(overallAvg, getConfig());
                int filled = (int) Math.round(overallAvg * 10);
                StringBuilder bars = new StringBuilder();
                for (int b = 0; b < 10; b++) {
                    bars.append(b < filled ? "■" : " ");
                }
                String name = ColorUtil.colorize("&#FF8C00&l► &f" + p.getName() +
                        " &f[" + "&" + color + bars + "&f] " + "&" + color + Math.round(overallAvg * 100) + "%");
                meta.setDisplayName(name);

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ColorUtil.colorize("&#FFC400Последние проверки:"));

                for (int row = 0; row < 3; row++) {
                    StringBuilder line = new StringBuilder();
                    for (int col = 0; col < 5; col++) {
                        int index = row * 5 + col;
                        if (index < lastChecks.size()) {
                            double value = lastChecks.get(index);
                            String valColor = getColorForProbability(value, getConfig());
                            line.append("&").append(valColor).append(String.format("%.4f", value));
                        } else {
                            line.append("&f     ");
                        }
                        if (col < 4) line.append(" ");
                    }
                    lore.add(ColorUtil.colorize(line.toString()));
                }

                lore.add("");
                lore.add(ColorUtil.colorize("&#FFC400Средний риск:"));
                lore.add(ColorUtil.colorize("&#FF8C00AVG: " + "&" + color + String.format("%.4f", overallAvg)));
                lore.add("");
                lore.add(ColorUtil.colorize("&#FFC400Последняя проверка:"));

                if (lastCheckTime > 0) {
                    long secondsAgo = (System.currentTimeMillis() - lastCheckTime) / 1000;
                    lore.add(ColorUtil.colorize("&f" + formatTimeAgo(secondsAgo)));
                } else {
                    lore.add(ColorUtil.colorize("&f-"));
                }

                lore.add("");
                lore.add(ColorUtil.colorize("&#FFC400Времени на сервере:"));
                lore.add(ColorUtil.colorize("&f" + playTime));
                lore.add("");
                lore.add(ColorUtil.colorize("&#FF8C00&l► &fНажмите чтобы следить"));

                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(headSlots[i], head);
        }
    }

    private String formatTimeAgo(long seconds) {
        if (seconds < 60) {
            return seconds + " " + plural(seconds, "секунда", "секунды", "секунд") + " назад";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long sec = seconds % 60;
            return minutes + " " + plural(minutes, "минута", "минуты", "минут") +
                    (sec > 0 ? " " + sec + " " + plural(sec, "секунда", "секунды", "секунд") : "") + " назад";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long sec = seconds % 60;
            return hours + " " + plural(hours, "час", "часа", "часов") +
                    (minutes > 0 ? " " + minutes + " " + plural(minutes, "минута", "минуты", "минут") : "") +
                    (sec > 0 ? " " + sec + " " + plural(sec, "секунда", "секунды", "секунд") : "") + " назад";
        }
    }

    private String formatPlayTime(long ticks) {
        long totalSeconds = ticks / 20;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(" ").append(plural(hours, "час", "часа", "часов"));
            if (minutes > 0) sb.append(" ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(" ").append(plural(minutes, "минута", "минуты", "минут"));
            if (seconds > 0) sb.append(" ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append(" ").append(plural(seconds, "секунда", "секунды", "секунд"));
        }
        return sb.toString();
    }

    private String plural(long n, String one, String few, String many) {
        long mod10 = n % 10;
        long mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) {
            return one;
        } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
            return few;
        } else {
            return many;
        }
    }

    // ================== СПЕКТАТОР ==================
    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.LEAVE)) {
            player.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        SavedPlayerState state = spectatorStates.remove(player.getUniqueId());
        if (state == null) {
            player.sendMessage(getPrefix() + msg("spectator-not-spectator"));
            return true;
        }
        player.setGameMode(state.previousGameMode);
        player.teleport(state.previousLocation);
        spectatorTargets.remove(player.getUniqueId());
        player.sendMessage(getPrefix() + msg("spectator-leave"));
        saveSpectatorStates();
        return true;
    }

    public void enterSpectator(Player viewer, Player target) {
        if (!viewer.hasPermission(Permissions.SPECTATOR)) {
            viewer.sendMessage(getPrefix() + msg("spectator-no-permission"));
            return;
        }
        if (isSpectator(viewer)) {
            viewer.sendMessage(getPrefix() + msg("spectator-already"));
            return;
        }
        if (viewer.equals(target)) {
            viewer.sendMessage(getPrefix() + msg("spectator-self"));
            return;
        }
        spectatorStates.put(viewer.getUniqueId(),
                new SavedPlayerState(viewer.getGameMode(), viewer.getLocation()));
        spectatorTargets.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.teleport(target);
        viewer.sendMessage(getPrefix() + msg("spectator-teleport"));
        saveSpectatorStates();
    }

    public boolean isSpectator(Player player) {
        return spectatorStates.containsKey(player.getUniqueId()) || spectatorTargets.containsKey(player.getUniqueId());
    }

    public UUID getSpectatorTarget(UUID spectatorId) {
        return spectatorTargets.get(spectatorId);
    }

    public void handlePlayerJoin(Player player) {
        SavedPlayerState state = spectatorStates.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        SchedulerManager.getAdapter().runSyncDelayed(() -> {
            if (player.isOnline()) {
                player.setGameMode(state.previousGameMode);
                player.teleport(state.previousLocation);
                spectatorTargets.remove(player.getUniqueId());
                player.sendMessage(getPrefix() + msg("spectator-leave"));
                saveSpectatorStates();
            }
        }, 1L);
    }

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();

        // Останавливаем трекинг, если игрок был целью или наблюдателем
        Iterator<Map.Entry<UUID, UUID>> iterator = probTracking.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            UUID adminId = entry.getKey();
            UUID targetId = entry.getValue();
            if (adminId.equals(playerId) || targetId.equals(playerId)) {
                stopTracking(adminId);
            }
        }

        // Сохраняем состояние наблюдателя, если выходит именно наблюдатель
        if (spectatorStates.containsKey(playerId)) {
            saveSpectatorStates();
        }
    }

    public void cancelGuiUpdate(Player player) {
        ScheduledTask task = guiUpdateTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    // ================== СОХРАНЕНИЕ И ЗАГРУЗКА СПЕКТАТОРОВ ==================
    public void loadSpectatorStates() {
        File file = new File(plugin.getDataFolder(), SPECTATORS_FILE);
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("spectators");
        if (section == null) return;

        spectatorStates.clear();
        spectatorTargets.clear();

        for (String uuidStr : section.getKeys(false)) {
            UUID playerUuid = UUID.fromString(uuidStr);
            ConfigurationSection stateSec = section.getConfigurationSection(uuidStr);
            if (stateSec == null) continue;

            String gameModeStr = stateSec.getString("gameMode", "SURVIVAL");
            GameMode gameMode;
            try {
                gameMode = GameMode.valueOf(gameModeStr);
            } catch (IllegalArgumentException e) {
                gameMode = GameMode.SURVIVAL;
            }

            String worldName = stateSec.getString("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double x = stateSec.getDouble("x");
            double y = stateSec.getDouble("y");
            double z = stateSec.getDouble("z");
            float yaw = (float) stateSec.getDouble("yaw");
            float pitch = (float) stateSec.getDouble("pitch");
            Location location = new Location(world, x, y, z, yaw, pitch);

            SavedPlayerState saved = new SavedPlayerState(gameMode, location);
            spectatorStates.put(playerUuid, saved);

            String targetUuidStr = stateSec.getString("targetUuid");
            if (targetUuidStr != null && !targetUuidStr.isEmpty()) {
                spectatorTargets.put(playerUuid, UUID.fromString(targetUuidStr));
            }
        }
    }

    public void saveSpectatorStates() {
        File file = new File(plugin.getDataFolder(), SPECTATORS_FILE);
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("spectators");

        for (Map.Entry<UUID, SavedPlayerState> entry : spectatorStates.entrySet()) {
            UUID playerUuid = entry.getKey();
            SavedPlayerState state = entry.getValue();
            Location loc = state.previousLocation;
            if (loc.getWorld() == null) continue;

            String path = playerUuid.toString();
            ConfigurationSection stateSec = section.createSection(path);
            stateSec.set("gameMode", state.previousGameMode.name());
            stateSec.set("world", loc.getWorld().getName());
            stateSec.set("x", loc.getX());
            stateSec.set("y", loc.getY());
            stateSec.set("z", loc.getZ());
            stateSec.set("yaw", loc.getYaw());
            stateSec.set("pitch", loc.getPitch());
            UUID targetUuid = spectatorTargets.get(playerUuid);
            if (targetUuid != null) {
                stateSec.set("targetUuid", targetUuid.toString());
            }
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить spectators.yml: " + e.getMessage());
        }
    }

    // ================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==================
    private String getColorForProbability(double prob, Config config) {
        if (prob < config.getCheckGreenThreshold()) return config.getCheckGreenColor();
        else if (prob < config.getCheckYellowThreshold()) return config.getCheckYellowColor();
        else if (prob < config.getCheckOrangeThreshold()) return config.getCheckOrangeColor();
        else return config.getCheckRedColor();
    }

    private ItemStack createColoredPane(String hexColor, String displayName) {
        String paneName = getPaneMaterialName(hexColor);
        Material material = Material.matchMaterial(paneName);
        if (material == null) material = Material.GLASS_PANE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getPaneMaterialName(String hexColor) {
        switch (hexColor.toLowerCase()) {
            case "#ff0000": return "RED_STAINED_GLASS_PANE";
            case "#ff7200": return "ORANGE_STAINED_GLASS_PANE";
            case "#fadf00": return "YELLOW_STAINED_GLASS_PANE";
            case "#73fa00": return "LIME_STAINED_GLASS_PANE";
            case "#a0a0a0": return "GRAY_STAINED_GLASS_PANE";
            case "#ffffff": return "WHITE_STAINED_GLASS_PANE";
            default: return "GRAY_STAINED_GLASS_PANE";
        }
    }

    // ================== ОСТАЛЬНЫЕ КОМАНДЫ ==================
    private boolean handleStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(getPrefix() + msg("usage-start"));
            return true;
        }
        String target = args[1];
        String labelStr = args[2];
        Label sessionLabel = Label.fromString(labelStr);
        if (sessionLabel == null) {
            sender.sendMessage(getPrefix() + msg("invalid-label", "{LABEL}", labelStr));
            sender.sendMessage(getPrefix() + msg("valid-labels"));
            return true;
        }
        String comment = parseComment(args, 3);
        return handleStartPlayer(sender, target, sessionLabel, comment);
    }

    private boolean handleStartPlayer(CommandSender sender, String playerName, Label label, String comment) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
            return true;
        }
        sessionManager.startSession(player, label, comment);
        sender.sendMessage(getPrefix() + msg("session-started", "{LABEL}", label.name(), "{COUNT}", "1", "{PLAYER}", player.getName()));
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + msg("usage-stop"));
            return true;
        }
        String target = args[1];
        return handleStopPlayer(sender, target);
    }

    private boolean handleStopPlayer(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
            return true;
        }
        if (!sessionManager.hasActiveSession(player)) {
            sender.sendMessage(getPrefix() + msg("no-sessions-to-stop"));
            return true;
        }
        sessionManager.stopSession(player);
        sender.sendMessage(getPrefix() + msg("session-stopped", "{PLAYER}", player.getName()));
        return true;
    }

    private String parseComment(String[] args, int startIndex) {
        if (startIndex >= args.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        String comment = sb.toString();
        if (comment.startsWith("\"") && comment.endsWith("\"") && comment.length() >= 2) {
            comment = comment.substring(1, comment.length() - 1);
        } else if (comment.startsWith("\"")) {
            comment = comment.substring(1);
        }
        return comment.trim();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(getPrefix() + msg("usage-header"));
        sender.sendMessage(msg("usage-start"));
        sender.sendMessage(msg("usage-stop"));
        sender.sendMessage(msg("usage-datastatus"));
        sender.sendMessage(msg("usage-alerts"));
        sender.sendMessage(msg("usage-prob"));
        sender.sendMessage(msg("usage-list"));
        sender.sendMessage(msg("usage-leave"));
        sender.sendMessage(msg("usage-kicklist"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> commands = Arrays.asList("start", "stop", "datastatus", "alerts", "prob", "reload", "kicklist", "list", "leave");
            completions.addAll(filterStartsWith(commands, args[0]));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("start") || subCommand.equals("stop") || subCommand.equals("prob")) {
                completions.addAll(filterStartsWith(getOnlinePlayerNames(), args[1]));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("start")) {
                List<String> labels = Arrays.stream(Label.values())
                        .map(Label::name)
                        .collect(Collectors.toList());
                completions.addAll(filterStartsWith(labels, args[2]));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("start")) {
                if (args[3].isEmpty() || args[3].startsWith("\"")) {
                    completions.add("\"comment\"");
                }
            }
        }
        return completions;
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerPrefix))
                .collect(Collectors.toList());
    }

    public void cleanup() {
        // Останавливаем задачи
        for (ScheduledTask task : probTasks.values()) task.cancel();
        for (ScheduledTask task : guiUpdateTasks.values()) task.cancel();

        probTasks.clear();
        probTracking.clear();
        guiUpdateTasks.clear();
        spectatorTargets.clear();
        // spectatorStates не очищаем, чтобы сохранить при выключении
    }
}