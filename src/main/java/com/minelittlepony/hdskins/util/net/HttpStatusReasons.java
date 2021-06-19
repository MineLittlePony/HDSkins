package com.minelittlepony.hdskins.util.net;

import java.util.Map;

import static java.util.Map.entry;

public class HttpStatusReasons {

    private static final String UNKNOWN_STATUS = "Unknown Status";

    private static final Map<Integer, String> REASONS = Map.ofEntries(
            entry(100, "Continue"),
            entry(101, "Switching Protocols"),
            entry(102, "Processing"),
            entry(200, "OK"),
            entry(201, "Created"),
            entry(202, "Accepted"),
            entry(203, "Non Authoritative Information"),
            entry(204, "No Content"),
            entry(205, "Reset Content"),
            entry(206, "Partial Content"),
            entry(207, "Multi Status"),
            entry(300, "Multiple Choices"),
            entry(301, "Moved Permanently"),
            entry(302, "Moved Temporarily"),
            entry(303, "See Other"),
            entry(304, "Not Modified"),
            entry(305, "Use Proxy"),
            entry(307, "Temporary Redirect"),
            entry(400, "Bad Request"),
            entry(401, "Unauthorized"),
            entry(402, "Payment Required"),
            entry(403, "Forbidden"),
            entry(404, "Not Found"),
            entry(405, "Method Not Allowed"),
            entry(406, "Not Acceptable"),
            entry(407, "Proxy Authentication Required"),
            entry(408, "Request Timeout"),
            entry(409, "Conflict"),
            entry(410, "Gone"),
            entry(411, "Length Required"),
            entry(412, "Precondition Failed"),
            entry(413, "Request Too Long"),
            entry(414, "Request URI Too Long"),
            entry(415, "Unsupported Media Type"),
            entry(416, "Requested Range Unsatisfiable"),
            entry(417, "Expectation Failed"),
            entry(419, "Insufficient Space On Resource"),
            entry(420, "Method Failure"),
            entry(422, "Unprocessable Entity"),
            entry(423, "Locked"),
            entry(424, "Failed Dependency"),
            entry(500, "Internal Server Error"),
            entry(501, "Not Implemented"),
            entry(502, "Bad Gateway"),
            entry(503, "Service Unavailable"),
            entry(504, "Gateway Timeout"),
            entry(505, "HTTP Version Not Supported"),
            entry(507, "Insufficient Storage")
    );

    public static String getReasonForStatus(int status) {
        return REASONS.getOrDefault(status, UNKNOWN_STATUS);
    }
}
