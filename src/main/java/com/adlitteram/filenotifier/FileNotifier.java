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
package com.adlitteram.filenotifier;

import com.adlitteram.filenotifier.files.DbManager;
import com.adlitteram.filenotifier.log.XLog;
import com.adlitteram.filenotifier.xml.ChannelsReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNotifier {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileNotifier.class);

    public static final String HOME_DIR = System.getProperty("user.home") + File.separator;
    public static final String PROG_DIR = System.getProperty("user.dir") + File.separator;
    public static final String USER_CONF_DIR = HOME_DIR + ".filenotifier" + File.separator;
    public static final String USER_LOG_DIR = USER_CONF_DIR + "log" + File.separator;

    public FileNotifier() {
    }

    public void runChannels() throws SchedulerException {

        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler sched = sf.getScheduler();
        sched.start();

        ArrayList<Channel> channelList = new ArrayList<>();
        File file = new File(USER_CONF_DIR + "channels.xml");
        if (file.exists()) {
            LOGGER.info("Loading channels : " + file.getPath());
            ChannelsReader.read(channelList, sched, file.toURI());
        }

        for (Channel channel : channelList) {
            channel.start();
        }

        System.out.println();
        Scanner scanner = new Scanner(System.in);
        String str;
        do {
            System.out.print("FileNotifier - Enter 'exit' to quit : ");
            str = scanner.nextLine();
        }
        while (!str.equals("exit"));

        for (Channel channel : channelList) {
            LOGGER.info("Stopping channel : " + channel.getId());
            channel.stop();
        }

        LOGGER.info("Shutdown scheduler");
        sched.shutdown();

        DbManager.closeAll();
    }

    public static void main(String[] args) throws IOException, Exception {

        FileUtils.forceMkdir(new File(USER_LOG_DIR));
        XLog.init(USER_LOG_DIR, FileNotifier.class.getSimpleName());

        LOGGER.info("OS_NAME = " + SystemUtils.OS_NAME);
        LOGGER.info("OS_VERSION = " + SystemUtils.OS_VERSION);
        LOGGER.info("JAVA_VERSION = " + SystemUtils.JAVA_VERSION);
        LOGGER.info("JAVA_VENDOR = " + SystemUtils.JAVA_VENDOR);
        LOGGER.info("APPLICATION_NAME = " + Version.getNAME());
        LOGGER.info("APPLICATION_RELEASE = " + Version.getRELEASE());
        LOGGER.info("APPLICATION_BUILD = " + Version.getBUILD());

        try {
            FileNotifier notifier = new FileNotifier();
            notifier.runChannels();

            LOGGER.info("Exiting " + Version.getNAME());
            System.exit(0);
        }
        catch (SchedulerException e) {
            LOGGER.warn("", e);
        }
    }
}
