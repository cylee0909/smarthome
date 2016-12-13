package com.babt.smarthome.entity

/**
 * Created by cylee on 16/12/10.
 */
class Pm25 {
    var p:String? = ""
    var t:Long = 0L
    constructor(p: String?, t: Long) {
        this.p = p
        this.t = t
    }
}