package com.babt.smarthome

import android.os.Handler
import android.os.Looper
import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.Log
import com.cylee.androidlib.util.TaskUtils
import com.cylee.socket.TimeCheckSocket
import com.cylee.socket.tcp.BaseTimeSocketListener
import com.cylee.socket.tcp.ITcpConnectListener
import com.cylee.socket.tcp.TcpSocket
import com.cylee.socket.tcp.TimeTcpCheckSocket
import java.net.DatagramPacket
import java.net.InetSocketAddress


/**
 * Created by cylee on 16/9/26.
 */
object SocketManager {
    var handler : Handler? = null
    var mAddressSocket: TimeCheckSocket? = null
    var mDataSocket : TimeTcpCheckSocket? = null;
    var initCount = 0
    var listener : InitListener? = null
    var initRunnable  = InitRunnable()
    var retry : Boolean = false
    fun init() {
        handler = Handler(Looper.getMainLooper())
        TaskUtils.doRapidWorkAndPost(object : Worker() {
            override fun work() {
                if (mAddressSocket == null) {
                    mAddressSocket = TimeCheckSocket.client(InetSocketAddress("255.255.255.255", 8001),null, 8001)
                }
            }
        }, object : Worker() {
            override fun work() {
                handler?.post(initRunnable)
            }
        })
    }

    fun retry(listener:InitListener) {
        this.listener = listener
        retry = true
        handler?.post(initRunnable)
    }

    fun sendString(data : String, listener : BaseTimeSocketListener) {
        if (mDataSocket != null) {
            mDataSocket?.sendString(data, listener)
        }
    }

    fun isInitSuccess():Boolean {
        return mDataSocket?.isConnected() ?: false
    }

    class InitRunnable : Runnable {
        override fun run() {
            handler?.removeCallbacks(this)
            Log.d("init runnable run , initCount = "+ initCount)
            if (retry || (!isInitSuccess() && initCount < 20)) {
                mAddressSocket?.sendString("ASKIP0", object : TimeCheckSocket.AbsTimeSocketListener() {
                    override fun onError(errorCode: Int) {
                        initCount ++
                        handler?.postDelayed(this@InitRunnable, 500)
                    }

                    override fun onSuccess(data: String?) {

                    }

                    override fun onRawData(rawData: DatagramPacket?) {
                        if (rawData != null) {
                            mDataSocket = TimeTcpCheckSocket(true);
                            mDataSocket?.connect(rawData.address.hostAddress, 8000, object : ITcpConnectListener{
                                override fun onConnect(socket: TcpSocket?) {
                                    Log.d("socket init success!")
                                    listener?.onInitSuccess()
                                }

                                override fun onConnectFail(errCode: Int) {
                                    initCount ++
                                    handler?.postDelayed(this@InitRunnable, 500)
                                }

                                override fun onReceive(socket: TcpSocket?, data: String?) {

                                }
                            })
                        }
                    }
                })
            }
        }
    }

    interface InitListener {
        fun onInitSuccess()
        fun onInitFail()
    }
}