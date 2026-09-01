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

package space.max.checks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CheckHistoryManager {

    private static final int GROUP_SIZE = 10;
    private static final int MAX_GROUPS = 27;

    private final Map<UUID, Deque<double[]>> historyMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> nameMap = new ConcurrentHashMap<>();
    private final Map<UUID, List<Double>> currentGroup = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCheckTime = new ConcurrentHashMap<>();

    public void addCheck(UUID playerId, String playerName, double probability) {
        List<Double> group = currentGroup.computeIfAbsent(playerId, k -> new ArrayList<>(GROUP_SIZE));
        group.add(probability);

        // Обновляем время последней проверки
        lastCheckTime.put(playerId, System.currentTimeMillis());

        if (group.size() >= GROUP_SIZE) {
            double[] arr = new double[GROUP_SIZE];
            for (int i = 0; i < GROUP_SIZE; i++) {
                arr[i] = group.get(i);
            }
            Deque<double[]> deque = historyMap.computeIfAbsent(playerId, k -> new ArrayDeque<>(MAX_GROUPS));
            synchronized (deque) {
                if (deque.size() >= MAX_GROUPS) {
                    deque.removeFirst();
                }
                deque.addLast(arr);
            }
            currentGroup.remove(playerId);
        }
        nameMap.put(playerId, playerName);
    }

    public List<double[]> getGroupsForDisplay(UUID playerId) {
        Deque<double[]> deque = historyMap.get(playerId);
        List<double[]> groups = new ArrayList<>();
        if (deque != null) {
            synchronized (deque) {
                groups.addAll(deque);
            }
        }

        List<Double> current = currentGroup.get(playerId);
        if (current != null && !current.isEmpty()) {
            double[] arr = new double[current.size()];
            for (int i = 0; i < current.size(); i++) {
                arr[i] = current.get(i);
            }
            groups.add(arr);
        }

        if (groups.size() > MAX_GROUPS) {
            groups = new ArrayList<>(groups.subList(groups.size() - MAX_GROUPS, groups.size()));
        }
        return groups;
    }

    public double getOverallAverage(UUID playerId) {
        List<double[]> groups = getGroupsForDisplay(playerId);
        if (groups.isEmpty()) return 0.0;

        double sum = 0.0;
        for (double[] group : groups) {
            sum += Arrays.stream(group).average().orElse(0.0);
        }
        return sum / groups.size();
    }

    public List<Double> getLastChecks(UUID playerId, int count) {
        List<Double> allChecks = new ArrayList<>();

        Deque<double[]> deque = historyMap.get(playerId);
        if (deque != null) {
            synchronized (deque) {
                for (double[] group : deque) {
                    for (double p : group) {
                        allChecks.add(p);
                    }
                }
            }
        }

        List<Double> current = currentGroup.get(playerId);
        if (current != null) {
            allChecks.addAll(current);
        }

        if (allChecks.size() <= count) {
            return allChecks;
        }
        return new ArrayList<>(allChecks.subList(allChecks.size() - count, allChecks.size()));
    }

    public double getAverageOfLastGroups(UUID playerId, int count) {
        List<double[]> groups = getGroupsForDisplay(playerId);
        if (groups.isEmpty()) return 0.0;

        List<Double> averages = new ArrayList<>();
        for (double[] group : groups) {
            averages.add(Arrays.stream(group).average().orElse(0.0));
        }

        if (averages.size() > count) {
            averages = new ArrayList<>(averages.subList(averages.size() - count, averages.size()));
        }
        return averages.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public List<Double> getLastGroupAverages(UUID playerId, int count) {
        List<double[]> groups = getGroupsForDisplay(playerId);
        if (groups.isEmpty()) return Collections.emptyList();

        List<Double> averages = new ArrayList<>();
        for (double[] group : groups) {
            averages.add(Arrays.stream(group).average().orElse(0.0));
        }

        if (averages.size() > count) {
            averages = new ArrayList<>(averages.subList(averages.size() - count, averages.size()));
        }
        return averages;
    }

    public String getPlayerName(UUID playerId) {
        return nameMap.get(playerId);
    }

    public long getLastCheckTime(UUID playerId) {
        return lastCheckTime.getOrDefault(playerId, 0L);
    }

    public void clear(UUID playerId) {
        historyMap.remove(playerId);
        nameMap.remove(playerId);
        currentGroup.remove(playerId);
        lastCheckTime.remove(playerId);
    }

    public void clearAll() {
        historyMap.clear();
        nameMap.clear();
        currentGroup.clear();
        lastCheckTime.clear();
    }
}