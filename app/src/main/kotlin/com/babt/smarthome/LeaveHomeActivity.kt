package com.babt.smarthome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import cn.csnbgsh.herbarium.bind
import com.babt.smarthome.entity.LeaveHomeData
import com.cylee.androidlib.base.BaseActivity
import com.cylee.androidlib.util.PreferenceUtils
import com.cylee.lib.widget.dialog.DialogUtil

/**
 * Created by cylee on 16/11/7.
 */
class LeaveHomeActivity : AppBaseActivity() {
    var modeStartSeek : SeekBar? = null
    var modeCompletedSeek : SeekBar? = null
    var ionStartSeek : SeekBar? = null
    var ionStopSeek : SeekBar? = null
    var confirm : TextView? = null

    var modeStartTip : TextView? = null
    var modeEndTip : TextView? = null
    var ionStartTip : TextView? = null
    var ionEndTip : TextView? = null

    var leaveData : LeaveHomeData? = null
    var enabled = false
    var changed = false

    companion object {
        fun createIntent(context : Context): Intent {
            var intent = Intent(context, LeaveHomeActivity::class.java)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leave_home)
        modeStartSeek = bind(R.id.alh_seek_mode_start)
        modeCompletedSeek = bind(R.id.alh_seek_mode_completed)
        ionStartSeek = bind(R.id.alh_seek_ion_start)
        ionStopSeek = bind(R.id.alh_seek_ion_stop)
        confirm = bind(R.id.alh_confirm_text)

        modeStartTip = bind(R.id.alh_mode_start_tip_text)
        modeEndTip = bind(R.id.alh_mode_stop_tip_text)
        ionStartTip = bind(R.id.alh_ion_start_tip_text)
        ionEndTip = bind(R.id.alh_ion_stop_tip_text)

        modeStartSeek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                modeStartTip?.text = "1、离家后,"+progress+"分钟离家模式开始运行"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        modeCompletedSeek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                modeEndTip?.text = "4、高能离子停止后"+(progress + 1)+"分钟风机停止,离家模式完成"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        ionStartSeek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ionStartTip?.text="2、风机运行后"+(progress + 1)+"分钟高能离子开始运行"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        ionStopSeek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ionEndTip?.text = "3、高能离子运行"+(progress + 1)+"分钟后停止"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        confirm?.setOnClickListener {
            changed = true
            enabled = !enabled
            confirm?.setText(if (enabled) "关闭" else "打开")
            DialogUtil.showToast(this, if (enabled) "打开成功" else "关闭成功", false)
            finish()
        }

        bind<View>(R.id.alh_exit).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        leaveData = PreferenceUtils.getObject(HomePreference.LEAVE_HOME, LeaveHomeData::class.java)

        enabled = leaveData != null
        if (enabled) {
            var modeStartTime = (leaveData?.modeStartTime ?: 0).toMinus().toInt()
            modeStartSeek?.progress = modeStartTime

            var modeEndTime = (leaveData?.modeEndTime ?: 0).toMinus().toInt()
            modeCompletedSeek?.progress = modeEndTime - 1

            var ionStartTime = (leaveData?.ionStartTime ?: 0).toMinus().toInt()
            ionStartSeek?.progress = ionStartTime - 1

            var ionEndTime = (leaveData?.ionEndTime ?: 0).toMinus().toInt()
            ionStopSeek?.progress = ionEndTime - 1

            confirm?.setText("关闭")
        } else{
            confirm?.setText("打开")
        }
    }

    override fun onStop() {
        super.onStop()
        if (changed) {
            if (enabled) {
                if (leaveData == null) {
                    leaveData = LeaveHomeData()
                }
                leaveData?.startTime = System.currentTimeMillis()
                leaveData?.ionStartTime = (ionStartSeek?.progress!! + 1) * 1000 * 60.toLong()
                leaveData?.ionEndTime = (ionStopSeek?.progress!! + 1)* 1000 * 60.toLong()
                leaveData?.modeStartTime = (modeStartSeek?.progress!!) * 1000 * 60.toLong()
                leaveData?.modeEndTime = (modeCompletedSeek?.progress!! + 1)* 1000 * 60.toLong()
                PreferenceUtils.setObject(HomePreference.LEAVE_HOME, leaveData)
            } else{
                PreferenceUtils.setObject(HomePreference.LEAVE_HOME, null)
            }
        }
    }
}