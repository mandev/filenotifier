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
package com.adlitteram.filenotifier.sources;

import com.adlitteram.filenotifier.Channel;
import com.adlitteram.filenotifier.files.FileSpooler;
import com.adlitteram.filenotifier.files.FileWatcher;
import com.adlitteram.filenotifier.files.SpoolerListener;
import com.adlitteram.filenotifier.files.WatcherListener;
import com.adlitteram.filenotifier.targets.FileEventProcessor;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class LocalSpoolJob implements InterruptableJob {

    private final static Logger LOGGER = LoggerFactory.getLogger(LocalSpoolJob.class);

    private Channel channel;
    private FileWatcher watcher;
    private FileSpooler spooler;
    private boolean interrupted;

    /**
     * <p>
     * Empty constructor for job initilization
     * </p>
     * <p>
     * Quartz requires a public empty constructor so that the scheduler can
     * instantiate the class whenever it needs.
     * </p>
     */
    public LocalSpoolJob() {
    }

    /**
     * <p>
     * Called by the <code>{@link org.quartz.Scheduler}</code> when a
     * <code>{@link org.quartz.Trigger}</code> fires that is associated with the
     * <code>Job</code>.
     * </p>
     *
     * @param context
     * @throws JobExecutionException if there is an exception while executing
     * the job.
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        JobDataMap dataMap = jobDetail.getJobDataMap();
        channel = (Channel) dataMap.get("_channel");
        log("ENTRY");

        String root = dataMap.getString("_root");
        String includes = dataMap.getString("_includes");
        String excludes = dataMap.getString("_excludes");
        boolean subdir = dataMap.getBoolean("_subdir");
        boolean ext = dataMap.getBoolean("_ext");
        int delay = dataMap.getInt("_delay");
        int maxevent = dataMap.getInt("_maxevent");
        double retain = dataMap.getDouble("_retain");
        String db = dataMap.getString("_db");
        FileEventProcessor[] feps = (FileEventProcessor[]) dataMap.get("_fileeventprocessors");

        log("_db: " + db);
        log("_root: " + root);
        log("_subdir: " + subdir);
        log("_delay: " + delay);
        log("_ext: " + ext);
        log("_maxevent: " + maxevent);
        log("_includes: " + includes);
        log("_excludes: " + excludes);
        log("_retain: " + retain);

        Pattern includePattern = createRegexpPattern(includes);
        Pattern excludePattern = createRegexpPattern(excludes);

        if (feps != null) {
            try {
                channel.active();
                spooler = runSpooler(feps, db, root, subdir, includePattern, excludePattern, maxevent, retain);
                watcher = runWatcher(feps, db, root, subdir, includePattern, excludePattern, maxevent, delay, ext);
            }
            catch (IOException ex) {
                LOGGER.warn("", ex);
                channel.fail();
            }
        }
        else {
            channel.fail();
            throw new JobExecutionException("No target builders");
        }
        log("RETURN");
    }

    private Pattern createRegexpPattern(String str) {
        Pattern pattern = null;
        try {
            if (str != null && str.length() > 0) {
                pattern = Pattern.compile(str);
            }
        }
        catch (PatternSyntaxException pse) {
            LOGGER.warn("Regexp Error: " + str + " - ", pse);
        }
        return pattern;
    }

    private FileSpooler runSpooler(FileEventProcessor[] feps, String db, String root, boolean subdir,
                                   Pattern includePattern, Pattern excludePattern, int maxevent, double retain) throws IOException {

        log("Running spooler");
        Path dbPath = Paths.get(db);

        long currentTime = System.currentTimeMillis();
        long retainTime = currentTime - (long) (retain * 24d * 3600d * 1000d);
        log("CurrentTime: " + new Date(currentTime));
        log("RetainTime: " + new Date(retainTime));

        spooler = new FileSpooler(channel, dbPath, retainTime);
        spooler.addPath(Paths.get(root));
        spooler.setIncludePattern(includePattern);
        spooler.setExcludePattern(excludePattern);

        for (FileEventProcessor fep : feps) {
            log("Adding spoolerListener [" + fep.getClass().getSimpleName() + "]");
            SpoolerListener spoolerListener = new SpoolerListener(maxevent, fep);
            spooler.addSpoolerListener(spoolerListener); // call flush
            spooler.addFileListener(spoolerListener);
        }
        spooler.run();
        return spooler;
    }

    private FileWatcher runWatcher(FileEventProcessor[] feps, String db, String root, boolean subdir,
                                   Pattern includePattern, Pattern excludePattern, int maxevent, int delay, boolean ext) throws IOException {

        log("Running watcher");
        Path dbPath = Paths.get(db);

        watcher = new FileWatcher(channel, dbPath, ext);
        watcher.addPath(Paths.get(root));
        watcher.setIncludePattern(includePattern);
        watcher.setExcludePattern(excludePattern);

        for (FileEventProcessor fep : feps) {
            log("Adding watcherListener[" + fep.getClass().getSimpleName() + "]");
            WatcherListener watcherListener = new WatcherListener(delay, maxevent, fep);
            watcher.addWatcherListener(watcherListener); // call flush
            watcher.addFileListener(watcherListener);
        }
        watcher.run();
        return watcher;
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        log("Interrupting LocalSpoolJob");
        interrupted = true;
        if (spooler != null) {
            spooler.interrupt();
        }
        if (watcher != null) {
            watcher.interrupt();
        }
    }

    private void log(String message) {
        LOGGER.info(channel.getId() + " - " + message);
    }

}
