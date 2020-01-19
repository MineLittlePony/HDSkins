package com.minelittlepony.hdskins.client;

import com.google.common.net.InetAddresses;
import org.apache.logging.log4j.LogManager;

import java.util.HashSet;
import java.util.Set;

public class PendingTextureDomains {

    private static Set<String> invalids = new HashSet<>();
    private static Set<String> pending = new HashSet<>();

    public static void addPending(String domain) {
        if (InetAddresses.isInetAddress(domain) || domain.split("\\.").length > 2) {
            if (pending.add(domain)) {
                LogManager.getLogger().info("Got unknown domain {} pending approval.", domain);
            }
        } else if (invalids.add(domain)) {
            LogManager.getLogger().warn("Got a non-qualified hostname ({}). Check with your server admin to make it fully qualified.", domain);
        }
    }

    public static Set<String> getPending() {
        return pending;
    }

}
