package com.babt.smarthome.model;

import com.android.volley.Request;
import com.cylee.androidlib.net.InputBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cylee on 16/12/18.
 */

public class Verify {
    public String result;
    public static class Input extends InputBase {
        private String id;
        private String verifyId;
        private Input(String id, String verifyId){
            this.id = id;
            this.verifyId = verifyId;
            this.url = "/smarthome/verify";
            this.method = Request.Method.POST;
            this.aClass = Verify.class;
        }
        @Override
        public Map<String, Object> getParams() {
            Map<String, Object> params = new HashMap<>();
            params.put("id", id);
            params.put("verifyId", verifyId);
            return params;
        }
    }


    public static InputBase buidInput(String id, String verifyId) {
        return new Input(id, verifyId);
    }
}
