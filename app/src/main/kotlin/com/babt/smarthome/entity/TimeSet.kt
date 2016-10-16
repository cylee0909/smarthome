package com.babt.smarthome.entity

import java.io.Serializable
import java.util.*

/**
 * Created by cylee on 16/10/15.
 */
class TimeSet : Serializable {
    var timeMap : HashMap<Int, MutableList<TimeItem>> = HashMap()
    class TimeItem {
        var week = 0
        var repeat = -1
        var action = -1
        var nextDate = ""
        var time = ""
        var createTime = 0L
    }
}