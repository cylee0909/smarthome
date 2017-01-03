package com.babt.smarthome

/**
 * Created by cylee on 16/12/31.
 */
class AppConfig {
    companion object {
        var envLocal = Env("http://192.168.31.103:8990/", "192.168.31.103", 8989)
        var envServer = Env("http://115.47.58.102:8990/", "115.47.58.102", 8989)
        var config = if (BuildConfig.DEBUG) envLocal else envServer
    }

    class Env {
        var serverHttpUrl = ""
        var socketHost = ""
        var socketPort = 80

        constructor(serverHttpUrl: String, socketHost: String, socketPort: Int) {
            this.serverHttpUrl = serverHttpUrl
            this.socketHost = socketHost
            this.socketPort = socketPort
        }
    }
}