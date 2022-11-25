package com.minelittlepony.hdskins.util.net;

import com.google.common.io.CharStreams;
import com.google.gson.JsonObject;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.util.UUIDTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utility class for getting different response types from a http response.
 */
@FunctionalInterface
public interface MoreHttpResponses {
    int SC_MULTIPLE_CHOICES = 300;
    HttpClient CLIENT = HttpClient.newHttpClient();
    Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    static MoreHttpResponses execute(HttpRequest request) throws IOException {
        try {
            HttpResponse<InputStream> response = CLIENT.send(request, BodyHandlers.ofInputStream());
            return () -> response;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    HttpResponse<InputStream> response();

    default boolean contentTypeMatches(String contentType) {
        return response()
                .headers()
                .allValues(FileTypes.HEADER_CONTENT_TYPE)
                .stream()
                .anyMatch(s -> s.toLowerCase().contains(contentType));
    }

    default BufferedReader reader() throws IOException {
        return new BufferedReader(new InputStreamReader(response().body(), StandardCharsets.UTF_8));
    }

    default String text() throws IOException {
        try (BufferedReader reader = reader()) {
            return CharStreams.toString(reader);
        }
    }

    default <T> T json(Class<T> type, String errorMessage) throws IOException {

        if (!contentTypeMatches(FileTypes.APPLICATION_JSON)) {
            String text = text();
            HDSkins.LOGGER.error(errorMessage, text);
            throw new HttpException(text, response().statusCode(), null);
        }

        String text = text();

        T t = GSON.fromJson(text, type);
        if (t == null) {
            throw new HttpException(errorMessage + "\n " + text, response().statusCode(), null);
        }
        return t;
    }

    default boolean ok() {
        return response().statusCode() < SC_MULTIPLE_CHOICES;
    }

    default MoreHttpResponses requireOk() throws IOException {
        if (!ok()) {
            JsonObject json = json(JsonObject.class, "Server did not respond correctly. Status Code " + response().statusCode());
            if (json.has("message")) {
                throw new HttpException(json.get("message").getAsString(), response().statusCode(), null);
            } else {
                throw new HttpException(json.toString(), response().statusCode(), null);
            }
        }
        return this;
    }
}
