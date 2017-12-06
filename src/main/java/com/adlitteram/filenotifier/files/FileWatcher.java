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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import name.pachler.nio.file.ext.ExtendedWatchEventModifier;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWatcher implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(FileWatcher.class);

    private static final WatchEvent.Kind[] WEK1 = new WatchEvent.Kind[]{
        StandardWatchEventKind.ENTRY_CREATE,
        StandardWatchEventKind.ENTRY_DELETE,
        StandardWatchEventKind.ENTRY_MODIFY,
        ExtendedWatchEventKind.ENTRY_RENAME_FROM,
        ExtendedWatchEventKind.ENTRY_RENAME_TO,
        StandardWatchEventKind.OVERFLOW};

    private static final WatchEvent.Kind[] WEK2 = new WatchEvent.Kind[]{
        StandardWatchEventKind.ENTRY_CREATE,
        StandardWatchEventKind.ENTRY_DELETE,
        StandardWatchEventKind.ENTRY_MODIFY,
        StandardWatchEventKind.OVERFLOW};

    private static final WatchEvent.Modifier[] WEM = new WatchEvent.Modifier[]{
        ExtendedWatchEventModifier.FILE_TREE};

    //@SuppressWarnings("rawtypes")
    private Channel channel;
    private java.nio.file.Path dbPath;
    private Pattern includePattern;
    private Pattern excludePattern;
    private WatchEvent.Kind[] eventKinds;
    private WatchEvent.Modifier[] eventModifiers;
    private WatchService watchService;
    private Map<WatchKey, Path> keys;
    private ChangeListenerPool<Object> fileListeners;
    private ChangeListenerPool<Object> watcherListeners;
    private boolean interrupted = false;
    private boolean waiting = false;

    public FileWatcher(Channel channel, java.nio.file.Path dbPath) {
        this(channel, dbPath, false);
    }

    public FileWatcher(Channel channel, java.nio.file.Path dbPath, boolean b) {
        this(channel, dbPath, b ? WEK1 : WEK2, WEM);
    }

    public FileWatcher(Channel channel, java.nio.file.Path dbPath, WatchEvent.Kind[] standardEvents, WatchEvent.Modifier[] eventModifiers) {
        this.channel = channel;
        this.dbPath = dbPath;
        this.eventKinds = standardEvents;
        this.eventModifiers = eventModifiers;
        this.fileListeners = new ChangeListenerPool<>();
        this.watcherListeners = new ChangeListenerPool<>();
        this.keys = new HashMap<>();
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    public void addWatcherListener(ChangeListener<Object> cl) {
        watcherListeners.addListener(cl);
    }

    public boolean removeWatcherListener(ChangeListener<Object> cl) {
        return watcherListeners.removeListener(cl);
    }

    public ChangeListener<Object>[] getWatcherListeners() {
        return watcherListeners.getListeners();
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

    public synchronized void addPath(java.nio.file.Path path) throws IOException {
        Path cpath = Paths.get(path.toString());
        WatchKey key = cpath.register(watchService, eventKinds, eventModifiers);
        keys.put(key, cpath);
    }

    public synchronized void removePath(java.nio.file.Path path) {
        Path cpath = Paths.get(path.toString());
        for (Map.Entry<WatchKey, Path> entry : keys.entrySet()) {
            if (entry.getValue().equals(cpath)) {
                entry.getKey().cancel();
                keys.remove(entry.getKey());
                break;
            }
        }
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

    @Override
    public void run() {
        watcherListeners.firechange(new RunEvent(channel, RunEvent.Type.START, "Watcher Started"));
        DbManager dbManager = DbManager.createManager(dbPath);
        int counter = 0;

        while (!interrupted) {
            WatchKey signalledKey;
            try {
                waiting = true;
                // take() will block until a file has been created/deleted
                signalledKey = watchService.take();
                waiting = false;
                if (interrupted) {
                    break;
                }
            }
            catch (InterruptedException ie) {
                logger.info(channel.getId() + " - Watch service interrupted, terminating: {}", ie.getMessage());
                break;
            }
            catch (ClosedWatchServiceException ice) {
                logger.warn(channel.getId() + " - Watch service closed, terminating: {}", ice.getMessage());
                break;
            }

            // get list of events from key
            String root = keys.get(signalledKey).toString();
            List<WatchEvent<?>> watchEventList = signalledKey.pollEvents();
            counter += watchEventList.size();

            // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
            // key to be reported again by the watch service
            signalledKey.reset();

            Path rpath = null;

            for (WatchEvent we : watchEventList) {
                FileEvent fe = null;

                if (we.kind() == StandardWatchEventKind.ENTRY_CREATE) {
                    Path cpath = (Path) we.context();
                    BasicFileAttributes attrs = getAttributes(root, cpath);
                    if (attrs != null && acceptPath(cpath, attrs)) {
                        fe = new FileEvent(channel, root, cpath.toString(), FileEvent.Type.CREATE);
                        dbManager.getCurMap().put(cpath.toString(), Long.valueOf(attrs.lastModifiedTime().toMillis()));
                    }
                }
                else if (we.kind() == StandardWatchEventKind.ENTRY_DELETE) {
                    Path cpath = (Path) we.context();
                    if (acceptPath(cpath, null)) {
                        fe = new FileEvent(channel, root, cpath.toString(), FileEvent.Type.DELETE);
                        dbManager.getCurMap().remove(cpath.toString());
                    }
                }
                else if (we.kind() == StandardWatchEventKind.ENTRY_MODIFY) {
                    Path cpath = (Path) we.context();
                    BasicFileAttributes attrs = getAttributes(root, cpath);
                    if (attrs != null && !attrs.isDirectory() && acceptPath(cpath, attrs)) {
                        fe = new FileEvent(channel, root, cpath.toString(), FileEvent.Type.MODIFY);
                        dbManager.getCurMap().put(cpath.toString(), Long.valueOf(attrs.lastModifiedTime().toMillis()));
                    }
                }
                else if (we.kind() == ExtendedWatchEventKind.ENTRY_RENAME_FROM) {
                    rpath = (Path) we.context();
                }
                else if (we.kind() == ExtendedWatchEventKind.ENTRY_RENAME_TO) {
                    Path cpath = (Path) we.context();
                    BasicFileAttributes attrs = getAttributes(root, cpath);
                    if (attrs != null && acceptPath(cpath, attrs)) {
                        if (rpath != null) {
                            fe = new FileEvent(channel, root, rpath.toString(), cpath.toString(), FileEvent.Type.RENAME);
                            dbManager.getCurMap().remove(rpath.toString());
                            dbManager.getCurMap().put(cpath.toString(), Long.valueOf(attrs.lastModifiedTime().toMillis()));
                        }
                        else {
                            logger.warn(channel.getId() + " - Unvalid rename event: {}", cpath.toString());
                        }
                    }
                }
                // StandardWatchEventKind.OVERFLOW
                else {
                    Path cpath = (Path) we.context();
                    BasicFileAttributes attrs = getAttributes(root, cpath);
                    if (acceptPath(cpath, attrs)) {
                        fe = new FileEvent(channel, root, cpath.toString(), FileEvent.Type.UNKNOWN);
                    }
                }

                if (fe != null) {
                    fileListeners.firechange(fe);
                }
            }
            dbManager.commit();
            if (counter > 10000) {
                log("Compacting database " + dbPath);
                dbManager.compact();
                counter = 0;
            }
        }
        close();
        watcherListeners.firechange(new RunEvent(channel, RunEvent.Type.STOP, "Watcher stopped"));
    }

    private BasicFileAttributes getAttributes(String root, Path cpath) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(root + "/" + cpath.toString());
            if (Files.exists(path)) {
                return Files.readAttributes(path, BasicFileAttributes.class);
            }
        }
        catch (IOException ex) {
            logger.warn(channel.getId(), ex);
        }
        return null;
    }

    private boolean acceptPath(Path cpath, BasicFileAttributes attrs) {

        String name = FilenameUtils.getName(cpath.toString());

        if (attrs != null && !attrs.isDirectory()
            && includePattern != null && !includePattern.matcher(name).matches()) {
            return false;
        }

        if (excludePattern != null && excludePattern.matcher(name).matches()) {
            return false;
        }

        return true;
    }

    private void close() {
        try {
            if (watchService != null) {
                watchService.close();
                watchService = null;
            }
        }
        catch (IOException ex) {
            logger.warn(channel.getId(), ex);
        }
    }

    public void interrupt() {
        log("Interrupting FileWatcher");
        interrupted = true;
        if (waiting) {
            close();
        }
    }

    private void log(String message) {
        logger.info(channel.getId() + " - " + message);
    }
}
