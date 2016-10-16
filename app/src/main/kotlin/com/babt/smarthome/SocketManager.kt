package com.babt.smarthome

import android.os.Handler
import android.os.Looper
import com.babt.smarthome.entity.TimeSet
import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.Log
import com.cylee.androidlib.util.PreferenceUtils
import com.cylee.androidlib.util.TaskUtils
import com.cylee.socket.TimeCheckSocket
import com.cylee.socket.tcp.BaseTimeSocketListener
import com.cylee.socket.tcp.ITcpConnectListener
import com.cylee.socket.tcp.TcpSocket
import com.cylee.socket.tcp.TimeTcpCheckSocket
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * Created by cylee on 16/9/26.
 */
object SocketManager {
    var handler: Handler? = null
    var mAddressSocket: TimeCheckSocket? = null
    var mDataSocket: TimeTcpCheckSocket? = null;
    var initCount = 0
    var listener: InitListener? = null
    var initRunnable = InitRunnable()
    var retry: Boolean = false
    var envWork = AskEnvRunnable()
    var timeWord = TimeRunnable()
    var lock = ReentrantLock()
    var lastTimeCheck = ""

    var envData = EnvData()
        get() {
            lock.withLock {
                return field
            }
        }

    fun init() {
        initCount = 0
        handler = Handler(Looper.getMainLooper())
        TaskUtils.doRapidWorkAndPost(object : Worker() {
            override fun work() {
                if (mAddressSocket == null) {
                    mAddressSocket = TimeCheckSocket.client(InetSocketAddress("255.255.255.255", 8001), null, 8001)
                }
            }
        }, object : Worker() {
            override fun work() {
                handler?.post(initRunnable)
            }
        })
    }

    fun reconnect() {
        TaskUtils.removePostedWork(envWork)
        TaskUtils.removePostedWork(timeWord)
        mDataSocket?.disConnect()
        mAddressSocket?.disConnect()
        mAddressSocket = null
        mDataSocket = null
        init()
    }

    fun retry(listener: InitListener) {
        this.listener = listener
        retry = true
        handler?.post(initRunnable)
    }

    fun sendString(data: String, listener: BaseTimeSocketListener) {
        if (mDataSocket != null) {
            mDataSocket?.sendString(data, listener)
        }
    }

    fun isInitSuccess(): Boolean {
        return mDataSocket?.isConnected() ?: false
    }

    class InitRunnable : Runnable {
        override fun run() {
            handler?.removeCallbacks(this)
            Log.d("init runnable run , initCount = " + initCount)
            if (retry || (!isInitSuccess() && initCount < 20)) {
//                DialogUtil.showToast(BaseApplication.getApplication(), "发ASKIP请求", false)
                mAddressSocket?.sendString("ASKIP0", object : TimeCheckSocket.AbsTimeSocketListener() {
                    override fun onError(errorCode: Int) {
//                        DialogUtil.showToast(BaseApplication.getApplication(), "UDP连接失败", false)
                        initCount++
                        handler?.postDelayed(this@InitRunnable, 500)
                    }

                    override fun onSuccess(data: String?) {

                    }

                    override fun onRawData(rawData: DatagramPacket?) {
                        if (rawData != null) {
//                            DialogUtil.showToast(BaseApplication.getApplication(), "ip连接: "+rawData.address.hostAddress, false)
                            mDataSocket = TimeTcpCheckSocket(true);
                            mDataSocket?.connect(rawData.address.hostAddress, 8000, object : ITcpConnectListener {
                                override fun onConnect(socket: TcpSocket?) {
                                    TaskUtils.removePostedWork(envWork)
                                    TaskUtils.removePostedWork(timeWord)
                                    Log.d("socket init success!")
                                    listener?.onInitSuccess()
                                    TaskUtils.postOnMain(envWork)
                                    TaskUtils.postOnMain(timeWord)
                                }

                                override fun onConnectFail(errCode: Int) {
//                                    DialogUtil.showToast(BaseApplication.getApplication(), "TCP连接失败 : "+errCode, false)
                                    initCount++
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

    /**
     * 定时任务
     */
    class TimeRunnable : Worker() {
        override fun work() {
            var currentTime = HomeUtil.timeFormat.format(Date())
            if (!currentTime.equals(lastTimeCheck)) {
                lastTimeCheck = currentTime
                var timeSet = PreferenceUtils.getObject(HomePreference.TIMES, TimeSet::class.java)
                if (timeSet != null) {
                    var changed = false
                    timeSet.timeMap.forEach {
                        k ->
                        var id = k.key
                        var times = k.value
                        changed = changed or processTime(id, times)
                    }
                    if (changed) {
                        PreferenceUtils.setObject(HomePreference.TIMES, timeSet)
                    }
                }
            }
            TaskUtils.postOnMain(this, 1000 * 20) // 10s
        }
    }

    fun processTime(id: Int, times: MutableList<TimeSet.TimeItem>): Boolean {
        if (times != null) {
            var removeItem : MutableList<TimeSet.TimeItem> = arrayListOf()
            times.forEach {
                v ->
                var nextDate = HomeUtil.getNextDateByWeek(v.week, v.time)
                var current = Date()
                if (HomeUtil.dateFormat.format(current).equals(nextDate)) { // 同一天
                    if (HomeUtil.timeFormat.format(current).equals(v.time)) { // 时间一致
                        var command = if (v.action == 0) "SETMB" else "DESMB"
                        SocketManager.sendString(command + HomeUtil.getChannelFromId(id), object : BaseTimeSocketListener {
                            override fun onError(errorCode: Int) {
                            }

                            override fun onSuccess(data: String?) {

                            }
                        })
                        if (v.repeat == 1) { // 一次
                            removeItem.add(v)
                        }
                    }
                }
            }
            if (removeItem.isNotEmpty()) {
                times.removeAll(removeItem)
                return true
            }
        }
        return false
    }

    class AskEnvRunnable : Worker() {
        override fun work() {
            SocketManager.sendString("ASK_T", object : TimeCheckSocket.AbsTimeSocketListener() {
                override fun onError(errorCode: Int) {
                    TaskUtils.postOnMain(this@AskEnvRunnable, 1000)
                }

                override fun onSuccess(data: String?) {
                    //PMTDH-12-00-12-00-23-17
                    if (data?.matches(Regex("PMTDH(-\\w{2}){6}")) ?: false) {
                        var items = data?.split('-')
                        if (items != null) {
                            var tmp = 0;
                            var pm = 0;
                            var hdy = 0
                            if (items.size >= 2) {
                                hdy = Integer.parseInt(items.get(1), 16)
                            }
                            if (items.size >= 4) {
                                var pmStr = items.get(2) + items.get(3);
                                pm = Integer.parseInt(pmStr, 16)
                            }
                            if (items.size >= 6) {
                                tmp = Integer.parseInt(items.get(5), 16)
                            }
                            lock.withLock {
                                envData.hdy = hdy
                                envData.pm = pm
                                envData.tmp = tmp
                            }
                        }
                    }
                    TaskUtils.postOnMain(this@AskEnvRunnable, 10000)
                }
            })
        }
    }

    class EnvData {
        var tmp = 0;
        var hdy = 0;
        var pm = 0;
    }
}