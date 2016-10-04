package com.babt.smarthome.entity

import java.io.Serializable

/**
 * Created by cylee on 16/9/26.
 */
class Rooms : Serializable {
    var mRooms : MutableList<RoomItem>? = null

    class RoomItem : Serializable {
        var name : String? = ""
        var id : Int = -1
        var iconRes : Int = 0
    }
}