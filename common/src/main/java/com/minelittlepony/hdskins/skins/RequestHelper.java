package com.minelittlepony.hdskins.skins;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for getting different response types from a http response.
 */
public class RequestHelper implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger();

    private final HttpRequest request;
    private final CloseableHttpResponse response;

    private String text;

    private RequestHelper(HttpRequest request, CloseableHttpResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpRequest request() {
        return request;
    }

    public HttpResponse response() {
        return response;
    }

    public boolean ok() {
        return responseCode() < HttpStatus.SC_MULTIPLE_CHOICES;
    }

    public void checkContent(String targetType) throws IOException {
        String actualType = contentType().getMimeType();
        if (!targetType.contentEquals(contentType().getMimeType())) {
            logDebugString();
            throw new IOException("Wrong content type. Expected " + targetType + " but got " + actualType +
                    ". Check the log for details.");
        }
    }

    public int responseCode() {
        return response().getStatusLine().getStatusCode();
    }

    public Optional<HttpEntity> entity() {
        return Optional.ofNullable(response().getEntity());
    }

    public ContentType contentType() {
        return entity()
                .map(ContentType::get)
                .orElse(ContentType.DEFAULT_TEXT);
    }

    public InputStream content() throws IOException {
        return response().getEntity().getContent();
    }

    public BufferedReader reader() throws IOException {
        Charset charset = contentType().getCharset();
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        return new BufferedReader(new InputStreamReader(content(), charset));
    }

    public String text() throws IOException {
        if (text == null) {
            try (BufferedReader reader = reader()) {
                text = CharStreams.toString(reader);
            }
        }
        return text;
    }

    public <T> T json(Gson gson, Class<T> type) throws IOException {
        return json(gson, (Type) type);
    }

    public <T> T json(Gson gson, Type type) throws IOException {
        checkContent("application/json");
        try {
            return gson.fromJson(text(), type);
        } catch (JsonParseException e) {
            LogManager.getLogger().warn("Failed to parse json.");
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        response.close();
    }

    public static RequestHelper execute(CloseableHttpClient client, HttpUriRequest request) throws IOException {
        final CloseableHttpResponse response = client.execute(request);
        return new RequestHelper(request, response);
    }

    public static NameValuePair[] mapAsParameters(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry ->
                        new BasicNameValuePair(entry.getKey(), entry.getValue())
                )
                .toArray(NameValuePair[]::new);
    }

    public void logDebugString() throws IOException {
        List<String> lines = new ArrayList<>();

        // request
        lines.add("> " + request().getRequestLine().toString());

        Arrays.stream(this.request().getAllHeaders())
                // hide any auth headers
                .filter(h -> !h.getName().equalsIgnoreCase("Authorization"))
                .map(Object::toString)
                .map(s1 -> "> " + s1)
                .forEach(lines::add);

        lines.add(">");

        // response
        lines.add("< " + response().getStatusLine().toString());

        Arrays.stream(this.response().getAllHeaders())
                .map(Object::toString)
                .map(s -> "< " + s)
                .forEach(lines::add);
        lines.add("<");

        // body
        lines.addAll(Arrays.asList(this.text().split("\r?\n")));

        logger.error("--- Start Debug Request Info ---");
        for (String line : lines) {
            logger.error(line);
        }
        logger.error("---  End Debug Request Info  --");
    }
}
