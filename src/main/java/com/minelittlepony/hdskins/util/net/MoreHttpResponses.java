package com.minelittlepony.hdskins.util.net;

import com.google.common.io.CharStreams;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.util.UUIDTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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
        } catch (InterruptedException | UnresolvedAddressException e) {
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

    default <T> T json(Class<T> type, @Nullable String errorMessage) throws IOException {
        return json((Type) type, errorMessage);
    }

    default <T> T json(Type type, String errorMessage) throws IOException {
        return json(type, () -> errorMessage);
    }

    default <T> T json(Class<T> type, Supplier<String> errorMessage) throws IOException {
        return json((Type) type, errorMessage);
    }

    default <T> T json(Type type, Supplier<String> errorMessage) throws IOException {
        String text = text();
        if (contentTypeMatches(FileTypes.APPLICATION_JSON)) {
            return GSON.fromJson(text, type);
        }
        throw new HttpException(String.format("%s\n%s", errorMessage.get(), text), response().statusCode(), null);
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

    default <T> Optional<T> accept(Untrusted<MoreHttpResponses, T> function) throws IOException {
        return ok() ? Optional.of(function.apply(this)) : Optional.empty();
    }

    interface Untrusted<A, B> {
        B apply(A a) throws IOException;
    }
}
