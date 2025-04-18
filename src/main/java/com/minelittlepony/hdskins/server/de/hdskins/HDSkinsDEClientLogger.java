package com.minelittlepony.hdskins.server.de.hdskins;

import com.minelittlepony.hdskins.client.HDSkins;
import de.hdskins.protocol.logger.InternalLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HDSkinsDEClientLogger implements InternalLogger {

    @Override
    public void handleException(@NotNull Throwable throwable) {
        HDSkins.LOGGER.error("An error occurred", throwable);
    }

    @Override
    public void debug(@NotNull String s) {
        HDSkins.LOGGER.debug(s);
    }

    @Override
    public void info(@NotNull String s) {
        HDSkins.LOGGER.info(s);
    }

    @Override
    public void warn(@NotNull String s) {
        HDSkins.LOGGER.warn(s);
    }

    @Override
    public void error(@NotNull String s, @Nullable Throwable throwable) {
        if (throwable != null) {
            HDSkins.LOGGER.error(s, throwable);
        } else {
            HDSkins.LOGGER.error(s);
        }
    }

}
