package com.babt.smarthome

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.android.volley.toolbox.StringRequest
import com.babt.smarthome.entity.LeaveHomeData
import com.babt.smarthome.entity.Pm25
import com.babt.smarthome.entity.Rooms
import com.babt.smarthome.entity.TimeSet
import com.babt.smarthome.model.Config
import com.babt.smarthome.model.Verify
import com.babt.smarthome.util.EncryptUtil
import com.baidu.android.common.util.DeviceId
import com.cylee.androidlib.base.BaseApplication
import com.cylee.androidlib.net.Net
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
    var mDataSocket: TimeTcpCheckSocket? = null
    var initCount = 0
    var listener: InitListener? = null
    var initRunnable = InitRunnable()
    var retry: Boolean = false
    var envWork = AskEnvRunnable()
    var timeWord = TimeRunnable()
    var lock = ReentrantLock()
    var lastTimeCheck = ""
    var pantErrorCount = 0
    var mMaxNullCheck = 10
    var nullCheckCount = 0
    var pm25 = 0

    var envData = EnvData()
        get() {
            lock.withLock {
                return field
            }
        }

    fun init() {
        initCount = 0
        pantErrorCount = 0
        handler = Handler(Looper.getMainLooper())
        TaskUtils.doRapidWorkAndPost(object : Worker() {
            override fun work() {
                if (mAddressSocket == null) {
                    mAddressSocket = TimeCheckSocket.client(InetSocketAddress("255.255.255.255", 8001), null, 8001)
                }
            }
        }, object : Worker() {
            override fun work() {
                if(PreferenceUtils.getBoolean(HomePreference.NET_INITED)) {
                    handler?.post(initRunnable)
                }
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

    fun sendString(data: String, listener: BaseTimeSocketListener?) {
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
                retry = false
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
                            mDataSocket = TimeTcpCheckSocket(true)
                            try {
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
                                        if (data == null) {
                                            nullCheckCount ++
                                            if (nullCheckCount < mMaxNullCheck) {
                                                reconnect()
                                            }
                                        } else{
                                            nullCheckCount = 0
                                        }
                                    }
                                })
                            } catch (e : Exception) {
                                e.printStackTrace()
                                initCount++
                                handler?.postDelayed(this@InitRunnable, 500)
                            }
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

    interface PmChangeListener {
        fun onChange(pm : String)
    }

    var pmListener : PmChangeListener? = null

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

                var leaveData = PreferenceUtils.getObject(HomePreference.LEAVE_HOME, LeaveHomeData::class.java)
                if (leaveData != null) {
                    var timeUsed:Int = (System.currentTimeMillis() - leaveData.startTime).toMinus().toInt()
                    var modeStartTime = leaveData.modeStartTime.toMinus().toInt()
                    var ionStartTime = leaveData.ionStartTime.toMinus().toInt()
                    var ionStopTime = leaveData.ionEndTime.toMinus().toInt()
                    var modeStopTime = leaveData.modeEndTime.toMinus().toInt()
                    if (timeUsed == modeStartTime) {
                        var rooms = PreferenceUtils.getObject(HomePreference.ROOMS, Rooms::class.java)?.mRooms
                        if (rooms != null) {
                            rooms.forEachIndexed {
                                i, room ->
                                TaskUtils.postOnMain(object : Worker() {
                                    override fun work() {
                                        Log.d("setmb "+HomeUtil.getChannelFromId(room.id))
                                        SocketManager.sendString("SETMB" + HomeUtil.getChannelFromId(room.id), null)
                                    }
                                }, i * 1000)
                            }
                        }
                    }

                    if (timeUsed == modeStartTime + ionStartTime) {
                        Log.d("open_2")
                        SocketManager.sendString("Open_2", null)
                    }
                    if (timeUsed == modeStartTime + ionStartTime + ionStopTime) {
                        Log.d("close2")
                        SocketManager.sendString("Close2", null)
                    }

                    if (timeUsed == modeStartTime + ionStartTime + ionStopTime + modeStopTime) {
                        var rooms = PreferenceUtils.getObject(HomePreference.ROOMS, Rooms::class.java)?.mRooms
                        if (rooms != null) {
                            rooms.forEachIndexed {
                                i, room ->
                                TaskUtils.postOnMain(object : Worker() {
                                    override fun work() {
                                        Log.d("deset "+HomeUtil.getChannelFromId(room.id))
                                        SocketManager.sendString("DESMB" + HomeUtil.getChannelFromId(room.id), null)
                                    }
                                }, i * 1000)
                            }
                        }
                    }

                    if (timeUsed >= modeStartTime + ionStartTime + ionStopTime + modeStopTime) {
                        PreferenceUtils.setObject(HomePreference.LEAVE_HOME, null)
                    }
                }
                refreshPm25()
                checkVerify()
            }

            refreshAutoRun()
            TaskUtils.postOnMain(this, 1000 * 20) // 10s
        }
    }

    var currentLevel = 0
    // 自动运行
    fun refreshAutoRun() {
        if (PreferenceUtils.getBoolean(HomePreference.AUTO_RUN)) {
            if (envData != null) {
                var lastTime = PreferenceUtils.getLong(HomePreference.AUTO_RUN_TIME)
                if (System.currentTimeMillis() - lastTime > 20 * 1000) { // 20s
                    PreferenceUtils.setLong(HomePreference.AUTO_RUN_TIME, System.currentTimeMillis())
                    var setPm = PreferenceUtils.getInt(HomePreference.SET_PM25)
                    if (envData.pm >= setPm) {
                        if (currentLevel < 10) {
                            currentLevel++
                            setPmLevel(currentLevel)
                        }
                    } else {
                        if (currentLevel > 0) {
                            currentLevel--
                            setPmLevel(currentLevel)
                        }
                    }
                }
            }
        }
    }

    fun setPmLevel(level : Int) {
        var rooms = PreferenceUtils.getObject(HomePreference.ROOMS, Rooms::class.java)?.mRooms
        if (rooms != null) {
            rooms.forEachIndexed {
                i, room ->
                TaskUtils.postOnMain(object : Worker() {
                    override fun work() {
                        SocketManager.sendString("SETMB" + HomeUtil.getChannelFromId(room.id) + HomeUtil.getPmSetLevel(level), null)
                    }
                }, i * 1000)
            }
        }
    }

    fun refreshPm25() {
        var pm = PreferenceUtils.getObject(HomePreference.PM25, Pm25::class.java)
        if (true || pm == null || (pm.t -  System.currentTimeMillis()) >= 1000 * 60 * 60) { // 1h
            var req = StringRequest("http://wthrcdn.etouch.cn/WeatherApi?city=%E5%8C%97%E4%BA%AC", object : Net.SuccessListener<String>() {
                override fun onResponse(response: String?) {
                    if (response != null) {
                        var match = "<pm25>(\\d+)</pm25>".toRegex().find(response)
                        if (match != null && match.groupValues != null && match.groupValues.size >= 2) {
                            var pm25 = match.groupValues[1]
                            if (pm25 != null) {
                                var p = Pm25(pm25, System.currentTimeMillis())
                                try {
                                 this@SocketManager.pm25 = p.p?.toInt() ?: 0
                                } catch (e : Exception){
                                }
                                PreferenceUtils.setObject(HomePreference.PM25, p)
                                pmListener?.onChange(pm25)
                            }
                        }
                    }
                }
            }, null)
            Net.fetchReqQueue().add(req)
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
                        SocketManager.sendString(command + HomeUtil.getChannelFromId(id), null)
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

    fun checkVerify() {
        if (System.currentTimeMillis() - PreferenceUtils.getLong(HomePreference.VERIFY_TIME) > 24 * 60 * 60 * 1000) {
            Net.post(BaseApplication.getApplication(), Config.buidInput(DeviceId.getDeviceID(BaseApplication.getApplication())), object : Net.SuccessListener<Config>() {
                override fun onResponse(response: Config?) {
                    PreferenceUtils.setLong(HomePreference.VERIFY_TIME, System.currentTimeMillis())
                    if (response != null) {
                        PreferenceUtils.setBoolean(HomePreference.NEED_VERIFY, response.verify)
                        if (response.verify) {
                            var s = PreferenceUtils.getString(HomePreference.VERIFY_KEY)
                            if (!TextUtils.isEmpty(s)) {
                                var id = DeviceId.getDeviceID(BaseApplication.getApplication())
                                var input = Verify.buidInput(id, s)
                                Net.post(BaseApplication.getApplication(), input, object : Net.SuccessListener<Verify>() {
                                    override fun onResponse(response: Verify?) {
                                        if (response != null) {
                                            PreferenceUtils.setBoolean(HomePreference.VERIFIED, true)
                                            if (!(response?.result?.equals(EncryptUtil.getVerify(id)) ?: false)) {
                                                PreferenceUtils.setBoolean(HomePreference.VERIFY_SUCCESS, false)
                                            }
                                        }
                                    }
                                }, null)
                            }
                        }
                    }
                }
            }, null)
        }
    }

    class AskEnvRunnable : Worker() {
        override fun work() {
            SocketManager.sendString("ASK_T", object : TimeCheckSocket.AbsTimeSocketListener() {
                override fun onError(errorCode: Int) {
                    TaskUtils.postOnMain(this@AskEnvRunnable, 10000)
                    pantErrorCount++
                    if (pantErrorCount >= 5) {
                        reconnect()
                    }
                }

                override fun onSuccess(data: String?) {
                    pantErrorCount = 0
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
        var tmp = 0
        var hdy = 0
        var pm = 0
    }
}