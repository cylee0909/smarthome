package com.babt.smarthome.model;

import com.android.volley.Request;
import com.cylee.androidlib.net.InputBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cylee on 16/12/18.
 */

public class Config {
    public boolean verify;
    public static class Input extends InputBase {
        private String id;
        private Input(String id){
            this.id = id;
            this.url = "/smarthome/config";
            this.method = Request.Method.POST;
            this.aClass = Config.class;
        }
        @Override
        public Map<String, Object> getParams() {
            Map<String, Object> params = new HashMap<>();
            params.put("id", id);
            return params;
        }
    }


    public static InputBase buidInput(String id) {
        return new Input(id);
    }
}
