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
package com.adlitteram.filenotifier.xml;

import com.adlitteram.filenotifier.Channel;
import com.adlitteram.filenotifier.sources.LocalSpoolJob;
import com.adlitteram.filenotifier.targets.FileEventProcessor;
import com.adlitteram.filenotifier.targets.FileEventWriter;
import com.adlitteram.filenotifier.targets.TxtEventWriter;
import java.util.ArrayList;
import org.quartz.JobBuilder;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import static org.quartz.TriggerBuilder.newTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ChannelsHandler extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelsHandler.class);

    private final ArrayList<Channel> channelList;
    private final Scheduler scheduler;
    private Channel channel;
    private JobDetail job;
    private ArrayList<FileEventProcessor> processorList;

    public ChannelsHandler(ArrayList<Channel> channelList, Scheduler scheduler) {
        this.channelList = channelList;
        this.scheduler = scheduler;
    }

    @Override
    public void startElement(String uri, String local, String raw, Attributes attrs) {
        if ("channel".equalsIgnoreCase(raw)) {
            channel = new Channel();
            channel.setScheduler(scheduler);
            channel.setId(attrs.getValue("id"));
            channel.setDescription(attrs.getValue("description"));
            channelList.add(channel);
        }
        else if ("trigger".equalsIgnoreCase(raw)) {
            Trigger trigger = newTrigger().startNow().build();
            channel.setTrigger(trigger);
        }
        else if ("localSpool".equalsIgnoreCase(raw)) {
            JobBuilder jobBuilder = newJob(LocalSpoolJob.class);
            jobBuilder.withDescription("LocalSpoolJob");

            job = jobBuilder.build();
            JobDataMap jobMap = job.getJobDataMap();
            jobMap.put("_db", attrs.getValue("db"));
            jobMap.put("_root", attrs.getValue("root"));
            jobMap.put("_subdir", attrs.getValue("subdir"));
            jobMap.put("_ext", attrs.getValue("ext"));
            jobMap.put("_delay", attrs.getValue("delay"));
            jobMap.put("_maxevent", attrs.getValue("maxevent"));
            jobMap.put("_excludes", attrs.getValue("excludes"));
            jobMap.put("_includes", attrs.getValue("includes"));
            jobMap.put("_retain", attrs.getValue("retain"));
            channel.setJobDetail(job);
            processorList = new ArrayList<>();
        }
        else if ("fileEventWriter".equalsIgnoreCase(raw)) {
            processorList.add(new FileEventWriter(attrs.getValue("path")));
        }
        else if ("logEventWriter".equalsIgnoreCase(raw)) {
            processorList.add(new TxtEventWriter(attrs.getValue("path")));
        }
    }

    @Override
    public void endElement(String uri, String local, String raw) {
        if ("channel".equalsIgnoreCase(raw)) {
            LOGGER.info("Channel: " + channel.getId() + " - " + channel.getDescription() + " created");
        }
        else if ("localSpool".equalsIgnoreCase(raw)) {
            job.getJobDataMap().put("_fileeventprocessors", processorList.toArray(new FileEventProcessor[processorList.size()]));
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
    }

    @Override
    public void ignorableWhitespace(char ch[], int start, int length) {
    }

    @Override
    public void warning(SAXParseException ex) {
        LOGGER.warn(getLocationString(ex), ex);
    }

    @Override
    public void error(SAXParseException ex) {
        LOGGER.warn(getLocationString(ex), ex);
    }

    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        LOGGER.warn(getLocationString(ex), ex);
    }

    // Returns a string of the location.
    private String getLocationString(SAXParseException ex) {
        StringBuilder str = new StringBuilder();
        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1) {
                systemId = systemId.substring(index + 1);
            }
            str.append(systemId);
        }
        str.append(':').append(ex.getLineNumber());
        str.append(':').append(ex.getColumnNumber());
        return str.toString();
    }
}
