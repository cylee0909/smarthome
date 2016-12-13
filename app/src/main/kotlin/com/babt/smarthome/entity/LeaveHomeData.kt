package com.babt.smarthome.entity

import java.io.Serializable

/**
 * Created by cylee on 16/11/9.
 */
class LeaveHomeData : Serializable {
    var startTime = 0L
    var modeStartTime = 0L
    var modeEndTime = 0L
    var ionStartTime = 0L
    var ionEndTime = 0L
}