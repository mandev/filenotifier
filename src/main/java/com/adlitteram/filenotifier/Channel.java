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

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.LoggerFactory;

public class Channel {

    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Channel.class);

    public enum State {
        DISABLE, STOP, START, ACTIVE, RUN, FAIL
    };

    private String id;
    private String description;
    private Scheduler scheduler;
    private Trigger trigger;
    private JobDetail jobDetail;
    private State state = State.STOP;

    public Channel() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public JobDetail getJobDetail() {
        return jobDetail;
    }

    public void setJobDetail(JobDetail jobDetail) {
        this.jobDetail = jobDetail;
        this.jobDetail.getJobDataMap().put("_channel", this);
    }

    public State getState() {
        return state;
    }

    public void disable() {
        if (state != State.DISABLE) {
            stop();
            state = State.DISABLE;
        }
    }

    public void enable() {
        if (state == State.DISABLE) {
            state = State.STOP;
        }
    }

    public void active() {
        state = State.ACTIVE;
    }

    public void fail() {
        state = State.FAIL;
    }

    public void stop() {
        LOGGER.info("Stopping channel: " + id);
        if (jobDetail != null) {
            try {
                state = State.STOP;
                scheduler.interrupt(jobDetail.getKey());
                scheduler.deleteJob(jobDetail.getKey());
                LOGGER.info("Channel " + id + " stopped");
            }
            catch (SchedulerException ex) {
                LOGGER.warn("", ex);
            }
        }
    }

    public void start() {
        LOGGER.info("Starting channel: " + id);
        if (state == State.STOP || state == State.FAIL) {
            try {
                if (jobDetail != null && trigger != null
                    && !scheduler.checkExists(jobDetail.getKey())) {
                    scheduler.scheduleJob(jobDetail, trigger);
                }
            }
            catch (SchedulerException ex) {
                LOGGER.warn("", ex);
            }
        }
    }
}
