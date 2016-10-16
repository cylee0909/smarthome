package com.babt.smarthome

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by cylee on 16/10/9.
 */
object HomeUtil {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    val timeFormat = SimpleDateFormat("HH:mm")
    val WEEKS = arrayOf("一", "二","三", "四", "五", "六", "日")
    fun getWeek(weekInt : Int) : String{
        var newWeekInt = Math.min(Math.max(weekInt, 0), 6)
        return WEEKS[newWeekInt]
    }

    fun getNextDateByWeek(weekInt: Int, time : String) : String {
        var current = Date()
        var cal = Calendar.getInstance()
        cal.time = current
        var currentWeek = cal.get(Calendar.DAY_OF_WEEK) -2
        if (currentWeek == -1) currentWeek = 6 // 周日
        var minDay = Int.MAX_VALUE
        var minWeek = currentWeek
        for (i in 0 .. WEEKS.size) {
            if (weekInt and (1 shl i) > 0) {
                var diff = i - currentWeek
                if (diff < 0) {
                    diff += 7
                }
                if (diff == 0) { // 是同一天,判断时间
                    var timeDate = timeFormat.parse(time)
                    var currentTime = timeFormat.parse(timeFormat.format(current))
                    if (timeDate.before(currentTime)) {
                        diff += 7
                    }
                }
                if (minDay > diff) {
                    minDay = diff
                    minWeek = i
                }
            }
        }
        cal.add(Calendar.DAY_OF_YEAR, minDay)
        return dateFormat.format(cal.time)
    }

    fun getAllWeek(weekInt: Int) : String{
        var result = ""
        if (weekInt > 0) {
            for (i in 0 .. WEEKS.size) {
                if (weekInt and (1 shl i) > 0) {
                    result += getWeek(i)+","
                }
            }
        }
        if (result.endsWith(",")) {
            result = result.substring(0, result.length - 1)
        }
        return result
    }

    fun getChannelFromId(p :Int):Char {
        if (p <= 9) return '0' + (p - 0)
        return 'a' + (p - 10)
    }
}