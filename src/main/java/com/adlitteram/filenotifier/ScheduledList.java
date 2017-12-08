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

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

abstract public class ScheduledList<T> implements Runnable {

    private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ScheduledList.class);

    private long time;
    private int maxsize;
    private ArrayList<T> delegate;
    private ScheduledExecutorService executor;

    public ScheduledList(long time) {
        this(time, Integer.MAX_VALUE);
    }

    public ScheduledList(long time, int maxsize) {
        this.time = time;
        this.maxsize = maxsize;
        this.delegate = new ArrayList<>();
    }

    abstract public void execute(ArrayList<T> list);

    public void start() {
        if (time > 0 && executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(this, time, time, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        try {
            flush();
        }
        catch (Throwable t) {
            // Avoid to hang the thread!
            LOGGER.warn("run() : ", t);
        }
    }

    public synchronized void flush() {
        if (delegate.size() > 0) {
            ArrayList<T> list = delegate;
            delegate = new ArrayList<>();
            execute(list);
        }
    }

    public void stop() {
        if (executor != null) {
            try {
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);
                executor.shutdownNow();
                executor = null;
            }
            catch (InterruptedException ex) {
            }
        }
        flush(); // flush the remaining element in the list
    }

    private void checkSize() {
        if (delegate.size() >= maxsize) {
            flush();
        }
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public T get(int index) {
        return delegate.get(index);
    }

    public T set(int index, T element) {
        return delegate.set(index, element);
    }

    public T remove(int index) {
        return delegate.remove(index);
    }

    public void clear() {
        delegate.clear();
    }

    public boolean add(T e) {
        boolean b = delegate.add(e);
        checkSize();
        return b;
    }
}
