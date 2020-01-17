package com.minelittlepony.hdskins.util.net;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

/**
 * Utility class for getting different response types from a http response.
 */
@FunctionalInterface
public interface MoreHttpResponses extends AutoCloseable {

    CloseableHttpResponse getResponse();

    default boolean ok() {
        return getResponseCode() < 400;
    }

    default int getResponseCode() {
        return getResponse().getStatusLine().getStatusCode();
    }

    default String getContentType() {
        return getResponse().getEntity().getContentType().getValue();
    }

    default InputStream getInputStream() throws IOException {
        return getResponse().getEntity().getContent();
    }

    default BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    default byte[] bytes() throws IOException {
        try (InputStream input = getInputStream()) {
            return ByteStreams.toByteArray(input);
        }
    }

    default String text() throws IOException {
        try (BufferedReader reader = getReader()) {
            return CharStreams.toString(reader);
        }
    }

    default Stream<String> lines() throws IOException {
        try (BufferedReader reader = getReader()) {
            return reader.lines();
        }
    }

    default JsonElement json() throws IOException, JsonParseException {
        if (!"application/json".equals(getContentType())) {
            throw new JsonParseException("Wrong content-type. Expected application/json, got " + getContentType());
        }
        try (BufferedReader reader = getReader()) {
            return new Gson().fromJson(reader, JsonElement.class);
        }
    }

    @Override
    default void close() throws IOException {
        this.getResponse().close();
    }

    static MoreHttpResponses execute(CloseableHttpClient client, HttpUriRequest request) throws IOException {
        CloseableHttpResponse response = client.execute(request);
        return () -> response;
    }
}
