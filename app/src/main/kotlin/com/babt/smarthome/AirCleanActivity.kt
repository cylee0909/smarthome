package com.babt.smarthome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import cn.csnbgsh.herbarium.bind
import com.cylee.androidlib.base.BaseActivity
import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.TaskUtils
import com.cylee.lib.widget.dialog.DialogUtil
import com.cylee.socket.TimeCheckSocket
import org.jetbrains.anko.find
import org.jetbrains.anko.onUiThread
import java.net.DatagramPacket

/**
 * Created by cylee on 16/9/22.
 */
class AirCleanActivity :BaseActivity() , View.OnClickListener {
    companion object {
        fun createIntent(context : Context): Intent {
            return Intent(context, AirCleanActivity::class.java);
        }
    }

    var mTmpContainer : LinearLayout? = null
    var mHryContainer : LinearLayout? = null
    var mLeaveContainer : LinearLayout? = null
    var mAutoContainer : LinearLayout? = null
    var mHeatContainer : LinearLayout? = null
    var mSelectContainer : LinearLayout? = null
    var mTmpText : TextView? = null
    var mHdyText : TextView? = null
    var mPmText : TextView ?= null
    var askEnvWork = AskEnvRunnable()
    var paused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_air_clean)
        mTmpContainer = bind(R.id.ac_m_left_container)
        mHryContainer = bind(R.id.ac_m_right_container)
        mLeaveContainer = bind(R.id.ac_leave_container)
        mAutoContainer = bind(R.id.ac_auto_run_container)
        mHeatContainer = bind(R.id.ac_heat_container)
        mSelectContainer = bind(R.id.ac_select_container)

        mTmpText = bind(R.id.aac_tmp_tip)
        mHdyText = bind(R.id.aac_hdy_tip)
        mPmText = bind(R.id.aac_pm_tip)

        mLeaveContainer!!.setOnClickListener(this)
        mAutoContainer!!.setOnClickListener(this)
        mHeatContainer!!.setOnClickListener(this)
        mSelectContainer!!.setOnClickListener(this)

        find<View>(R.id.aac_exit).setOnClickListener {
            view -> onBackPressed()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ac_leave_container -> {}
            R.id.ac_auto_run_container -> {}
            R.id.ac_heat_container -> {
                dialogUtil.showWaitingDialog(this, "正在操作...", true)
                var command = if (mHeatContainer?.isSelected ?: false) "Close3" else "Open_3"
                SocketManager.sendString(command, object : TimeCheckSocket.AbsTimeSocketListener() {
                    override fun onSuccess(data: String?) {
                        onUiThread {
                            dialogUtil.dismissWaitingDialog()
                            mHeatContainer?.isSelected = !(mHeatContainer?.isSelected ?: false)
                            DialogUtil.showToast(this@AirCleanActivity,
                                    "加热模式已"+(if (mHeatContainer?.isSelected ?: false) "打开" else "关闭")
                                    , false)
                        }
                    }
                    override fun onRawData(rawData: DatagramPacket?) {
                        super.onRawData(rawData)
                    }

                    override fun onError(errorCode: Int) {
                        super.onError(errorCode)
                        onUiThread {
                            dialogUtil.dismissWaitingDialog()
                            DialogUtil.showToast(this@AirCleanActivity, "操作失败,请重试!", false)
                        }
                    }
                })
            }
            R.id.ac_select_container -> {
                startActivity(RoomSelectActivity.createIntent(this))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SocketManager.sendString("ASKCH3", object : TimeCheckSocket.AbsTimeSocketListener() {
            override fun onSuccess(data: String?) {
                if (data != null && data.matches(Regex("C3=\\d"))) {
                    var state = (data[3] - '0').toInt()
                    onUiThread {
                        if (state == 0) {
                            mHeatContainer?.isSelected = false
                        } else if (state == 1) {
                            mHeatContainer?.isSelected = true
                        }
                    }
                }
            }
            override fun onRawData(rawData: DatagramPacket?) {
                super.onRawData(rawData)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        paused = false
        TaskUtils.postOnMain(askEnvWork)
    }

    override fun onPause() {
        super.onPause()
        paused = true
        TaskUtils.removePostedWork(askEnvWork)
    }

    inner class AskEnvRunnable : Worker() {
        override fun work() {
            if (paused) return
            SocketManager.sendString("ASK_T", object : TimeCheckSocket.AbsTimeSocketListener() {
                override fun onError(errorCode: Int) {
                    TaskUtils.postOnMain(this@AskEnvRunnable, 1000)
                }
                override fun onRawData(rawData: DatagramPacket?) {
                }
                override fun onSuccess(data: String?) {
                    //PMTDH-12-00-12-00-23-17
                    if (data?.matches(Regex("PMTDH(-\\w{2}){6}")) ?: false) {
                        var items = data?.split('-')
                        if (items != null) {
                            var tmp = 0 ; var pm = 0; var hdy = 0
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

                            onUiThread {
                                mTmpText?.setText(tmp.toString()+"°C")
                                mHdyText?.setText(hdy.toString()+"%")
                                mPmText?.setText(String.format("室内\n%dug/m3", pm))
                            }
                        }
                    }
                    TaskUtils.postOnMain(this@AskEnvRunnable, 1000)
                }
            })
        }
    }
}