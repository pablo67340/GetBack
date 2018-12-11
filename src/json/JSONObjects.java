/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package json;

import org.json.simple.JSONObject;

/**
 *
 * @author Bryce
 */
public final class JSONObjects {

    public static String getStatusOk() {
        JSONObject obj = new JSONObject();
        obj.put("status", "ok");
        return obj.toJSONString();
    }
    
    public static String getStatusOk(String data) {
        JSONObject obj = new JSONObject();
        // instead of parsing into another JSONObject (which data might not be), lets 
        // cast the good o'l way to a regular object which can be returned into the data
        // field within obj.
        Object obj2 = (Object)data;
        
        obj.put("status", "ok");
        obj.put("data", obj2);
        
        return obj.toJSONString();
    }
    
    public static String getStatusOk(JSONObject data){
        JSONObject obj = new JSONObject();
        
        obj.put("status", "ok");
        obj.put("data", data);
        
        return obj.toJSONString();
    }

    public static String getStatusFailure() {
        JSONObject obj = new JSONObject();
        obj.put("status", "failure");

        return obj.toJSONString();
    }
    
    public static String getStatusFailure(String reason) {
        JSONObject obj = new JSONObject();
        obj.put("status", "failure");
        obj.put("message", reason);

        return obj.toJSONString();
    }
    
    public static String getStatusError(String message){
        JSONObject obj = new JSONObject();
        obj.put("status", "failure");
        obj.put("reason", "error");
        obj.put("message", message);
        return obj.toJSONString();
    }

}
