package com.minelittlepony.hdskins.util.net;

import java.io.IOException;

public class HttpException extends IOException {
    private static final long serialVersionUID = -6168434367054139332L;

    private final String reason;

    private final int statusCode;

    public HttpException(String message, MoreHttpResponses response) {
        super(message);

        this.reason = response.statusReason();
        this.statusCode = response.statusCode();
    }

    public String getReasonPhrase() {
        return reason;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
