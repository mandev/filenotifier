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

public class RunEvent {

    public enum Type {
        START, STOP
    }

    private final Channel channel;
    private final Type type;
    private final Object param;

    public RunEvent(Channel channel, Type type, Object param) {
        this.channel = channel;
        this.type = type;
        this.param = param;
    }

    public Type getType() {
        return type;
    }

    public Object getParam() {
        return param;
    }

    public Channel getChannel() {
        return channel;
    }

}
