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
public class ActionParser {
    public ParsedAction parse(String rawCommand) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) {
            return new ParsedAction(ActionType.RAW, "");
        }
        ActionType type = ActionType.fromCommand(rawCommand);
        String command = type.stripPrefix(rawCommand);
        return new ParsedAction(type, command);
    }
    public boolean hasActionPrefix(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        ActionType type = ActionType.fromCommand(command);
        return type != ActionType.RAW;
    }
}