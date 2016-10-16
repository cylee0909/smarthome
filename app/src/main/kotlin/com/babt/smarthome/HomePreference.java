package com.babt.smarthome;

import com.cylee.androidlib.util.PreferenceUtils;

/**
 * Created by cylee on 16/9/25.
 */

public enum HomePreference implements PreferenceUtils.DefaultValueInterface {
    ROOMS(null),
    TIMES(null);
    HomePreference(Object def) {
        mDef = def;
    }

    private Object mDef;
    @Override
    public Object getDefaultValue() {
        return mDef;
    }

    @Override
    public String getNameSpace() {
        return "HomePreference";
    }
}
