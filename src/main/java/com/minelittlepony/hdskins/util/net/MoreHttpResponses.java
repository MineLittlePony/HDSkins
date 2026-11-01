package com.minelittlepony.hdskins.util.net;

import com.google.common.io.CharStreams;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.util.UUIDTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for getting different response types from a http response.
 */
@FunctionalInterface
public interface MoreHttpResponses {
    int SC_MULTIPLE_CHOICES = 300;
    Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    Function<URI, HttpClient> HTTP_CLIENTS = Util.memoize((hostname) -> {
        var builder = HttpClient.newBuilder();
        // The java.net.http client defaults to using http/2 for all requests.
        // Some servers don't behave with http/2 on an insecure local connection.
        // So set the http version to 1.1 for those servers.
        if ("http".equalsIgnoreCase(hostname.getScheme())) {
            builder.version(HttpClient.Version.HTTP_1_1);
        }
        return builder.build();
    });

    static MoreHttpResponses execute(HttpRequest.Builder request) throws IOException {
        return execute(request.build());
    }

    static MoreHttpResponses execute(HttpRequest request) throws IOException {
        HDSkinsServer.LOGGER.debug("Executing web request to {}", request::uri);
        // Get a client from the url resolved to its root
        var CLIENT = HTTP_CLIENTS.apply(request.uri().resolve("/"));
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
