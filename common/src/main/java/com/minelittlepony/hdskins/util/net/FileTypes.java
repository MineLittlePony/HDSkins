package com.minelittlepony.hdskins.util.net;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

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
        return FilenameUtils.getExtension(path.getFileName().toString());
    }

    public static Path changeExtension(Path path, String extension) {
        String noExt = FilenameUtils.removeExtension(path.toString());

        return Paths.get(noExt + extension);
    }
}
