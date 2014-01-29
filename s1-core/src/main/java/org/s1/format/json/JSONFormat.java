package org.s1.format.json;

import org.s1.format.json.org_json_simple.JSONObject;
import org.s1.format.json.org_json_simple.parser.ParseException;
import org.s1.format.json.org_json_simple.parser.JSONParser;

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
