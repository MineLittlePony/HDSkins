package com.minelittlepony.hdskins.util.net;

import java.io.IOException;
import java.util.Set;
import com.google.gson.JsonObject;

public class HttpException extends IOException {
    private static final long serialVersionUID = -6168434367054139332L;

    private static final Set<String> MESSAGE_FIELDS = Set.of("message", "detail");

    private final String reason;

    private final int statusCode;

    HttpException(String reason, int statusCode, Throwable cause) {
        super("(" + statusCode + ") " + reason, cause);

        this.reason = reason;
        this.statusCode = statusCode;
    }

    public String getReasonPhrase() {
        return reason;
    }

    public int getStatusCode() {
        return statusCode;
    }

    static HttpException of(MoreHttpResponses response) throws IOException {
        return new HttpException(
            response.json(JsonObject.class)
                .getKey()
                .map(HttpException::getMessage)
                .orElseGet(() -> "Server did not respond correctly. Status Code " + response.response().statusCode()),
            response.response().statusCode(),
            null
        );
    }

    static String getMessage(JsonObject json) {
        return MESSAGE_FIELDS.stream().filter(json::has).map(json::get).findFirst().orElse(json).getAsString();
    }

    interface UnsafeSupplier<T> {
        T get() throws IOException;
    }
}
