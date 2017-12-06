/* 
 * Copyright 2017 Emmanuel Deviller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adlitteram.filenotifier.files;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbManager {

    private final Logger LOGGER = LoggerFactory.getLogger(DbManager.class);

    public final static String NAME_KEY = "*";
    private final static Long OLD_MAP = 0L;
    private final static Long CUR_MAP = 1L;
    private final static HashMap<Path, DbManager> DB_MAP = new HashMap<>();

    private final Path dbPath;
    private final Map<String, Long> oldMap;
    private final Map<String, Long> curMap;
    private DB db;

    private DbManager(Path dbPath) {
        LOGGER.info("Creating database: " + dbPath);
        this.dbPath = dbPath;

        try {
            db = create();
        }
        catch (IOError error) {
            LOGGER.warn("DbManager() : ", error);
            destroy();
            db = create();
        }

        Map<String, Long> map1 = db.getHashMap("map1");
        Map<String, Long> map2 = db.getHashMap("map2");
        Long val1 = map1.get(NAME_KEY);
        Long val2 = map2.get(NAME_KEY);
        LOGGER.info(dbPath + " - val1: " + val1 + " - val2: " + val2);
        map1.remove(NAME_KEY);
        map2.remove(NAME_KEY);

        if (Objects.equals(val1, OLD_MAP) && Objects.equals(val2, CUR_MAP)) {
            LOGGER.info(dbPath + " - oldMap = map1 & curMap = map2");
            oldMap = map1;
            curMap = map2;
        }
        else if (Objects.equals(val1, CUR_MAP) && Objects.equals(val2, OLD_MAP)) {
            LOGGER.info(dbPath + " - oldMap = map2 & curMap = map1");
            oldMap = map2;
            curMap = map1;
        }
        else {
            LOGGER.info(dbPath + " - oldMap cleared");
            oldMap = map1;
            curMap = map2;
            oldMap.clear();
        }

        curMap.clear();
        oldMap.put(NAME_KEY, CUR_MAP);
        curMap.put(NAME_KEY, OLD_MAP);

        LOGGER.info(dbPath + " - Compacting maps");
        db.commit();
        db.compact();;

        LOGGER.info(dbPath + " is ready");
    }

    public static synchronized DbManager createManager(Path dbPath) {
        DbManager dbManager = DB_MAP.get(dbPath);
        if (dbManager == null) {
            dbManager = new DbManager(dbPath);
            DB_MAP.put(dbPath, dbManager);
        }
        return dbManager;
    }

    private DB create() {
        return DBMaker.newFileDB(dbPath.toFile())
                .closeOnJvmShutdown()
                .make();
    }

    public void commit() {
        db.commit();
    }

    public void compact() {
        // Always Commit Before compact
        db.commit();
        db.compact();
    }

    public static void closeAll() {
        for (DbManager dbManager : DB_MAP.values()) {
            dbManager.close();
        }
    }

    public void close() {
        LOGGER.warn("DbManager.closing(): " + dbPath);

        if (db != null) {
            db.commit();
            db.close();
        }
        db = null;
    }

    public Map<String, Long> getOldMap() {
        return oldMap;
    }

    public Map<String, Long> getCurMap() {
        return curMap;
    }

    private void destroy() {
        close();

        try {
            Files.deleteIfExists(dbPath);
        }
        catch (IOException ex) {
            LOGGER.warn("FileSpooler.clearDB(): ", ex);
        }

        try {
            Files.deleteIfExists(dbPath.resolve(".p"));
        }
        catch (IOException ex) {
            LOGGER.warn("FileSpooler.clearDB(): ", ex);
        }
    }
}
