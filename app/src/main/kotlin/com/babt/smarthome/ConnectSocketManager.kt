package com.babt.smarthome

import android.content.pm.PackageManager
import android.os.Build
import com.baidu.android.common.util.DeviceId
import com.cylee.androidlib.GsonBuilderFactory
import com.cylee.androidlib.base.BaseApplication
import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.TaskUtils
import com.cylee.socket.AbsBaseTimeSocketListener
import com.cylee.socket.tcp.ITcpConnectListener
import com.cylee.socket.tcp.TcpSocket
import com.cylee.socket.tcp.TimeTcpCheckSocket

/**
 * Created by cylee on 16/12/15.
 * 连接远程服务器的管理类
 */
object ConnectSocketManager {
    const val HOST = "192.168.31.103"
    const val PORT = 8989
    const val MAX_RETRY_COUNT = 100
    const val HEART_INTERNAL = 40 * 1000 //40s
    var mConnectCount = 0
    var mSocket: TimeTcpCheckSocket? = null
    var connectWork = ConnectWork()
    var heartWork : HeartBeatWork? = null

    class ConnectWork : Worker() {
        override fun work() {
            if (mSocket == null) {
                mSocket = TimeTcpCheckSocket(true)
            }
            mSocket?.setEndChar('\n');
            mSocket?.connect(HOST, PORT, object : ITcpConnectListener {
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
                }
            })
        }
    }

    class HeartBeatWork : Worker() {
        override fun work() {
            if (mSocket != null) {
                mSocket!!.sendString("HEART", null)
            }
            TaskUtils.postOnMain(this, HEART_INTERNAL)
        }
    }

    fun initConnect() {
        if (mSocket != null) {
            if (!mSocket!!.isConnected) {
                mSocket!!.stop()
                mSocket = null
            }
        }
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
        connecData.address = "美和园西区7号楼4单元802"
        return connecData
    }

    class ConnectData {
        var appid : String = ""
        var vc : Int = 0
        var vName : String = ""
        var address : String = ""
    }
}