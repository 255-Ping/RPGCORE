package com.github._255_ping.rpg.core.persistence;

import com.github._255_ping.rpg.api.persistence.DataStore;

import java.io.File;

public final class YamlDataStore implements DataStore {

    private final File baseDir;

    public YamlDataStore(File baseDir) {
        this.baseDir = baseDir;
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IllegalStateException("Could not create data dir: " + baseDir);
        }
    }

    @Override
    public Repository repository(String name) {
        return new YamlRepository(new File(baseDir, name));
    }
}
