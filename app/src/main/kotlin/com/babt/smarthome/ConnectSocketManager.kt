package com.babt.smarthome

import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.TaskUtils
import com.cylee.socket.tcp.ITcpConnectListener
import com.cylee.socket.tcp.TcpSocket
import com.cylee.socket.tcp.TimeTcpCheckSocket

/**
 * Created by cylee on 16/12/15.
 * 连接远程服务器的管理类
 */
object ConnectSocketManager {
    const val HOST = "http://127.0.0.1"
    const val PORT = 8000
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
            mSocket?.connect(HOST, PORT, object : ITcpConnectListener {
                override fun onConnect(socket: TcpSocket?) {
                    mConnectCount = 0
                    if (heartWork != null) {
                        TaskUtils.removePostedWork(heartWork)
                    }
                    heartWork = HeartBeatWork()
                    TaskUtils.postOnMain(heartWork)
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
        TaskUtils.postOnMain(connectWork)
    }
}