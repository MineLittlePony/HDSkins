package com.minelittlepony.hdskins.util.net;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import java.io.IOException;

public class HttpException extends IOException {
    private static final long serialVersionUID = -6168434367054139332L;

    private final String reason;

    private final int statusCode;

    @Deprecated
    public HttpException(HttpResponse response) {
        this(response.getStatusLine());
    }

    @Deprecated
    public HttpException(StatusLine status) {
        this(status.getReasonPhrase(), status.getStatusCode(), null);
    }

    public HttpException(String reason, int statusCode, Throwable cause) {
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
}
