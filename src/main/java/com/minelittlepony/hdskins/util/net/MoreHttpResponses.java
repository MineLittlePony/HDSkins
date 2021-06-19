package com.minelittlepony.hdskins.util.net;

import com.github.mizosoft.methanol.HttpStatus;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.TypeRef;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.util.UUIDTypeAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utility class for getting different response types from a http response.
 */
@FunctionalInterface
public interface MoreHttpResponses extends AutoCloseable {
    Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    HttpResponse<InputStream> response();

    /**
     * Gets the status code from the response.
     *
     * @return The status code
     */
    default int statusCode() {
        return response().statusCode();
    }

    /**
     * Gets the reason phrase/friendly name for the HTTP Status code.
     *
     * @return The reason phrase
     * @see HttpStatusReasons#getReasonForStatus(int)
     * @see #statusCode()
     */
    default String statusReason() {
        return HttpStatusReasons.getReasonForStatus(statusCode());
    }

    /**
     * Parses the {@code Content-Type} header as a {@link MediaType}. If none is
     * found, defaults to {@link MediaType#TEXT_ANY}.
     *
     * @return The content type
     */
    default MediaType contentType() {
        return response().headers().firstValue("Content-Type")
                .map(MediaType::parse)
                .orElse(MediaType.TEXT_ANY);
    }

    /**
     * Gets the {@link InputStream} from the response body.
     *
     * @return The input stream
     */
    default InputStream inputStream() {
        return response().body();
    }

    /**
     * Creates a new {@link BufferedReader} from the input stream using the
     * charset declared in the {@code Content-Type} header. If the charset
     * parameter is missing, UTF-8 is used.
     *
     * @return The new buffered reader
     */
    default BufferedReader reader() {
        return new BufferedReader(new InputStreamReader(inputStream(), contentType().charsetOrDefault(StandardCharsets.UTF_8)));
    }

    /**
     * Consumes the input stream. Call this when you don't care about the result
     * to prevent broken HTTP connections.
     *
     * @throws IOException If there is an error while reading
     * @see HttpResponse.BodySubscribers#ofInputStream() ofInputStream() API Note
     */
    default void consume() throws IOException {
        ByteStreams.exhaust(inputStream());
    }

    /**
     * Reads the response body and returns the raw bytes content as
     * {@code byte[]}. This consumes the response.
     *
     * @return The bytes content as a byte[]
     * @throws IOException
     */
    default byte[] bytes() throws IOException {
        return ByteStreams.toByteArray(inputStream());
    }

    /**
     * Reads the response body and returns the string content. This consumes the
     * response.
     *
     * @return The string content
     * @throws IOException If an io error occurs
     */
    default String text() throws IOException {
        return CharStreams.toString(reader());
    }

    /**
     * Parses the response body as json and returns the {@link JsonElement}.
     * This consumes the response.
     *
     * @return The json element
     * @throws IOException If an io error occurs
     */
    default JsonElement json() throws IOException {
        return json(JsonElement.class);
    }

    /**
     * Parses the response body as json and maps it to an object of {@code type}.
     * This consumes the response.
     *
     * @param type The class of the object mapping
     * @param <T>  The JSON's type
     * @return The object
     * @throws IOException If an io error occurs
     */
    default <T> T json(Class<T> type) throws IOException {
        return json(TypeRef.from(type));
    }

    /**
     * Parses the response body as json and maps it to an object of {@code type}.
     * This consumes the response.
     *
     * @param type The type of the object mapping
     * @param <T>  The JSON's type
     * @return The object
     * @throws IOException If an io error occurs
     */
    default <T> T json(TypeRef<T> type) throws IOException {
        if (!MediaType.APPLICATION_JSON.includes(contentType())) {
            String text = text();
            HDSkins.LOGGER.error("Server returned a non-json response!\n{}", text);
            throw new IOException("Server returned a non-json response!");
        }

        return GSON.fromJson(reader(), type.type());
    }

    /**
     * Ensures the request is successful. If it is not, an exception is thrown
     * with the status, reason, and url as the message.
     *
     * @return This object if the request is successful
     * @throws IOException If the request is not successful
     */
    default MoreHttpResponses requireOK() throws IOException {
        return requireOK(MoreHttpResponses::getMessageString);
    }

    /**
     * Ensures the request is successful. If it is not, an exception is thrown
     * using {@code errorHandler} to create the message.
     *
     * @param errorHandler The error handler used to get the exception message
     * @return This object if the request is successful
     * @throws IOException If the request is not successful
     */
    default MoreHttpResponses requireOK(ErrorHandler errorHandler) throws IOException {
        return require(moreHttpResponses -> HttpStatus.isSuccessful(moreHttpResponses.response()), errorHandler);
    }

    /**
     * Ensures the request is successful according to {@code successPredicate}.
     * If it is not, an exception is thrown with the status, reason, and url as
     * the message.
     *
     * @param successPredicate The predicate determining success
     * @return This object if the request is successful
     * @throws IOException If the request is not successful
     */
    default MoreHttpResponses require(SuccessPredicate successPredicate) throws IOException {
        return require(successPredicate, MoreHttpResponses::getMessageString);
    }

    /**
     * Ensures the request is successful according to {@code successPredicate}.
     * If it is not, an exception is thrown using {@code errorhandler} to create
     * the message.
     *
     * @param successPredicate The predicate determining success
     * @param errorHandler     The error handler used to get the exception message
     * @return This object if the request is successful
     * @throws IOException If the request is not successful
     */
    default MoreHttpResponses require(SuccessPredicate successPredicate, ErrorHandler errorHandler) throws IOException {
        if (!successPredicate.isSuccess(this)) {
            var message = errorHandler.getErrorMessage(this);
            // consume the input stream to prevent killed connections
            ByteStreams.exhaust(inputStream());

            throw new HttpException(message, this);
        }
        return this;
    }

    @Override
    default void close() throws IOException {
        inputStream().close();
    }

    private static String getMessageString(MoreHttpResponses that) {
        var status = that.statusCode();
        var reason = that.statusReason();
        var url = that.response().uri();

        if (HttpStatus.isInformational(that.response())) {
            return "%d Informational: %s for url: %s".formatted(status, reason, url);
        }
        if (HttpStatus.isSuccessful(that.response())) {
            return "%d Successful: %s for url: %s".formatted(status, reason, url);
        }
        if (HttpStatus.isClientError(that.response())) {
            return "%d Client Error: %s for url: %s".formatted(status, reason, url);
        }
        if (HttpStatus.isServerError(that.response())) {
            return "%d Server Error: %s for url: %s".formatted(status, reason, url);
        }
        throw new RuntimeException("Unexpected status code %d: %s for url %s".formatted(status, reason, url));
    }

    static MoreHttpResponses execute(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return () -> response;
    }

    @FunctionalInterface
    interface SuccessPredicate {
        boolean isSuccess(MoreHttpResponses response) throws IOException;
    }

    @FunctionalInterface
    interface ErrorHandler {
        String getErrorMessage(MoreHttpResponses response) throws IOException;
    }
}
