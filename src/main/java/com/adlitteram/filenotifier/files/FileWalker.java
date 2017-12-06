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

import com.adlitteram.filenotifier.ChangeListenerPool;
import com.adlitteram.filenotifier.Channel;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWalker implements FileVisitor<Path> {

    private final Logger LOGGER = LoggerFactory.getLogger(FileWalker.class);

    private final Map<String, Long> oldMap;
    private final Map<String, Long> curMap;
    private final Channel channel;
    private final ChangeListenerPool<Object> fileListeners;
    private final Path rootPath;
    private boolean interrupted = false;
    private final Pattern includePattern;
    private final Pattern excludePattern;
    private final long retainTime;

    public FileWalker(Channel channel, ChangeListenerPool<Object> fileListeners, Path rootPath, Map<String, Long> oldMap, Map<String, Long> curMap, Pattern includePattern, Pattern excludePattern, long retainTime) {
        this.channel = channel;
        this.fileListeners = fileListeners;
        this.rootPath = rootPath;
        this.oldMap = oldMap;
        this.curMap = curMap;
        this.excludePattern = excludePattern;
        this.includePattern = includePattern;
        this.retainTime = retainTime;
    }

    private void visit(Path path, BasicFileAttributes attrs) throws IOException {
        String key = path.toString();
        if (key.isEmpty()) {
            return;
        }

        String name = path.getFileName().toString();
        if (includePattern != null && !includePattern.matcher(name).matches() && !attrs.isDirectory()) {
            return;
        }
        if (excludePattern != null && excludePattern.matcher(name).matches()) {
            return;
        }

        long filetime = Math.max(attrs.lastModifiedTime().toMillis(), attrs.creationTime().toMillis());
        Long value = Long.valueOf(filetime);
        curMap.put(key, value);
        Long oldValue = oldMap.get(key);

        if (oldValue == null) {
            if (filetime > retainTime) {
                fileListeners.firechange(new FileEvent(channel, rootPath.toString(), rootPath.relativize(path).toString(), FileEvent.Type.CREATE));
            }
        }
        else if (!oldValue.equals(value)) {
            if (filetime > retainTime) {
                fileListeners.firechange(new FileEvent(channel, rootPath.toString(), rootPath.relativize(path).toString(), FileEvent.Type.MODIFY));
            }
            oldMap.remove(key);
        }
        else {
            oldMap.remove(key);
        }
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        visit(dir, attrs);
        return interrupted ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        visit(file, attrs);
        return interrupted ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return interrupted ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return interrupted ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
    }

    public void interrupt() {
        interrupted = true;
    }

    private void log(String message) {
        LOGGER.info(channel.getId() + " - " + message);
    }
}
