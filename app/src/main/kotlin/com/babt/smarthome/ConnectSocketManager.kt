package com.babt.smarthome

import android.content.pm.PackageManager
import com.babt.smarthome.entity.Rooms
import com.babt.smarthome.model.AirCleanData
import com.babt.smarthome.model.RoomDetailData
import com.babt.smarthome.util.EncryptUtil
import com.baidu.android.common.util.DeviceId
import com.cylee.androidlib.GsonBuilderFactory
import com.cylee.androidlib.base.BaseApplication
import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.PreferenceUtils
import com.cylee.androidlib.util.TaskUtils
import com.cylee.socket.AbsBaseTimeSocketListener
import com.cylee.socket.TimeCheckSocket
import com.cylee.socket.tcp.ITcpConnectListener
import com.cylee.socket.tcp.TcpSocket
import com.cylee.socket.tcp.TimeTcpCheckSocket
import com.babt.smarthome.model.ConnectData

/**
 * Created by cylee on 16/12/15.
 * 连接远程服务器的管理类
 */
object ConnectSocketManager {
    const val MAX_RETRY_COUNT = 100
    const val HEART_INTERNAL = 40 * 1000 //40s
    var mConnectCount = 0
    var mSocket: TimeTcpCheckSocket? = null
    var connectWork = ConnectWork()
    var heartWork : HeartBeatWork? = null
    var heartMaxErrorCount = 3
    var heartErrorCount = 0

    class ConnectWork : Worker() {
        override fun work() {
            if (mSocket == null) {
                mSocket = TimeTcpCheckSocket(true)
            }
            mSocket?.setDefaultTimeout(3000)
            mSocket?.setRetryCount(0) // no retry
            mSocket?.setEndChar('\n')

            TaskUtils.doRapidWork(object : Worker() {
                override fun work() {
                    try {
                        mSocket?.connect(AppConfig.config.socketHost, AppConfig.config.socketPort, object : ITcpConnectListener {
                            override fun onConnect(socket: TcpSocket?) {
                                mSocket?.sendString("SETID"+GsonBuilderFactory.createBuilder().toJson(createConnectData()), object : AbsBaseTimeSocketListener() {
                                    override fun onSuccess(data: String?) {
                                        mConnectCount = 0
                                        if (heartWork != null) {
                                            TaskUtils.removePostedWork(heartWork)
                                        }
                                        heartWork = HeartBeatWork()
                                        TaskUtils.postOnMain(heartWork)
                                    }
                                })
                            }

                            override fun onConnectFail(errCode: Int) {
                                mConnectCount++
                                if (mConnectCount < MAX_RETRY_COUNT) {
                                    TaskUtils.postOnMain(this@ConnectWork)
                                }
                            }

                            override fun onReceive(socket: TcpSocket?, data: String?) {
                                if (data != null && socket != null) {
                                    processReceive(socket!!, data!!)
                                }
                            }
                        })
                    } catch (e : Exception) {
                        e.printStackTrace()
                        mConnectCount++
                        if (mConnectCount < MAX_RETRY_COUNT) {
                            TaskUtils.postOnMain(this@ConnectWork)
                        }
                    }
                }
            })
        }
    }

    fun processReceive(socket: TcpSocket, data: String) {
        if (data.startsWith("EXEC_", false)) {
            if (data!!.length > 7) {
                var id = data!!.substring(5,7)
                var endIndex = data.lastIndexOf('^')
                if (endIndex >= 7) {
                    var command = data.substring(7, endIndex)
                    when (command) {
                        "airclean" -> {
                            var airData = AirCleanData()
                            var env = SocketManager.envData
                            if (env != null) {
                                airData.hdy = env.hdy
                                airData.ipm = env.pm
                                airData.tmp = env.tmp
                                airData.opm = SocketManager.pm25
                                airData.heat = PreferenceUtils.getBoolean(HomePreference.HEAT)
                                airData.autoRun = PreferenceUtils.getBoolean(HomePreference.AUTO_RUN)
                            }
                            socket?.send("#"+id+GsonBuilderFactory.createBuilder().toJson(airData)+"^\n")
                        }
                        "autorun0" -> {
                            PreferenceUtils.setBoolean(HomePreference.AUTO_RUN, false)
                            socket?.send("#"+id+"ok"+"^\n")
                        }
                        "autorun1" -> {
                            PreferenceUtils.setBoolean(HomePreference.AUTO_RUN, true)
                            socket?.send("#"+id+"ok"+"^\n")
                        }
                        "filterTime" -> {
                            socket?.send("#"+id+(System.currentTimeMillis() - PreferenceUtils.getLong(HomePreference.CHANGE_FILTER_TIP_TIME))+"^\n")
                        }
                        "rooms0" -> {
                            socket?.send("#"+id+GsonBuilderFactory.createBuilder().toJson(PreferenceUtils.getObject(HomePreference.ROOMS, Rooms::class.java))+"^\n")
                        }
                        "rooms1" -> { // 重新搜索房间
                            SocketManager.sendString("ASKCH1", object : TimeCheckSocket.AbsTimeSocketListener() {
                                override fun onSuccess(data: String?) {
                                    if (data != null && data.matches(Regex("C1=\\w+"))) {
                                        var rooms = Integer.parseInt(data.subSequence(3, data.length).toString(), 16)
                                        var mRooms = Rooms()
                                        mRooms?.mRooms = mutableListOf()
                                        for (i in 1..15) {
                                            if ((rooms and RoomSelectActivity.ROOM_MASKS[i]) > 0) {
                                                var item = Rooms.RoomItem()
                                                item.id = i
                                                item.name = "房间" + RoomSelectActivity.NUM_CHS[i]
                                                mRooms?.mRooms?.add(item)
                                            }
                                        }
                                        PreferenceUtils.setObject(HomePreference.ROOMS, mRooms)
                                    }
                                    socket?.send("#"+id+GsonBuilderFactory.createBuilder().toJson(PreferenceUtils.getObject(HomePreference.ROOMS, Rooms::class.java))+"^\n")
                                }
                            })
                        }
                        else -> {
                            if (command != null && command.startsWith("roomDetail")) {
                                var idnum = command.substring(10)
                                try {
                                    SocketManager.sendString("ASKMB"+HomeUtil.getChannelFromId(idnum.toInt()), object : TimeCheckSocket.AbsTimeSocketListener() {
                                        override fun onSuccess(data: String?) {
                                            if (data != null && data.matches(Regex("RETMB-MB=8\\d"))) {
                                                var level = (data.get(10) - '0').toInt()
                                                var detailData = RoomDetailData()
                                                var env = SocketManager.envData
                                                if (env != null) {
                                                    detailData.hdy = env.hdy
                                                    detailData.ipm = env.pm
                                                    detailData.tmp = env.tmp
                                                    detailData.opm = SocketManager.pm25
                                                    detailData.mode = level

                                                }
                                                socket?.send("#"+id+GsonBuilderFactory.createBuilder().toJson(detailData)+"^\n")
                                            } else {
                                                socket?.send("#"+id+GsonBuilderFactory.createBuilder().toJson(RoomDetailData())+"^\n")
                                            }
                                        }

                                        override fun onError(errorCode: Int) {
                                            super.onError(errorCode)
                                        }
                                    })
                                } catch (e : Exception) {
                                    socket?.send("#"+id+GsonBuilderFactory.createBuilder().toJson(RoomDetailData())+"^\n")
                                }
                                return
                            }

                            SocketManager.sendString(command, object : AbsBaseTimeSocketListener() {
                                override fun onSuccess(data: String?) {
                                    super.onSuccess(data)
                                    socket?.send("#" + id + data + "^\n")
                                }
                            })
                        }
                    }
                }
            }
        }
    }


    class HeartBeatWork : Worker() {
        override fun work() {
            TaskUtils.doRapidWork(object : Worker() {
                override fun work() {
                    if (mSocket != null) {
                        mSocket!!.sendString("HEART", object : AbsBaseTimeSocketListener() {
                            override fun onError(errorCode: Int) {
                                super.onError(errorCode)
                                heartErrorCount++;
                                if (heartErrorCount >= heartMaxErrorCount) {
                                    mSocket!!.disConnect()
                                    initConnect()
                                }
                            }

                            override fun onSuccess(data: String?) {
                                super.onSuccess(data)
                                heartErrorCount = 0
                            }
                        })
                    }
                    TaskUtils.postOnMain(this@HeartBeatWork, HEART_INTERNAL)
                }
            })
        }
    }

    fun initConnect() {
        if (mSocket != null) {
            if (!mSocket!!.isConnected) {
                mSocket!!.stop()
                mSocket = null
            }
        }
        heartErrorCount = 0
        TaskUtils.doRapidWork(connectWork)
    }

    fun createConnectData() : ConnectData {
        var connecData = ConnectData()
        var context = BaseApplication.getApplication()
        connecData.appid = DeviceId.getDeviceID(context)
        val packageManager = context.getPackageManager()
        val packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNATURES)
        connecData.vc = packageInfo.versionCode
        connecData.vName = packageInfo.versionName
        connecData.address = PreferenceUtils.getString(HomePreference.NET_LOGIN_ADDRESS)
        connecData.loginName = PreferenceUtils.getString(HomePreference.NET_LOGIN_NAME)
        connecData.loginPassd = PreferenceUtils.getString(HomePreference.NET_LOGIN_PASSWD)
        return connecData
    }
}