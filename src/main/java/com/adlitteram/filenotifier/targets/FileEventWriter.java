package com.adlitteram.filenotifier.targets;

import com.adlitteram.filenotifier.files.FileEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.znerd.xmlenc.XMLOutputter;

public class FileEventWriter implements FileEventProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(FileEventWriter.class);

    private static final AtomicLong COUNTER = new AtomicLong();
    private static final String[] TYPES = {"c", "d", "m", "r", "u"};
    private final String path;

    public FileEventWriter(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean process(ArrayList<FileEvent> eventList) {
        String s = System.currentTimeMillis() + "_" + COUNTER.incrementAndGet();
        Path p = Paths.get(path.replace("#", s));

        try (BufferedWriter writer = Files.newBufferedWriter(p, StandardCharsets.UTF_8);) {
            XMLOutputter xmlWriter = new XMLOutputter(writer, "UTF-8");
            xmlWriter.declaration();
            xmlWriter.startTag("files");

            for (FileEvent event : eventList) {
                xmlWriter.startTag("file");
                xmlWriter.attribute("root", event.getRoot());
                xmlWriter.attribute("path", event.getPath());
                if (event.getRpath() != null) {
                    xmlWriter.attribute("rpath", event.getRpath());
                }
                xmlWriter.attribute("type", TYPES[event.getType().ordinal()]);
                xmlWriter.endTag();
            }

            xmlWriter.endTag();
            xmlWriter.endDocument();
            writer.close();
            return true;
        }
        catch (IllegalStateException | IllegalArgumentException | IOException ex) {
            LOGGER.warn("", ex);
            return false;
        }
    }
}
