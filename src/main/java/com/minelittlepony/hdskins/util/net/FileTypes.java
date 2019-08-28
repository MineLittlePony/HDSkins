package com.minelittlepony.hdskins.util.net;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static String removeExtension(Path path) {
        String sPath = path.toString();
        String ext = getExtension(path);

        if (ext.isEmpty()) {
            return sPath;
        }
        return sPath.substring(0, sPath.length() - ext.length() - 1);
    }

    public static Path changeExtension(Path path, String extension) {

        String noExt = removeExtension(path);

        return Paths.get(noExt + extension);
    }
}
