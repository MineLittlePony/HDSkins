package com.minelittlepony.hdskins.util.net;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Flow.Subscriber;

import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Strings;

public interface FileTypes {
    String MULTI_PART_BOUNDARY = "MULTI-PART_BOUNDARY";

    String HEADER_ACCEPT = "Accept";
    String HEADER_CONTENT_TYPE = "Content-Type";
    String HEADER_AUTHORIZATION = "Authorization";

    String APPLICATION_JSON = "application/json";
    String APPLICATION_OCTET_STREAM = "application/octet-stream";
    String MULTI_PART_FORM_DATA = "multipart/form-data; boundary=\"" + MULTI_PART_BOUNDARY + "\"";

    static String getMimeType(Path path) {
        try {
            return Strings.nullToEmpty(Files.probeContentType(path));
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    static String getExtension(Path path) {
        return FilenameUtils.getExtension(path.getFileName().toString());
    }

    static Path changeExtension(Path path, String extension) {
        return Paths.get(FilenameUtils.removeExtension(path.toString()) + extension);
    }

    static MultiPartBuilder multiPart() {
        return new MultiPartBuilder();
    }

    static MultiPartBuilder multiPart(Map<String, ?> fields) {
        var builder = multiPart();
        fields.forEach(builder::field);
        return builder;
    }

    class MultiPartBuilder {
        private static final byte[] HEAD = ("--" + MULTI_PART_BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8);
        private static final byte[] NEWLINE = "\r\n".getBytes(StandardCharsets.UTF_8);
        private static final byte[] TAIL = ("--" + MULTI_PART_BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8);

        private final List<byte[]> buffer = new ArrayList<>();
        private long length = 0;

        public MultiPartBuilder field(String name, Object value) {
            append(HEAD);
            append("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
            append("Content-Type: text/plain; charset=UTF-8\r\n");
            append(NEWLINE);
            append(String.valueOf(value) + "\r\n");
            return this;
        }

        public MultiPartBuilder field(String name, URI file) throws IOException {
            if ("file".equals(file.getAuthority())) {
                return field(name, Path.of(file));
            }
            return field(name, file.toString());
        }

        public MultiPartBuilder field(String name, Path file) throws IOException {
            append(HEAD);
            append("Content-Disposition: form-data;"
                    + " name=\"" + name + "\";"
                    + " filename=\"" + file.getFileName().toString() + "\"\r\n");
            append("Content-Type: " + getMimeType(file) + "\r\n");
            append(NEWLINE);
            append(Files.readAllBytes(file));
            append(NEWLINE);
            return this;
        }

        public MultiPartBuilder append(String data) {
            return append(data.getBytes(StandardCharsets.UTF_8));
        }

        public MultiPartBuilder append(byte[] data) {
            buffer.add(data);

            length += data.length;
            return this;
        }

        public BodyPublisher build() {
            append(TAIL);

            StringBuilder builder = new StringBuilder();
            for (byte[] chunk : buffer) {
                builder.append(new String(chunk, StandardCharsets.UTF_8));
            }

            System.out.println("Request Body:");
            System.out.println(builder.toString());


            BodyPublisher publisher = BodyPublishers.ofByteArrays(buffer);
            return new BodyPublisher() {
                @Override
                public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                    publisher.subscribe(subscriber);
                }

                @Override
                public long contentLength() {
                    return length;
                }
            };
        }
    }
}
