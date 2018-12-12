package org.getback4j.getback.json;

import org.json.simple.JSONObject;

/**
 *
 * @author Bryce
 */
public final class JSONObjects {

    /**
     * Gets a generic OK status response without data content
     *
     * @return JSON Generic JSONObject containing status: ok
     */
    public static String getStatusOk() {
        JSONObject obj = new JSONObject();
        obj.put("status", "ok");
        return obj.toJSONString();
    }

    /**
     * Gets a OK status response with data content provided
     *
     * @param data The JSON string containing the data that will be inside the
     * JSON data key.
     *
     * @return JSON Generic JSONObject containing status: ok and data: yourdata
     */
    public static String getStatusOk(String data) {
        JSONObject obj = new JSONObject();
        // instead of parsing into another JSONObject (which data might not be), lets 
        // cast the good o'l way to a regular object which can be returned into the data
        // field within obj.
        Object obj2 = (Object) data;

        obj.put("status", "ok");
        obj.put("data", obj2);

        return obj.toJSONString();
    }

    /**
     * Gets a OK status response with data content provided
     *
     * @param data The JSONObject to be stored into the data key
     *
     * @return JSON Generic JSONObject containing status: ok and data: yourdata
     */
    public static String getStatusOk(JSONObject data) {
        JSONObject obj = new JSONObject();

        obj.put("status", "ok");
        obj.put("data", data);

        return obj.toJSONString();
    }

    /**
     * Gets a generic Failure status response without data content
     *
     * @return JSON Generic JSONObject containing status: failure
     */
    public static String getStatusFailure() {
        JSONObject obj = new JSONObject();
        obj.put("status", "failure");

        return obj.toJSONString();
    }

    /**
     * Gets a Failure status response with a reason
     *
     * @param reason The reason why the status is a failure
     *
     * @return JSON Generic JSONObject containing status: failure and
     * reason:reason
     */
    public static String getStatusFailure(String reason) {
        JSONObject obj = new JSONObject();
        obj.put("status", "failure");
        obj.put("message", reason);

        return obj.toJSONString();
    }

    /**
     * Gets a Failure status response with reason: error, message: yourmessage
     *
     * @param message The message why the reason is 'error'
     *
     * @return JSON Generic JSONObject containing status: failure and
     * reason:error, message: yourmessage
     */
    public static String getStatusError(String message) {
        JSONObject obj = new JSONObject();
        obj.put("status", "failure");
        obj.put("reason", "error");
        obj.put("message", message);
        return obj.toJSONString();
    }

}
