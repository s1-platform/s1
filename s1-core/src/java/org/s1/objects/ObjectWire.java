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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Object Wire helper
 * <p>You need to convert some classes (Dates) to serializable format</p>
 * <p>Simple types are serialized in string format for instance <code>/Date(1389439481222)</code>/</p>
 * <p>Supported types:</p>
 * <ul>
 *     <li>/Date(...)/ {@link java.util.Date}</li>
 * </ul>
 */
public class ObjectWire {

    /**
     * Format Java Classes into strings version
     *
     * @param json
     * @return
     */
    static Map<String, Object> toWire(Map<String, Object> json) {
        return (Map<String, Object>) ObjectIterator.iterate(json, new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean i) {
                try {
                    if (i.getValue() instanceof Date) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
                        //return "/Date(" + ((Date) i.getValue()).getTime() + ")/";
                        return "/Date(" + sdf.format((Date) i.getValue()) + ")/";
                    }
                } catch (Exception e) {
                }
                return i.getValue();
            }
        });
    }

    /**
     * Parse simple types into Java Classes
     *
     * @param json
     * @return
     */
    static Map<String, Object> fromWire(Map<String, Object> json) {
        return (Map<String, Object>) ObjectIterator.iterate(json, new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean i) {
                if (i.getValue() instanceof String) {
                    String s = (String) i.getValue();
                    try{
                        if (s.startsWith("/") && s.endsWith("/")) {
                            s = s.substring(1, s.length() - 1);
                            String type = s.substring(0, s.indexOf("("));
                            String value = s.substring(s.indexOf("(") + 1,
                                    s.lastIndexOf(")"));
                            if ("Date".equalsIgnoreCase(type)) {
                                return ObjectType.cast(value, Date.class);
                            }
                        }
                    }catch (Throwable e){}
                }
                return i.getValue();
            }
        });
    }

}
