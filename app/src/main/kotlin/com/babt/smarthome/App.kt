package com.babt.smarthome

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.os.PowerManager
import com.cylee.androidlib.base.BaseApplication
import com.cylee.androidlib.net.Config
import com.cylee.androidlib.util.Log
import com.cylee.androidlib.util.PreferenceUtils
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by cylee on 16/9/20.
 */
class App : BaseApplication() {
    var lock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, packageName)
        lock?.acquire()

        Config.setHost(AppConfig.config.serverHttpUrl)
        Log.setLogLevel(if (BuildConfig.DEBUG) Log.OFF else Log.OFF)
        bindSocket()
        redirectLog()
    }

    fun bindSocket() {
        SocketManager.reconnect()
        // 如果已经设置过网络,则尝试初始化连接
        if (PreferenceUtils.getBoolean(HomePreference.NET_INITED)) {
            ConnectSocketManager.initConnect()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        lock?.release()
    }

    //测试包或者非release包将日志输出到文件中，方便查问题
    fun redirectLog() {
        if (BuildConfig.DEBUG) {
            val defHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
                var osw: OutputStreamWriter? = null
                try {
                    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd/HH:mm:ss", Locale.CHINA)
                    val log = File(Environment.getExternalStorageDirectory(), "scan_crash.log")
                    if (!log.exists()) {
                        log.createNewFile()
                    }

                    osw = OutputStreamWriter(FileOutputStream(log, false))
                    var baos = ByteArrayOutputStream()
                    val pw = PrintWriter(baos)
                    pw.println("\n============================================================\n")
                    pw.println(simpleDateFormat.format(Date()))
                    ex.printStackTrace(pw)
                    pw.flush()
                    var content = baos.toString("utf-8")

                    var clipboard = getSystemService(BaseApplication.CLIPBOARD_SERVICE) as ClipboardManager
                    var textCd = ClipData.newPlainText("crash_log",content)
                    clipboard.setPrimaryClip(textCd);

                    osw!!.write(content)
                    osw.flush()
                } catch (e: Exception) {
                } finally {
                    if (osw != null) {
                        try {
                            osw.close()
                        } catch (e: Exception) {
                        }

                    }
                }
                defHandler?.uncaughtException(thread, ex)
            }
        }
    }
}