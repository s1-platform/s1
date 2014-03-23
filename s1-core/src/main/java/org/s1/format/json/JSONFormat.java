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

package org.s1.format.json;

import org.s1.format.json.org_json_simple.JSONObject;
import org.s1.format.json.org_json_simple.parser.JSONParser;
import org.s1.format.json.org_json_simple.parser.ParseException;

import java.util.Map;

/**
 * JSON Format helper
 *
 * uses <a href="http://code.google.com/p/json-simple/" target="_blank">json-simple</a>
 */
public class JSONFormat {

    /**
     * Convert {@link java.util.Map} to JSON string
     *
     * @param m
     * @return
     */
    public static String toJSON(Map<String, Object> m) {
        return new JSONObject(m).toJSONString();
    }

    /**
     * Convert JSON string to {@link java.util.Map}
     *
     * @param json
     * @return

     */
    public static Map<String, Object> evalJSON(String json) throws JSONFormatException {
        JSONParser parser = new JSONParser();
        JSONObject o = null;
        try {
            o = (JSONObject)parser.parse(json);
        } catch (ParseException e) {
            throw new JSONFormatException(e.getMessage(),e);
        }
        return o;
    }

}
