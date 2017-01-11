package com.babt.smarthome;

import com.cylee.androidlib.util.PreferenceUtils;

/**
 * Created by cylee on 16/9/25.
 */

public enum HomePreference implements PreferenceUtils.DefaultValueInterface {
    ROOMS(null),
    TIMES(null),
    NET_INITED(false),
    NET_LOGIN_NAME(""),
    NET_LOGIN_PASSWD(""),
    NET_LOGIN_ADDRESS(""),
    PM25(null),
    POWER_SEEK(512),
    AUTO_RUN_SET_PM25(50),
    AUTO_RUN(false),
    AUTO_RUN_TIME(0L),
    VERIFIED(false),
    VERIFY_SUCCESS(false),
    VERIFY_TIME(0L),
    NEED_VERIFY(false),
    VERIFY_KEY(""),
    HEAT(false),
    CHANGE_FILTER_TIP_TIME(System.currentTimeMillis()),
    LEAVE_HOME(null);
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
