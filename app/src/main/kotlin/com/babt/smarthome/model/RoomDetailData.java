package com.babt.smarthome.model;

import com.cylee.androidlib.GsonBuilderFactory;

/**
 * Created by cylee on 16/12/31.
 */

public class RoomDetailData {

    public int ipm;
    public int opm;
    public int hdy;
    public int tmp;
    /**
     * 当前开关状态 0是关闭 3 低 6 中 9 高
     */
    public int mode;
    public static RoomDetailData fromJson(String json) {
        return GsonBuilderFactory.createBuilder().fromJson(json, RoomDetailData.class);
    }
}
