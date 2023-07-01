package com.minelittlepony.hdskins.client.filedialog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.spongepowered.include.com.google.common.base.Strings;

import com.minelittlepony.hdskins.client.HDSkins;

import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;

public interface FileSystemUtil {
    boolean IS_LINUX = Util.getOperatingSystem() == OperatingSystem.LINUX;
    boolean IS_SANDBOXED = isSandboxed();

    String CONTENT_TYPE_DOWNLOAD = "Download";

    static boolean isSandboxed() {
        try {
            Path testPath = getUserDirectory().resolve(".hdskinsfstest" + System.currentTimeMillis() + ".tmp");
            try {
                Files.createFile(testPath);
                return !Files.isReadable(testPath);
            } finally {
                Files.deleteIfExists(testPath);
            }
        } catch (Exception e) {
            HDSkins.LOGGER.error(e);
        }

        return true;
    }

    static Path getActiveDirectory() {
        return Path.of("/");
    }

    static Path getUserDirectory() {
        String path = Strings.nullToEmpty(System.getProperty("user.home"));

        if (path.isEmpty()) {
            return getActiveDirectory();
        }

        try {
            return Path.of(path);
        } catch (Exception e) {
            HDSkins.LOGGER.error(e);
        }

        return getActiveDirectory();
    }

    static Path getUserContentDirectory(String contentType) {
        Path userDirectory = getUserDirectory();
        if (userDirectory.getParent() == null) {
            return userDirectory;
        }

        do {
            Path candidate;
            if (Files.isDirectory(candidate = userDirectory.resolve(contentType))) {
                return candidate;
            }

            if (Files.isDirectory(candidate = userDirectory.resolve(contentType.toLowerCase(Locale.ROOT)))) {
                return candidate;
            }

            contentType += "s";
        } while (!contentType.endsWith("ss"));

        return userDirectory;
    }
}
