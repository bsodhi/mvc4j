/*
Copyright 2015 Balwinder Sodhi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.javamvc.core;

import java.util.Set;

/**
 *
 * @author Balwinder Sodhi
 */
public interface SharedDataProvider<K, V> {
    /**
     * Adds or replaces a key value pair in the shared data store.
     * @param key
     * @param value 
     */
    void put(K key, V value);
    
    /**
     * Returns the value for given key from shared data store. Given key is
     * not present then null value is returned. We do not distinguish between
     * a null value and a missing key, i.e., there can be a key will null value.
     * @param key
     * @return 
     */
    V get(K key);
    
    /**
     * Return the number of items in the shared data store.
     * @return 
     */
    int size();
    
    /**
     * Checks whether the given key is present in shared data store.
     * @param key
     * @return Return true if key is found, else false.
     */
    boolean containsKey(K key);
    
    /**
     * Removed the entry for specified key from shared data store.
     * @param key
     * @return The value associated with key just removed. Returns null if
     * key was not found.
     */
    V remove(K key);
    
    /**
     * Returns the set of keys present in the shared data store.
     * @return 
     */
    Set<K> keySet();
}
