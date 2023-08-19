package com.minelittlepony.hdskins.util.net;

import com.google.common.io.CharStreams;
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
import java.util.Map;
import java.util.Optional;
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
        var json = json(type);
        return json.getKey().orElseThrow(() -> new HttpException(errorMessage + "\n" + json.getValue(), response().statusCode(), null));
    }

    default <T> Map.Entry<Optional<T>, String> json(Class<T> type) throws IOException {
        String text = text();
        if (contentTypeMatches(FileTypes.APPLICATION_JSON)) {
            return Map.entry(Optional.ofNullable(GSON.fromJson(text, type)), text);
        }
        return Map.entry(Optional.empty(), text);
    }

    default boolean ok() {
        return response().statusCode() < SC_MULTIPLE_CHOICES;
    }

    default MoreHttpResponses requireOk() throws IOException {
        if (!ok()) {
            throw HttpException.of(this);
        }
        return this;
    }
}
