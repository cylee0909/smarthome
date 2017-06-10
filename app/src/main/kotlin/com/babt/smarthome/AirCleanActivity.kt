package com.babt.smarthome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import cn.csnbgsh.herbarium.bind
import com.babt.smarthome.entity.Pm25
import com.cylee.androidlib.base.BaseActivity
import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.PreferenceUtils
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
    var mOutPmText : TextView ?= null
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
        mOutPmText = bind(R.id.out_pm_text)

        mLeaveContainer!!.setOnClickListener(this)
        mAutoContainer!!.setOnClickListener(this)
        mHeatContainer!!.setOnClickListener(this)
        mSelectContainer!!.setOnClickListener(this)

        find<View>(R.id.aac_exit).setOnClickListener {
            view -> onBackPressed()
        }

        find<View>(R.id.shutdown).setOnClickListener {
            dialogUtil.showWaitingDialog(this, "正在操作...", true)
            SocketManager.sendString("SETMBG", object : TimeCheckSocket.AbsTimeSocketListener() {
                override fun onSuccess(data: String?) {
                    onUiThread {
                        dialogUtil.dismissWaitingDialog()
                        DialogUtil.showToast(this@AirCleanActivity, "关机成功!", false)
                    }
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

        var pm = PreferenceUtils.getObject(HomePreference.PM25, Pm25::class.java)
        if (pm != null) {
            mOutPmText?.text = "室外\n"+pm.p+"ug/m3"
        }
        SocketManager.pmListener = object : SocketManager.PmChangeListener {
            override fun onChange(pm: String) {
                mOutPmText?.text = "室外\n"+pm+"ug/m3"
            }
        }

        if (PreferenceUtils.getBoolean(HomePreference.AUTO_RUN)) {
            mAutoContainer?.isSelected = true
        } else{
            mAutoContainer?.isSelected = false
        }

        mAutoContainer!!.setOnLongClickListener {
            val v = View.inflate(this, R.layout.input_pm, null);
            val numEdit = v.bind<EditText>(R.id.num_edit)
            numEdit.setText(PreferenceUtils.getInt(HomePreference.AUTO_RUN_SET_PM25).toString())
            numEdit.setSelection(numEdit.text.length)
            DialogUtil().showViewDialog(this, "自动运行设置", "", "确认",object : DialogUtil.ButtonClickListener {
                override fun OnRightButtonClick() {
                    var s = numEdit.text.toString()
                    if (!TextUtils.isEmpty(s)) {
                        var input = s.toInt()
                        input = Math.min(100, Math.max(input, 10))
                        PreferenceUtils.setInt(HomePreference.AUTO_RUN_SET_PM25, input)
                    }
                }
                override fun OnLeftButtonClick() {
                }
            }, v)
            return@setOnLongClickListener true
        }

        var powerSeek = bind<SeekBar>(R.id.aac_power_seek);
        powerSeek.progress = PreferenceUtils.getInt(HomePreference.POWER_SEEK)
        powerSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                var current = seekBar?.progress ?: 0;
                var currentStr = String.format("%04d", current)
                SocketManager.sendString("SETPID"+currentStr, object : TimeCheckSocket.AbsTimeSocketListener() {
                    override fun onSuccess(data: String?) {
                        onUiThread {
                            DialogUtil.showToast(this@AirCleanActivity, "操作成功!", false)
                            PreferenceUtils.setInt(HomePreference.POWER_SEEK, current)
                        }
                    }
                    override fun onError(errorCode: Int) {
                        super.onError(errorCode)
                        onUiThread {
                            DialogUtil.showToast(this@AirCleanActivity, "操作失败,请重试!", false)
                        }
                    }
                })
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketManager.pmListener = null
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ac_leave_container -> {
                startActivity(LeaveHomeActivity.createIntent(this))
            }
            R.id.ac_auto_run_container -> {
                mAutoContainer?.isSelected = (!(mAutoContainer?.isSelected ?: false))
                var autoRun = mAutoContainer?.isSelected ?: false
                PreferenceUtils.setBoolean(HomePreference.AUTO_RUN, autoRun)
                DialogUtil.showToast(this@AirCleanActivity,
                        "自动运行"+(if (autoRun) "打开" else "关闭")
                        , false)
            }
            R.id.ac_heat_container -> {
                dialogUtil.showWaitingDialog(this, "正在操作...", true)
                var isOpen = mHeatContainer?.isSelected ?: false;
                var command = if (isOpen) "Close3" else "Open_3"
                SocketManager.sendString(command, object : TimeCheckSocket.AbsTimeSocketListener() {
                    override fun onSuccess(data: String?) {
                        onUiThread {
                            dialogUtil.dismissWaitingDialog()
                            if (!isOpen && TextUtils.equals(data, "NOPEN")) { // 打开
                                DialogUtil.showToast(this@AirCleanActivity, "打开失败,请先打开面板!", false)
                                return@onUiThread
                            }
                            mHeatContainer?.isSelected = !(mHeatContainer?.isSelected ?: false)
                            DialogUtil.showToast(this@AirCleanActivity,
                                    "加热模式已"+(if (mHeatContainer?.isSelected ?: false) "打开" else "关闭")
                                    , false)
                            PreferenceUtils.setBoolean(HomePreference.HEAT, mHeatContainer!!.isSelected)
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
                        PreferenceUtils.setBoolean(HomePreference.HEAT, mHeatContainer!!.isSelected)
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
            var envData = SocketManager.envData
            mTmpText?.setText(envData.tmp.toString()+"°C")
            mHdyText?.setText(envData.hdy.toString()+"%")
            mPmText?.setText(String.format("室内\n%dug/m3", envData.pm))
            TaskUtils.postOnMain(this, 10000)
        }
    }
}