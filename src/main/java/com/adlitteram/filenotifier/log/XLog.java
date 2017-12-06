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
package com.adlitteram.filenotifier.log;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class XLog {

    public static void init(String logDir, String logName) {
        Level DEFAULT_LEVEL = Level.INFO;

        // Get root logger
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(DEFAULT_LEVEL);

        // Remove all default handlers
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }

        // Create standard formatter
        StreamFormatter streamFormatter = new StreamFormatter(false, true);

        // Log to Console
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(DEFAULT_LEVEL);
        consoleHandler.setFormatter(streamFormatter);
        rootLogger.addHandler(consoleHandler);

        // Log to File
        if (logDir != null) {
            try {
                File dir = new File(logDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String pattern = logDir + "/" + logName + "_%g.log";
                FileHandler fileHandler = new FileHandler(pattern, 1024 * 1024, 20);
                fileHandler.setLevel(Level.ALL);
                fileHandler.setFormatter(streamFormatter);
                rootLogger.addHandler(fileHandler);
            }
            catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    public static Logger getRootLogger() {
        return Logger.getLogger("");
    }

    public static void close() {
        LogManager lm = LogManager.getLogManager();
        for (Enumeration<String> names = lm.getLoggerNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            Logger logger = lm.getLogger(name);
            Handler[] handlers = logger.getHandlers();
            for (Handler handler : handlers) {
                handler.close();
            }
        }
    }
}
