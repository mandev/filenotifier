package com.adlitteram.filenotifier.targets;

import com.adlitteram.filenotifier.files.FileEvent;
import com.adlitteram.filenotifier.log.StreamFormatter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxtEventWriter implements FileEventProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(TxtEventWriter.class);

    private static final String[] TYPES = {"create", "delete", "modify", "rename", "uknown"};
    private final String path;
    private FileHandler fileHandler;

    public TxtEventWriter(String path) {
        this(path, true);
    }

    public TxtEventWriter(String path, boolean rotate) {

        this.path = path;

        try {
            StreamFormatter streamFormatter = new StreamFormatter(true);
            streamFormatter.setWithMethods(false);
            streamFormatter.setWithLevel(false);
            streamFormatter.setSeparator(";");
            if (rotate) {
                fileHandler = new FileHandler(path + "_%g.txt", 1024 * 1024, 20);
            }
            else {
                fileHandler = new FileHandler(path + "_%g.txt");
            }
            fileHandler.setFormatter(streamFormatter);
        }
        catch (IOException | SecurityException ex) {
            LOGGER.warn("Unable to initialize LogEventWriter. ", ex);
        }
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean process(ArrayList<FileEvent> eventList) {

        if (fileHandler != null) {
            for (FileEvent event : eventList) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(";").append(event.getChannel().getId());
                buffer.append(";").append(TYPES[event.getType().ordinal()]);
                buffer.append(";").append(event.getRoot());
                buffer.append(";").append(event.getPath());
                if (event.getRpath() != null) {
                    buffer.append(";").append(event.getRpath());
                }
                fileHandler.publish(new LogRecord(Level.INFO, buffer.toString()));
            }
        }

        return true;
    }
}
