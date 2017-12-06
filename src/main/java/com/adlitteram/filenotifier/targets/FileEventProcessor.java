package com.adlitteram.filenotifier.targets;

import com.adlitteram.filenotifier.files.FileEvent;
import java.util.ArrayList;

public interface FileEventProcessor extends Processor<ArrayList<FileEvent>> {

    @Override
    public boolean process(ArrayList<FileEvent> eventList);

}
