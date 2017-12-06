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

import com.adlitteram.filenotifier.Channel;
import java.util.Objects;

public class FileEvent {

    public enum Type {
        CREATE, DELETE, MODIFY, RENAME, UNKNOWN
    };

    private Channel channel;
    private String root;
    private String path;
    private String rpath;
    private Type type;

    public FileEvent(Channel channel, String root, String path, Type type) {
        this(channel, root, path, null, type);
    }

    public FileEvent(Channel channel, String root, String path, String rpath, Type type) {
        this.channel = channel;
        this.root = root;
        this.path = path;
        this.rpath = rpath;
        this.type = type;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getRoot() {
        return root;
    }

    public String getPath() {
        return path;
    }

    public String getRpath() {
        return rpath;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.root);
        hash = 83 * hash + Objects.hashCode(this.path);
        hash = 83 * hash + Objects.hashCode(this.rpath);
        hash = 83 * hash + Objects.hashCode(this.type);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileEvent other = (FileEvent) obj;
        if (!Objects.equals(this.root, other.root)) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        if (!Objects.equals(this.rpath, other.rpath)) {
            return false;
        }
        return this.type == other.type;
    }
}
