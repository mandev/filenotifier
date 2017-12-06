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

import java.lang.reflect.Array;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChangeListenerPool<T> {

    private final CopyOnWriteArrayList<ChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void addListener(ChangeListener<T> cl) {
        if (cl != null && !listeners.contains(cl)) {
            listeners.add(cl);
        }
    }

    public ChangeListener<T>[] getListeners(Class<T> clazz) {
        ChangeListener<T>[] a = (ChangeListener<T>[]) Array.newInstance(clazz.getClass(), listeners.size());
        return listeners.toArray(a);
    }

    public ChangeListener<T>[] getListeners() {
        return listeners.toArray(new ChangeListener[listeners.size()]);
    }

    public boolean removeListener(ChangeListener<T> cl) {
        return listeners.remove(cl);
    }

    public void firechange(T event) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).changed(event);
        }
    }
}
