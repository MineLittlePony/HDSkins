package com.minelittlepony.hdskins.skins;

import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.List;

public class SkinServerList implements Iterable<SkinServer> {

    private final List<SkinServer> servers;

    public SkinServerList(List<SkinServer> servers) {
        this.servers = servers;
    }

    public Iterator<SkinServer> iterator() {
        return servers.iterator();
    }

    public Iterator<SkinServer> getCycler() {
        return Iterators.cycle(servers);
    }
}
