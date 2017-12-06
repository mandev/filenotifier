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
import com.adlitteram.filenotifier.ScheduledList;
import com.adlitteram.filenotifier.targets.FileEventProcessor;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatcherListener extends ScheduledList<FileEvent> implements ChangeListener<Object> {

    private final Logger LOGGER = LoggerFactory.getLogger(WatcherListener.class);

    private final FileEventProcessor fileEventProcessor;

    public WatcherListener(int delay, int maxsize, FileEventProcessor fileEventProcessor) {
        super(delay, maxsize);
        this.fileEventProcessor = fileEventProcessor;
    }

    @Override
    public void execute(ArrayList<FileEvent> list) {
        fileEventProcessor.process(list);
    }

    public void changed(RunEvent event) {
        if (event.getType() == RunEvent.Type.START) {
            LOGGER.info(event.getChannel().getId() + " - Starting WatcherListener [" + fileEventProcessor.getClass().getSimpleName() + "]");
            start();
        }
        else if (event.getType() == RunEvent.Type.STOP) {
            LOGGER.info(event.getChannel().getId() + " - Stopping WatcherListener [" + fileEventProcessor.getClass().getSimpleName() + "]");
            stop();
        }
    }

    public void changed(FileEvent event) {
        if (event == null) {
            return;
        }
        if (size() > 0 && event.equals(get(size() - 1))) {
            return;
        }
        add(event);
    }

    // Unfortunalety in java because of type erasure we cannot implement multiple generified
    // interface like ChangeListener<FileEvent> and ChangeListener<WatcherEvent>  
    @Override
    public void changed(Object event) {
        if (event instanceof FileEvent) {
            changed((FileEvent) event);
        }
        else if (event instanceof RunEvent) {
            changed((RunEvent) event);
        }
    }
}
