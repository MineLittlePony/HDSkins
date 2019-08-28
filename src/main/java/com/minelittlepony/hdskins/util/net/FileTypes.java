package com.minelittlepony.hdskins.util.net;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Strings;

public class FileTypes {

    public static String getMimeType(Path path) {
        try {
            return Strings.nullToEmpty(Files.probeContentType(path));
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    public static String getExtension(Path path) {
        String[] split = path.getFileName().toString().split("\\.");
        return split.length > 1 ? split[split.length - 1] : "";
    }
}
