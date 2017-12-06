package com.adlitteram.filenotifier.targets;

import com.adlitteram.filenotifier.files.FileEvent;
import com.adlitteram.filenotifier.files.FileStabilityChecker;
import static com.adlitteram.filenotifier.files.FileEvent.Type.CREATE;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSyncer implements FileEventProcessor {

    private final Logger logger = LoggerFactory.getLogger(FileSyncer.class);

    private Path path;
    private FileStabilityChecker fileStabilityChecker;

    public FileSyncer(Path path) {
        this(path, new FileStabilityChecker());
    }

    public FileSyncer(Path path, FileStabilityChecker fileStabilityChecker) {
        this.path = path;
        this.fileStabilityChecker = fileStabilityChecker;
    }

    @Override
    public boolean process(ArrayList<FileEvent> eventList) {
        for (FileEvent event : eventList) {
            String id = event.getChannel().getId();
            Path srcPath = Paths.get(event.getRoot(), event.getPath());
            Path dstPath = path.resolve(event.getPath());

            // In reality we need to check the stabilty of the file
            switch (event.getType()) {
                case CREATE:
                    logger.info(id + " - create " + srcPath + " " + dstPath);
                    break;
                case MODIFY:
                    logger.info(id + " - modify " + srcPath + " " + dstPath);
                    break;
                case DELETE:
                    logger.info(id + " - delete " + dstPath);
                    break;
                case UNKNOWN:
                    logger.info(id + " - unknown " + srcPath);
                    break;
            }
        }
        return true;
    }
}
