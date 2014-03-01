/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.s1.objects;

import org.s1.misc.Closure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Iterator over Map-List-Object structures
 */
public class ObjectIterator {

    /**
     * Iterate through Object tree and call callback for each leaf,
     * callback call order is - leafs first then root
     *
     * @param o
     * @param closure Callback
     * @return Processed with Callback object
     */
    static <T> T iterate(Object o, Closure<IterateBean, Object> closure) {
        return (T)iterateNamedObjectFromLeaf(null, "", o, closure);
    }

    /**
     * @param name
     * @param path
     * @param o
     * @param closure
     * @return
     */
    private static Object iterateNamedObjectFromLeaf(String name, String path, Object o, Closure<IterateBean, Object> closure) {
        if (o instanceof Map) {
            final Map<String, Object> m1 = new HashMap<String, Object>();
            Map<String, Object> m = (Map<String, Object>) o;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                m1.put(e.getKey(),
                        iterateNamedObjectFromLeaf(e.getKey(), (!Objects.isNullOrEmpty(path) ? path + "." + e.getKey() : e.getKey()), e.getValue(), closure));
            }
            return closure.callQuite(new IterateBean(name, m1, path));
        } else if (o instanceof List) {
            List l = new ArrayList();
            for (int i = 0; i < ((List) o).size(); i++) {
                l.add(iterateNamedObjectFromLeaf(null, path + "[" + i + "]", ((List) o).get(i), closure));
            }
            return closure.callQuite(new IterateBean(name, l, path));
        } else {
            return closure.callQuite(new IterateBean(name, o, path));
        }
    }

    /**
     * Bean that comes as input parameter to closure in {@link ObjectIterator#iterate(Object, org.s1.misc.Closure)}
     */
    public static class IterateBean {
        private String name;
        private Object value;
        private String path;

        public IterateBean(String name, Object value, String path) {
            this.name = name;
            this.value = value;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public String getPath() {
            return path;
        }
    }

}
