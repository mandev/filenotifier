package com.adlitteram.filenotifier.targets;

public interface Processor<T> {

    public boolean process(T event);

}
