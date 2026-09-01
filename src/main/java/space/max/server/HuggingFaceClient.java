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

package space.max.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HuggingFaceClient implements IAIClient {
    private static final String INFERENCE_BASE = "https://api-inference.huggingface.co/models/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 30;

    private final String modelId;
    private final String baseUrl;
    private final String token;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private volatile boolean connected = true;

    public HuggingFaceClient(String modelId, String token) {
        String id = modelId == null ? "" : modelId.trim();
        this.modelId = id;
        this.baseUrl = (id.startsWith("http://") || id.startsWith("https://")) ? id : (INFERENCE_BASE + id);
        this.token = token != null && !token.isEmpty() ? token.trim() : null;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        connected = !modelId.isEmpty();
        return CompletableFuture.completedFuture(connected);
    }

    @Override
    public CompletableFuture<Boolean> connectWithRetry() {
        return connect();
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        connected = false;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<AIResponse> predict(byte[] playerData, String playerUuid) {
        if (!connected || modelId.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("HuggingFace client not configured or disconnected"));
        }
        String base64Data = Base64.getEncoder().encodeToString(playerData);
        JsonObject body = new JsonObject();
        JsonObject inputs = new JsonObject();
        inputs.addProperty("data", base64Data);
        body.add("inputs", inputs);

        Request.Builder reqBuilder = new Request.Builder()
            .url(baseUrl)
            .post(RequestBody.create(body.toString(), JSON));
        if (token != null) {
            reqBuilder.addHeader("Authorization", "Bearer " + token);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Response response = httpClient.newCall(reqBuilder.build()).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    return new AIResponse(0.0, "HTTP " + response.code() + ": " + responseBody);
                }
                double probability = parseProbability(responseBody);
                return new AIResponse(probability);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    private double parseProbability(String json) {
        try {
            JsonElement root = gson.fromJson(json, JsonElement.class);
            if (root == null) return 0.0;
            if (root.isJsonArray()) {
                JsonArray arr = root.getAsJsonArray();
                if (arr.size() > 0) {
                    JsonElement first = arr.get(0);
                    if (first.isJsonObject() && first.getAsJsonObject().has("probability")) {
                        return first.getAsJsonObject().get("probability").getAsDouble();
                    }
                }
            }
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("error")) return 0.0;
                if (obj.has("probability")) return obj.get("probability").getAsDouble();
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    @Override
    public boolean isConnected() {
        return connected && !modelId.isEmpty();
    }

    @Override
    public boolean isLimitExceeded() {
        return false;
    }

    @Override
    public String getSessionId() {
        return "hf-" + modelId;
    }

    @Override
    public String getServerAddress() {
        return baseUrl;
    }
}
