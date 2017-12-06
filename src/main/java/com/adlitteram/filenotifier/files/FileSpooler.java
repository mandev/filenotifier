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

import com.adlitteram.filenotifier.ChangeListener;
import com.adlitteram.filenotifier.ChangeListenerPool;
import com.adlitteram.filenotifier.Channel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSpooler {

    private final Logger LOGGER = LoggerFactory.getLogger(FileSpooler.class);

    private final Channel channel;
    private final ArrayList<Path> rootPaths;
    private final Path dbPath;
    private Pattern includePattern;
    private Pattern excludePattern;
    private final ChangeListenerPool<Object> fileListeners;
    private final ChangeListenerPool<Object> spoolerListeners;
    private boolean interrupted = false;
    private FileWalker walker;
    private final long retainTime;

    public FileSpooler(Channel channel, Path dbPath, long retainTime) throws IOException {
        this.channel = channel;
        this.dbPath = dbPath;
        this.retainTime = retainTime;

        this.rootPaths = new ArrayList<>();
        this.spoolerListeners = new ChangeListenerPool<>();
        this.fileListeners = new ChangeListenerPool<>();
    }

    public Pattern getIncludePattern() {
        return includePattern;
    }

    public void setIncludePattern(Pattern includePattern) {
        this.includePattern = includePattern;
    }

    public Pattern getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(Pattern excludePattern) {
        this.excludePattern = excludePattern;
    }

    public void addPath(Path path) throws IOException {
        if (!rootPaths.contains(path)) {
            rootPaths.add(path);
        }
    }

    public ArrayList<Path> getRootPath() {
        return new ArrayList<>(rootPaths);
    }

    public Path getDbPath() {
        return dbPath;
    }

    public void run() {
        long time = System.currentTimeMillis();
        spoolerListeners.firechange(new RunEvent(channel, RunEvent.Type.START, "Starting spooler"));
        DbManager dbManager = DbManager.createManager(dbPath);

        try {

            for (Path rootPath : rootPaths) {
                walker = new FileWalker(channel, fileListeners, rootPath, dbManager.getOldMap(), dbManager.getCurMap(), includePattern, excludePattern, retainTime);
                Files.walkFileTree(rootPath, walker);
                if (interrupted) {
                    break;
                }
            }

            for (String key : dbManager.getOldMap().keySet()) {
                if (interrupted) {
                    break;
                }
                if (DbManager.NAME_KEY.equals(key) || ".".equals(key)) {
                    continue;
                }

                Path path = Paths.get(key);

                String name = path.getFileName().toString();
                if (includePattern != null && !includePattern.matcher(name).matches() && !Files.isDirectory(path)) {
                    continue;
                }
                if (excludePattern != null && excludePattern.matcher(name).matches()) {
                    continue;
                }

                for (Path rootPath : rootPaths) {
                    if (path.startsWith(rootPath)) {
                        String r = rootPath.toString();
                        String p = rootPath.relativize(path).toString();
                        fileListeners.firechange(new FileEvent(channel, r, p, FileEvent.Type.DELETE));
                        break;
                    }
                }
            }
            dbManager.commit();
        }
        catch (IOException ex) {
            LOGGER.warn(channel.getId() + " - FileSpooler.run(): ", ex);
        }
        finally {
            spoolerListeners.firechange(new RunEvent(channel, RunEvent.Type.STOP, "Spooler stopped"));
        }

        log("Spooler Time: " + (System.currentTimeMillis() - time));
    }

    public void addFileListener(ChangeListener<Object> cl) {
        fileListeners.addListener(cl);
    }

    public boolean removeFileListener(ChangeListener<Object> cl) {
        return fileListeners.removeListener(cl);
    }

    public ChangeListener<Object>[] getFileListeners() {
        return fileListeners.getListeners();
    }

    public void addSpoolerListener(ChangeListener<Object> cl) {
        spoolerListeners.addListener(cl);
    }

    public boolean removeSpoolerListener(ChangeListener<Object> cl) {
        return spoolerListeners.removeListener(cl);
    }

    public ChangeListener<Object>[] getSpoolerListeners() {
        return spoolerListeners.getListeners();
    }

    public void interrupt() {
        LOGGER.info("Interrupting FileSpooler");
        interrupted = true;
        if (walker != null) {
            walker.interrupt();
        }
    }

    private void log(String message) {
        LOGGER.info(channel.getId() + " - " + message);
    }
}
