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
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class ChannelsReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelsReader.class);

    public static boolean read(ArrayList<Channel> channelList, Scheduler scheduler, URI uri) {
        return (uri == null) ? false : read(channelList, scheduler, new InputSource(uri.toString()));
    }

    public static boolean read(ArrayList<Channel> channelList, Scheduler scheduler, Reader reader) {
        return (reader == null) ? false : read(channelList, scheduler, new InputSource(reader));
    }

    public static boolean read(ArrayList<Channel> channelList, Scheduler scheduler, InputSource inputSource) {

        try {
            XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            ChannelsHandler xh = new ChannelsHandler(channelList, scheduler);
            parser.setContentHandler(xh);
            parser.setErrorHandler(xh);
            parser.setFeature("http://xml.org/sax/features/validation", false);
            parser.setFeature("http://xml.org/sax/features/namespaces", false);
            parser.setFeature("http://apache.org/xml/features/validation/schema", false);
            //parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", false);
            parser.parse(inputSource);
        }
        catch (org.xml.sax.SAXParseException spe) {
            LOGGER.warn("", spe);
            return false;
        }
        catch (org.xml.sax.SAXException se) {
            LOGGER.warn("", se);
            return false;
        }
        catch (ParserConfigurationException | IOException e) {
            LOGGER.warn("", e);
            return false;
        }
        return true;
    }
}
