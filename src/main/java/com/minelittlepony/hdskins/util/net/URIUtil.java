package com.minelittlepony.hdskins.util.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.common.hash.Hashing;

import net.minecraft.client.MinecraftClient;

public interface URIUtil {

    static URI buildURI(String path, NameValuePair...parameters) throws URISyntaxException {
        URI uri = URI.create(path);
        return new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                uri.getPath(),
                Arrays.stream(parameters).map(NameValuePair::toURIComponent).collect(Collectors.joining("&")),
                uri.getFragment()
        );
    }

    public record NameValuePair(String name, String value) {
        String toURIComponent() {
            return URLEncoder.encode(name, StandardCharsets.UTF_8) + '=' + URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
    }

    static String getChecksum(URI uri) throws IOException {
        return Hashing.farmHashFingerprint64().hashBytes(getBytes(uri)).toString();
    }

    static byte[] getBytes(URI uri) throws IOException {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return Files.readAllBytes(Path.of(uri));
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)uri.toURL().openConnection(MinecraftClient.getInstance().getNetworkProxy());
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode / 100 != 2) {
                throw new IOException("Failed to open " + uri + ", HTTP error code: " + responseCode);
            }

            return connection.getInputStream().readAllBytes();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
