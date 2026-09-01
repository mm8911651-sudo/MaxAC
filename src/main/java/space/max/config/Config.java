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

package space.max.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class Config {
    private final boolean debug;
    private final int preHitTicks;
    private final int postHitTicks;
    private final double hitLockThreshold;
    private final int postHitTimeoutTicks;
    private final String outputDirectory;
    private final boolean aiEnabled;
    private final String aiApiKey;
    private final double aiAlertThreshold;
    private final boolean aiConsoleAlerts;
    private final double aiBufferFlag;
    private final double aiBufferResetOnFlag;
    private final double aiBufferMultiplier;
    private final double aiBufferDecrease;
    private final int aiSequence;
    private final int aiStep;
    private final double aiPunishmentMinProbability;
    private final Map<Integer, String> punishmentCommands;
    private final String customAlertPrefix;
    private final boolean animationEnabled;
    private final String prefix;
    private final Map<String, String> messages;
    private final boolean liteBansEnabled;
    private final String liteBansDbHost;
    private final int liteBansDbPort;
    private final String liteBansDbName;
    private final String liteBansDbUsername;
    private final String liteBansDbPassword;
    private final String liteBansTablePrefix;
    private final int liteBansLookbackDays;
    private final Set<String> liteBansCheatReasons;
    private final boolean autostartEnabled;
    private final String autostartLabel;
    private final String autostartComment;
    private final ServerType serverType;
    private final String serverAddress;
    private final int reportStatsIntervalSeconds;
    private final boolean vlDecayEnabled;
    private final int vlDecayIntervalSeconds;
    private final int vlDecayAmount;
    private final boolean worldGuardEnabled;
    private final List<String> worldGuardDisabledRegions;
    private final boolean foliaEnabled;
    private final int foliaThreadPoolSize;
    private final boolean foliaEntitySchedulerEnabled;
    private final boolean foliaRegionSchedulerEnabled;

    // Новые поля для настройки отображения проверок
    private final double checkGreenThreshold;
    private final double checkYellowThreshold;
    private final double checkOrangeThreshold;
    private final String checkGreenColor;
    private final String checkYellowColor;
    private final String checkOrangeColor;
    private final String checkRedColor;
    private final String alertPrefix;

    // Константы по умолчанию (все)
    public static final boolean DEFAULT_DEBUG = false;
    public static final String DEFAULT_OUTPUT_DIRECTORY = "plugins/MaxAC/data";
    public static final int PRE_HIT_TICKS = 5;
    public static final int POST_HIT_TICKS = 3;
    public static final double HIT_LOCK_THRESHOLD = 5.0;
    public static final int POST_HIT_TIMEOUT_TICKS = 40;
    public static final boolean DEFAULT_AI_ENABLED = false;
    public static final String DEFAULT_AI_API_KEY = "";
    public static final double DEFAULT_AI_ALERT_THRESHOLD = 0.5;
    public static final boolean DEFAULT_AI_CONSOLE_ALERTS = true;
    public static final double DEFAULT_AI_BUFFER_FLAG = 50.0;
    public static final double DEFAULT_AI_BUFFER_RESET_ON_FLAG = 25.0;
    public static final double DEFAULT_AI_BUFFER_MULTIPLIER = 100.0;
    public static final double DEFAULT_AI_BUFFER_DECREASE = 0.25;
    public static final double DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY = 0.85;
    public static final String DEFAULT_CUSTOM_ALERT_PREFIX = "&6[MaxAC] &f";
    public static final boolean DEFAULT_ANIMATION_ENABLED = true;
    public static final int DEFAULT_AI_SEQUENCE = 40;
    public static final int DEFAULT_AI_STEP = 10;
    public static final String DEFAULT_PREFIX = "&#FF8C00[MaxAC] &f";
    public static final String DEFAULT_MSG_ALERTS_ENABLED = "&aAI alerts enabled";
    public static final String DEFAULT_MSG_ALERTS_DISABLED = "&eAI alerts disabled";
    public static final String DEFAULT_MSG_ALERT_FORMAT = "&#FF8C00<player> &7flagged by AI &f| Prob: &e<probability> &f| Buffer: &e<buffer>";
    public static final String DEFAULT_MSG_ALERT_FORMAT_VL = "&#FF8C00<player> &7flagged by AI &f| Prob: &e<probability> &f| Buffer: &e<buffer> &f| VL: &#FF8C00<vl>";
    public static final String DEFAULT_MSG_ACTIONBAR_FORMAT =
            "&fИгрок &#FF8C00{PLAYER} &7| &fВероятность: &#FF8C00{PROBABILITY} &7| &fБуфер: &#FF8C00{BUFFER} &7| &fVL: &#FF8C00{VL}";
    public static final String DEFAULT_MSG_USAGE_CHECKS = "&cИспользование: /mac checks <ник>";
    public static final String DEFAULT_MSG_NO_CHECKS_FOUND = "&eДля игрока &c{PLAYER} &eнет записей проверок.";
    public static final String DEFAULT_ALERT_PREFIX = "&#FF8C00[ALERT] &f";
    public static final String DEFAULT_MSG_HOLOGRAM_AVG = "&fAVG: &{COLOR}{AVG}";
    public static final String DEFAULT_MSG_HOLOGRAM_CHECK_ITEM = "&fC{INDEX}: &{COLOR}{VALUE}";
    public static final String DEFAULT_MSG_HOLOGRAM_TEXT = "&fAVG: &7...";
    public static final String DEFAULT_MSG_SPECTATOR_TELEPORT = "&fЧтобы выйти из режима наблюдателя пропишите /mac leave";
    public static final String DEFAULT_MSG_SPECTATOR_LEAVE = "&fВы вышли из режима наблюдателя";
    public static final String DEFAULT_MSG_SPECTATOR_NO_PERMISSION = "&cУ вас нет прав для этого";
    public static final String DEFAULT_MSG_SPECTATOR_NOT_SPECTATOR = "&cВы не в режиме наблюдателя";
    public static final String DEFAULT_MSG_SPECTATOR_ALREADY = "&cВы уже находитесь в режиме наблюдателя";
    public static final String DEFAULT_MSG_SPECTATOR_SELF = "&#FF8C00Вы не можете следить за самим собой";
    public static final String DEFAULT_MSG_LIST_AVG = "&fAVG: &{COLOR}{AVG}";
    public static final boolean DEFAULT_LITEBANS_ENABLED = false;
    public static final String DEFAULT_LITEBANS_DB_HOST = "localhost";
    public static final int DEFAULT_LITEBANS_DB_PORT = 3306;
    public static final String DEFAULT_LITEBANS_DB_NAME = "litebans";
    public static final String DEFAULT_LITEBANS_DB_USERNAME = "";
    public static final String DEFAULT_LITEBANS_DB_PASSWORD = "";
    public static final String DEFAULT_LITEBANS_TABLE_PREFIX = "litebans_";
    public static final int DEFAULT_LITEBANS_LOOKBACK_DAYS = 7;
    public static final boolean DEFAULT_AUTOSTART_ENABLED = false;
    public static final String DEFAULT_AUTOSTART_LABEL = "UNLABELED";
    public static final String DEFAULT_AUTOSTART_COMMENT = "";
    public static final String DEFAULT_SERVER_TYPE = "signalr";
    public static final String DEFAULT_SERVER_ADDRESS = "https://api-inference.huggingface.co/models/username/model";
    public static final int DEFAULT_REPORT_STATS_INTERVAL_SECONDS = 30;
    public static final boolean DEFAULT_VL_DECAY_ENABLED = true;
    public static final int DEFAULT_VL_DECAY_INTERVAL_SECONDS = 60;
    public static final int DEFAULT_VL_DECAY_AMOUNT = 1;
    public static final boolean DEFAULT_WORLDGUARD_ENABLED = true;
    public static final List<String> DEFAULT_WORLDGUARD_DISABLED_REGIONS = new ArrayList<>();
    public static final boolean DEFAULT_FOLIA_ENABLED = true;
    public static final int DEFAULT_FOLIA_THREAD_POOL_SIZE = 0;
    public static final boolean DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED = true;
    public static final boolean DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED = true;
    public static final double DEFAULT_CHECK_GREEN_THRESHOLD = 0.3;
    public static final double DEFAULT_CHECK_YELLOW_THRESHOLD = 0.5;
    public static final double DEFAULT_CHECK_ORANGE_THRESHOLD = 0.8;
    public static final String DEFAULT_CHECK_GREEN_COLOR = "#73FA00";
    public static final String DEFAULT_CHECK_YELLOW_COLOR = "#FADF00";
    public static final String DEFAULT_CHECK_ORANGE_COLOR = "#FF7200";
    public static final String DEFAULT_CHECK_RED_COLOR = "#FF0000";

    // Конструктор по умолчанию (для тестов)
    public Config() {
        this.debug = DEFAULT_DEBUG;
        this.preHitTicks = PRE_HIT_TICKS;
        this.postHitTicks = POST_HIT_TICKS;
        this.hitLockThreshold = HIT_LOCK_THRESHOLD;
        this.postHitTimeoutTicks = POST_HIT_TIMEOUT_TICKS;
        this.outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        this.aiEnabled = DEFAULT_AI_ENABLED;
        this.aiApiKey = DEFAULT_AI_API_KEY;
        this.aiAlertThreshold = DEFAULT_AI_ALERT_THRESHOLD;
        this.aiConsoleAlerts = DEFAULT_AI_CONSOLE_ALERTS;
        this.aiBufferFlag = DEFAULT_AI_BUFFER_FLAG;
        this.aiBufferResetOnFlag = DEFAULT_AI_BUFFER_RESET_ON_FLAG;
        this.aiBufferMultiplier = DEFAULT_AI_BUFFER_MULTIPLIER;
        this.aiBufferDecrease = DEFAULT_AI_BUFFER_DECREASE;
        this.aiSequence = DEFAULT_AI_SEQUENCE;
        this.aiStep = DEFAULT_AI_STEP;
        this.aiPunishmentMinProbability = DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY;
        this.punishmentCommands = new HashMap<>();
        this.customAlertPrefix = DEFAULT_CUSTOM_ALERT_PREFIX;
        this.animationEnabled = DEFAULT_ANIMATION_ENABLED;
        this.prefix = DEFAULT_PREFIX;
        this.messages = createDefaultMessages();
        this.liteBansEnabled = DEFAULT_LITEBANS_ENABLED;
        this.liteBansDbHost = DEFAULT_LITEBANS_DB_HOST;
        this.liteBansDbPort = DEFAULT_LITEBANS_DB_PORT;
        this.liteBansDbName = DEFAULT_LITEBANS_DB_NAME;
        this.liteBansDbUsername = DEFAULT_LITEBANS_DB_USERNAME;
        this.liteBansDbPassword = DEFAULT_LITEBANS_DB_PASSWORD;
        this.liteBansTablePrefix = DEFAULT_LITEBANS_TABLE_PREFIX;
        this.liteBansLookbackDays = DEFAULT_LITEBANS_LOOKBACK_DAYS;
        this.liteBansCheatReasons = createDefaultCheatReasons();
        this.autostartEnabled = DEFAULT_AUTOSTART_ENABLED;
        this.autostartLabel = DEFAULT_AUTOSTART_LABEL;
        this.autostartComment = DEFAULT_AUTOSTART_COMMENT;
        this.serverType = ServerType.fromString(DEFAULT_SERVER_TYPE);
        this.serverAddress = DEFAULT_SERVER_ADDRESS;
        this.reportStatsIntervalSeconds = DEFAULT_REPORT_STATS_INTERVAL_SECONDS;
        this.vlDecayEnabled = DEFAULT_VL_DECAY_ENABLED;
        this.vlDecayIntervalSeconds = DEFAULT_VL_DECAY_INTERVAL_SECONDS;
        this.vlDecayAmount = DEFAULT_VL_DECAY_AMOUNT;
        this.worldGuardEnabled = DEFAULT_WORLDGUARD_ENABLED;
        this.worldGuardDisabledRegions = new ArrayList<>(DEFAULT_WORLDGUARD_DISABLED_REGIONS);
        this.foliaEnabled = DEFAULT_FOLIA_ENABLED;
        this.foliaThreadPoolSize = DEFAULT_FOLIA_THREAD_POOL_SIZE;
        this.foliaEntitySchedulerEnabled = DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED;
        this.foliaRegionSchedulerEnabled = DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED;
        this.checkGreenThreshold = DEFAULT_CHECK_GREEN_THRESHOLD;
        this.checkYellowThreshold = DEFAULT_CHECK_YELLOW_THRESHOLD;
        this.checkOrangeThreshold = DEFAULT_CHECK_ORANGE_THRESHOLD;
        this.checkGreenColor = DEFAULT_CHECK_GREEN_COLOR;
        this.checkYellowColor = DEFAULT_CHECK_YELLOW_COLOR;
        this.checkOrangeColor = DEFAULT_CHECK_ORANGE_COLOR;
        this.checkRedColor = DEFAULT_CHECK_RED_COLOR;
        this.alertPrefix = DEFAULT_ALERT_PREFIX;
    }

    private static Set<String> createDefaultCheatReasons() {
        Set<String> reasons = new HashSet<>();
        reasons.add("killaura");
        reasons.add("cheat");
        reasons.add("hack");
        return reasons;
    }

    private static Map<String, String> createDefaultMessages() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("alerts-enabled", DEFAULT_MSG_ALERTS_ENABLED);
        defaults.put("alerts-disabled", DEFAULT_MSG_ALERTS_DISABLED);
        defaults.put("alert-format", DEFAULT_MSG_ALERT_FORMAT);
        defaults.put("alert-format-vl", DEFAULT_MSG_ALERT_FORMAT_VL);
        defaults.put("actionbar-format", DEFAULT_MSG_ACTIONBAR_FORMAT);
        defaults.put("usage-checks", DEFAULT_MSG_USAGE_CHECKS);
        defaults.put("no-checks-found", DEFAULT_MSG_NO_CHECKS_FOUND);
        defaults.put("hologram-avg", DEFAULT_MSG_HOLOGRAM_AVG);
        defaults.put("hologram-check-item", DEFAULT_MSG_HOLOGRAM_CHECK_ITEM);
        defaults.put("hologram-text", DEFAULT_MSG_HOLOGRAM_TEXT);
        defaults.put("spectator-teleport", DEFAULT_MSG_SPECTATOR_TELEPORT);
        defaults.put("spectator-leave", DEFAULT_MSG_SPECTATOR_LEAVE);
        defaults.put("spectator-no-permission", DEFAULT_MSG_SPECTATOR_NO_PERMISSION);
        defaults.put("spectator-not-spectator", DEFAULT_MSG_SPECTATOR_NOT_SPECTATOR);
        defaults.put("spectator-already", DEFAULT_MSG_SPECTATOR_ALREADY);
        defaults.put("spectator-self", DEFAULT_MSG_SPECTATOR_SELF);
        defaults.put("list-avg", DEFAULT_MSG_LIST_AVG);
        defaults.put("prob-no-data", "&7{PLAYER}: &eНет данных");
        defaults.put("kicklist-empty", "&7Нет киков от AI античита");
        defaults.put("kicklist-header", "&#FF8C00Последние кики от AI античита:");
        defaults.put("kicklist-separator", "&7─────────────────────────────────");
        defaults.put("kicklist-format", "&e{INDEX}. &f{PLAYER} &7[&c{TIME}&7] &8- &bProb: &f{PROBABILITY} &8| &bBuf: &f{BUFFER} &8| &bVL: &#FF8C00{VL}");
        defaults.put("datastatus-players", "&7Игроки собирающие данные:");
        defaults.put("datastatus-player-format", "&b  {PLAYER}&7 [&e{LABEL}&7]{COMMENT}");
        defaults.put("datastatus-ticks-format", "&7    Тики: &a{TICKS}&7 | В бою: &a{IN_COMBAT}");
        defaults.put("checks-title", "&7Проверки: &#FF8C00{PLAYER}");
        defaults.put("checks-group-avg", "&fAVG: &{COLOR}{AVG}");
        defaults.put("checks-chance", "&fШанс: &{COLOR}{CHANCE}");
        defaults.put("list-title", "&8Список игроков");
        defaults.put("list-group", "&fГруппа {GROUP}: &{COLOR}{AVG}");
        defaults.put("usage-list", "&7  /mac list - Список игроков по вероятности");
        defaults.put("usage-kicklist", "&7  /mac kicklist - Последние 10 киков от AI античита");
        defaults.put("usage-leave", "&7  /mac leave - Выйти из режима наблюдателя");

        // Сообщения модерации
        defaults.put("moderation.no-permission", "&cУ вас нет прав!");
        defaults.put("moderation.players-only", "&cТолько для игроков!");
        defaults.put("moderation.ban-usage", "&cИспользование: /ban <ник> <причина> <время>");
        defaults.put("moderation.ban-kick", "&#FF8C00Вы забанены!\n&7Причина: &f{REASON}\n&7Осталось: &f{TIME}");
        defaults.put("moderation.ban-broadcast", "&#FF8C00[Mod] &fИгрок &c{PLAYER} &fзабанен по причине &c{REASON} &fна &c{TIME}");
        defaults.put("moderation.cannot-punish", "&cНельзя наказать этого игрока!");
        defaults.put("moderation.mute-usage", "&cИспользование: /mute <ник> <причина> <время>");
        defaults.put("moderation.mute-target", "&#FF8C00Вы замучены!\n&7Причина: &f{REASON}\n&7Осталось: &f{TIME}");
        defaults.put("moderation.mute-broadcast", "&#FF8C00[Mod] &fИгрок &c{PLAYER} &fзамучен по причине &c{REASON} &fна &c{TIME}");
        defaults.put("moderation.unban-usage", "&cИспользование: /unban <ник>");
        defaults.put("moderation.unban-broadcast", "&#FF8C00[Mod] &fИгрок &c{PLAYER} &fразбанен!");
        defaults.put("moderation.unmute-usage", "&cИспользование: /unmute <ник>");
        defaults.put("moderation.unmute-target", "&#FF8C00Вы снова можете писать в чат!");
        defaults.put("moderation.unmute-sender", "&#FF8C00[Mod] &fИгрок &c{PLAYER} &fразмучен!");
        defaults.put("moderation.banip-usage", "&cИспользование: /banip <ник> <причина> <время>");
        defaults.put("moderation.ipban-kick", "&#FF8C00Вы забанены по IP!\n&7Причина: &f{REASON}\n&7Осталось: &f{TIME}");
        defaults.put("moderation.ipban-broadcast", "&#FF8C00[Mod] &fИгрок &c{PLAYER} &fзабанен по IP по причине &c{REASON} &fна &c{TIME}");
        defaults.put("moderation.check-usage", "&cИспользование: /check <ник>");
        defaults.put("moderation.report-usage", "&cИспользование: /report <ник>");
        defaults.put("moderation.report-sent", "&#FF8C00Жалоба отправлена!");
        defaults.put("moderation.report-cooldown", "&cВы уже жаловались на &f{PLAYER}&c! Осталось: &f{TIME}");
        defaults.put("moderation.clear-suspicious", "&#FF8C00Подозрения игрока &f{PLAYER} &aочищены!");
        defaults.put("moderation.salary-claimed", "&#FF8C00Вы получили зарплату: &f{AMOUNT} шардов");
        defaults.put("moderation.salary-cooldown", "&cЗарплату можно получать раз в неделю!");
        defaults.put("moderation.unknown-command", "&cНеизвестная команда!");
        defaults.put("moderation.invalid-time", "&cНеверный формат времени! Пример: 30m, 20h, 15d, 1w");
        defaults.put("moderation.player-not-found", "&cИгрок &f{PLAYER} &cне найден!");
        return defaults;
    }

    public Config(JavaPlugin plugin) {
        this(plugin, null);
    }

    public Config(JavaPlugin plugin, Logger logger) {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        this.debug = config.getBoolean("debug", DEFAULT_DEBUG);
        this.preHitTicks = PRE_HIT_TICKS;
        this.postHitTicks = POST_HIT_TICKS;
        this.hitLockThreshold = HIT_LOCK_THRESHOLD;
        this.postHitTimeoutTicks = POST_HIT_TIMEOUT_TICKS;
        this.outputDirectory = config.getString("outputDirectory", DEFAULT_OUTPUT_DIRECTORY);
        this.aiEnabled = config.getBoolean("detection.enabled", config.getBoolean("ai.enabled", DEFAULT_AI_ENABLED));
        this.aiApiKey = config.getString("detection.api-key", config.getString("ai.api-key", DEFAULT_AI_API_KEY));
        double alertThreshold = config.getDouble("alerts.threshold", config.getDouble("ai.alert.threshold", DEFAULT_AI_ALERT_THRESHOLD));
        this.aiAlertThreshold = clampThreshold(alertThreshold, "alerts.threshold", logger);
        this.aiConsoleAlerts = config.getBoolean("alerts.console", config.getBoolean("ai.alert.console", DEFAULT_AI_CONSOLE_ALERTS));
        this.aiBufferFlag = config.getDouble("violation.threshold", config.getDouble("ai.buffer.flag", DEFAULT_AI_BUFFER_FLAG));
        this.aiBufferResetOnFlag = config.getDouble("violation.reset-value", config.getDouble("ai.buffer.reset-on-flag", DEFAULT_AI_BUFFER_RESET_ON_FLAG));
        this.aiBufferMultiplier = config.getDouble("violation.multiplier", config.getDouble("ai.buffer.multiplier", DEFAULT_AI_BUFFER_MULTIPLIER));
        this.aiBufferDecrease = config.getDouble("violation.decay", config.getDouble("ai.buffer.decrease", DEFAULT_AI_BUFFER_DECREASE));
        this.aiSequence = config.getInt("detection.sample-size", config.getInt("ai.sequence", DEFAULT_AI_SEQUENCE));
        this.aiStep = config.getInt("detection.sample-interval", config.getInt("ai.step", DEFAULT_AI_STEP));
        double punishmentMinProb = config.getDouble("penalties.min-probability", config.getDouble("ai.punishment.min-probability", DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY));
        this.aiPunishmentMinProbability = clampThreshold(punishmentMinProb, "penalties.min-probability", logger);
        this.customAlertPrefix = config.getString("penalties.custom-alert-prefix", DEFAULT_CUSTOM_ALERT_PREFIX);
        this.animationEnabled = config.getBoolean("penalties.animation.enabled", DEFAULT_ANIMATION_ENABLED);
        this.punishmentCommands = new HashMap<>();
        ConfigurationSection cmdSection = config.getConfigurationSection("penalties.actions");
        if (cmdSection == null) {
            cmdSection = config.getConfigurationSection("ai.punishment.commands");
        }
        if (cmdSection != null) {
            for (String key : cmdSection.getKeys(false)) {
                try {
                    int vl = Integer.parseInt(key);
                    String cmd = cmdSection.getString(key);
                    if (cmd != null && !cmd.isEmpty()) {
                        punishmentCommands.put(vl, cmd);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        this.prefix = config.getString("messages.prefix", DEFAULT_PREFIX);
        this.messages = createDefaultMessages();
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                if (!key.equals("prefix")) {
                    String msg = msgSection.getString(key);
                    if (msg != null) {
                        messages.put(key, msg);
                    }
                }
            }
        }
        // Чтение модерационных сообщений из секции "moderation"
        ConfigurationSection moderationSection = config.getConfigurationSection("moderation");
        if (moderationSection != null) {
            for (String key : moderationSection.getKeys(false)) {
                String value = moderationSection.getString(key);
                if (value != null) {
                    messages.put("moderation." + key, value);
                }
            }
        }
        this.liteBansEnabled = config.getBoolean("litebans.enabled", DEFAULT_LITEBANS_ENABLED);
        this.liteBansDbHost = config.getString("litebans.database.host", DEFAULT_LITEBANS_DB_HOST);
        this.liteBansDbPort = config.getInt("litebans.database.port", DEFAULT_LITEBANS_DB_PORT);
        this.liteBansDbName = config.getString("litebans.database.name", DEFAULT_LITEBANS_DB_NAME);
        this.liteBansDbUsername = config.getString("litebans.database.username", DEFAULT_LITEBANS_DB_USERNAME);
        this.liteBansDbPassword = config.getString("litebans.database.password", DEFAULT_LITEBANS_DB_PASSWORD);
        this.liteBansTablePrefix = config.getString("litebans.table-prefix", DEFAULT_LITEBANS_TABLE_PREFIX);
        this.liteBansLookbackDays = config.getInt("litebans.lookback-days", DEFAULT_LITEBANS_LOOKBACK_DAYS);
        this.liteBansCheatReasons = new HashSet<>();
        List<String> reasonsList = config.getStringList("litebans.cheat-reasons");
        if (reasonsList.isEmpty()) {
            this.liteBansCheatReasons.addAll(createDefaultCheatReasons());
        } else {
            this.liteBansCheatReasons.addAll(reasonsList);
        }
        this.autostartEnabled = config.getBoolean("autostart.enabled", DEFAULT_AUTOSTART_ENABLED);
        this.autostartLabel = config.getString("autostart.label", DEFAULT_AUTOSTART_LABEL);
        this.autostartComment = config.getString("autostart.comment", DEFAULT_AUTOSTART_COMMENT);
        this.serverType = ServerType.fromString(config.getString("detection.server-type", config.getString("ai.server-type", DEFAULT_SERVER_TYPE)));
        this.serverAddress = config.getString("detection.endpoint", config.getString("ai.server", DEFAULT_SERVER_ADDRESS));
        this.reportStatsIntervalSeconds = DEFAULT_REPORT_STATS_INTERVAL_SECONDS;
        this.vlDecayEnabled = config.getBoolean("violation.vl-decay.enabled", DEFAULT_VL_DECAY_ENABLED);
        this.vlDecayIntervalSeconds = config.getInt("violation.vl-decay.interval", DEFAULT_VL_DECAY_INTERVAL_SECONDS);
        this.vlDecayAmount = config.getInt("violation.vl-decay.amount", DEFAULT_VL_DECAY_AMOUNT);
        this.worldGuardEnabled = config.getBoolean("detection.worldguard.enabled", DEFAULT_WORLDGUARD_ENABLED);
        this.worldGuardDisabledRegions = config.getStringList("detection.worldguard.disabled-regions");
        this.foliaEnabled = config.getBoolean("folia.enabled", DEFAULT_FOLIA_ENABLED);
        this.foliaThreadPoolSize = config.getInt("folia.thread-pool-size", DEFAULT_FOLIA_THREAD_POOL_SIZE);
        this.foliaEntitySchedulerEnabled = config.getBoolean("folia.entity-scheduler.enabled", DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED);
        this.foliaRegionSchedulerEnabled = config.getBoolean("folia.region-scheduler.enabled", DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED);
        this.checkGreenThreshold = config.getDouble("checks.green-threshold", DEFAULT_CHECK_GREEN_THRESHOLD);
        this.checkYellowThreshold = config.getDouble("checks.yellow-threshold", DEFAULT_CHECK_YELLOW_THRESHOLD);
        this.checkOrangeThreshold = config.getDouble("checks.orange-threshold", DEFAULT_CHECK_ORANGE_THRESHOLD);
        this.checkGreenColor = config.getString("checks.green-color", DEFAULT_CHECK_GREEN_COLOR);
        this.checkYellowColor = config.getString("checks.yellow-color", DEFAULT_CHECK_YELLOW_COLOR);
        this.checkOrangeColor = config.getString("checks.orange-color", DEFAULT_CHECK_ORANGE_COLOR);
        this.checkRedColor = config.getString("checks.red-color", DEFAULT_CHECK_RED_COLOR);
        this.alertPrefix = config.getString("alerts.prefix", DEFAULT_ALERT_PREFIX);
    }

    private double clampThreshold(double value, String configPath, Logger logger) {
        if (value < 0.0 || value > 1.0) {
            double clamped = Math.max(0.0, Math.min(1.0, value));
            if (logger != null) {
                logger.warning("[Config] " + configPath + " value " + value +
                        " is outside valid range [0.0, 1.0], clamped to " + clamped);
            }
            return clamped;
        }
        return value;
    }

    // Геттеры для новых полей
    public double getCheckGreenThreshold() { return checkGreenThreshold; }
    public double getCheckYellowThreshold() { return checkYellowThreshold; }
    public double getCheckOrangeThreshold() { return checkOrangeThreshold; }
    public String getCheckGreenColor() { return checkGreenColor; }
    public String getCheckYellowColor() { return checkYellowColor; }
    public String getCheckOrangeColor() { return checkOrangeColor; }
    public String getCheckRedColor() { return checkRedColor; }
    public String getAlertPrefix() { return alertPrefix; }

    // Остальные геттеры
    public boolean isDebug() { return debug; }
    public int getPreHitTicks() { return preHitTicks; }
    public int getPostHitTicks() { return postHitTicks; }
    public double getHitLockThreshold() { return hitLockThreshold; }
    public int getPostHitTimeoutTicks() { return postHitTimeoutTicks; }
    public String getOutputDirectory() { return outputDirectory; }
    public boolean isAiEnabled() { return aiEnabled; }
    public String getAiApiKey() { return aiApiKey; }
    public double getAiAlertThreshold() { return aiAlertThreshold; }
    public boolean isAiConsoleAlerts() { return aiConsoleAlerts; }
    public double getAiBufferFlag() { return aiBufferFlag; }
    public double getAiBufferResetOnFlag() { return aiBufferResetOnFlag; }
    public double getAiBufferMultiplier() { return aiBufferMultiplier; }
    public double getAiBufferDecrease() { return aiBufferDecrease; }
    public int getAiSequence() { return aiSequence; }
    public int getAiStep() { return aiStep; }
    public double getAiPunishmentMinProbability() { return aiPunishmentMinProbability; }
    public String getCustomAlertPrefix() { return customAlertPrefix; }
    public boolean isAnimationEnabled() { return animationEnabled; }
    public String getPunishmentCommand(int vl) { return punishmentCommands.get(vl); }
    public Map<Integer, String> getPunishmentCommands() { return punishmentCommands; }
    public String getPrefix() { return prefix; }
    public String getMessage(String key) { return messages.getOrDefault(key, ""); }
    public String getMessage(String key, String player, double probability, double buffer, int vl) {
        String msg = getMessage(key);
        String playerValue = player != null ? player : "";
        String probValue = String.format("%.2f", probability);
        String bufferValue = String.format("%.1f", buffer);
        String vlValue = String.valueOf(vl);
        return msg
                .replace("{PLAYER}", playerValue)
                .replace("{PROBABILITY}", probValue)
                .replace("{BUFFER}", bufferValue)
                .replace("{VL}", vlValue)
                .replace("<player>", playerValue)
                .replace("<probability>", probValue)
                .replace("<buffer>", bufferValue)
                .replace("<vl>", vlValue);
    }
    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }
    public boolean isLiteBansEnabled() { return liteBansEnabled; }
    public String getLiteBansDbHost() { return liteBansDbHost; }
    public int getLiteBansDbPort() { return liteBansDbPort; }
    public String getLiteBansDbName() { return liteBansDbName; }
    public String getLiteBansDbUsername() { return liteBansDbUsername; }
    public String getLiteBansDbPassword() { return liteBansDbPassword; }
    public String getLiteBansTablePrefix() { return liteBansTablePrefix; }
    public int getLiteBansLookbackDays() { return liteBansLookbackDays; }
    public Set<String> getLiteBansCheatReasons() { return liteBansCheatReasons; }
    public boolean isAutostartEnabled() { return autostartEnabled; }
    public String getAutostartLabel() { return autostartLabel; }
    public String getAutostartComment() { return autostartComment; }
    public ServerType getServerType() { return serverType; }
    public String getServerAddress() { return serverAddress; }
    public int getReportStatsIntervalSeconds() { return reportStatsIntervalSeconds; }
    public String getServerHost() {
        int colonIndex = serverAddress.lastIndexOf(':');
        if (colonIndex > 0) {
            return serverAddress.substring(0, colonIndex);
        }
        return serverAddress;
    }
    public int getServerPort() {
        int colonIndex = serverAddress.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < serverAddress.length() - 1) {
            try {
                return Integer.parseInt(serverAddress.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                return 5000;
            }
        }
        return 5000;
    }
    public boolean isVlDecayEnabled() { return vlDecayEnabled; }
    public int getVlDecayIntervalSeconds() { return vlDecayIntervalSeconds; }
    public int getVlDecayAmount() { return vlDecayAmount; }
    public boolean isWorldGuardEnabled() { return worldGuardEnabled; }
    public List<String> getWorldGuardDisabledRegions() { return worldGuardDisabledRegions; }
    public boolean isFoliaEnabled() { return foliaEnabled; }
    public int getFoliaThreadPoolSize() { return foliaThreadPoolSize; }
    public boolean isFoliaEntitySchedulerEnabled() { return foliaEntitySchedulerEnabled; }
    public boolean isFoliaRegionSchedulerEnabled() { return foliaRegionSchedulerEnabled; }
}