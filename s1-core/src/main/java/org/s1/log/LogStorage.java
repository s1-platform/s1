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

package org.s1.log;

import java.util.List;
import java.util.Map;

/**
 * Log storage that can be listed
 */
public class LogStorage {

    /**
     * Get log messages
     *
     * @param list
     * @param search
     * @param skip
     * @param max
     * @return
     */
    public long list(List<Map<String,Object>> list, Map<String,Object> search, int skip, int max){
        return 0;
    }

}
