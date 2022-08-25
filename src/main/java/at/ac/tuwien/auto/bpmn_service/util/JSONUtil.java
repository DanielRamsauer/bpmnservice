package at.ac.tuwien.auto.bpmn_service.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author Daniel Ramsauer
 * @since 12.04.2022, Di.
 */
public abstract class JSONUtil {
    public static JSONObject getJsonObjectOfVariableMap(Map<String, Object> variables) {
        JSONObject jsonObject = new JSONObject();
        for (String s : variables.keySet()) {
            jsonObject.put(s, variables.get(s).toString());
        }
        return jsonObject;
    }

    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}
