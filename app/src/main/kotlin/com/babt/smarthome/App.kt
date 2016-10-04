package com.babt.smarthome

import com.cylee.androidlib.base.BaseApplication
import com.cylee.androidlib.util.Log

/**
 * Created by cylee on 16/9/20.
 */
class App : BaseApplication() {
    override fun onCreate() {
        super.onCreate()

        Log.setLogLevel(if (BuildConfig.DEBUG) Log.VERBOSE else Log.OFF)
        bindSocket()
    }

    fun bindSocket() {
        SocketManager.init()
    }
}