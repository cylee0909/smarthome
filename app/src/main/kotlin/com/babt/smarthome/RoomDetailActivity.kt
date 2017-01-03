package com.babt.smarthome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import cn.csnbgsh.herbarium.bind
import com.babt.smarthome.entity.Rooms
import com.cylee.androidlib.base.BaseActivity
import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.TaskUtils
import com.cylee.lib.widget.dialog.DialogUtil
import com.cylee.socket.TimeCheckSocket
import org.jetbrains.anko.find
import org.jetbrains.anko.onUiThread

/**
 * Created by cylee on 16/9/27.
 */
class RoomDetailActivity : AppBaseActivity() , View.OnClickListener{
    companion object {
        val INPUT_ROOM_ITEM = "INPUT_ROOM_ITEM"
        fun createIntent(context : Context, roomItem: Rooms.RoomItem): Intent {
            var intent = Intent(context, RoomDetailActivity::class.java)
            intent.putExtra(INPUT_ROOM_ITEM, roomItem)
            return intent
        }
    }
    var mStoped = true
    var mTimeSetText : TextView? = null
    var mPmTipText : TextView? = null
    var mTimeTip : TextView? = null
    var mTmpText :TextView? = null
    var mHdyText :TextView? = null
    var mFanIcon :ImageView? = null
    var mHighIcon :ImageView? = null
    var mMediumIcon :ImageView? = null
    var mLowIcon:ImageView? = null
    var mStartIcon :ImageView? = null
    var mTitleText : TextView? = null
    var askEnvWork = AskEnvRunnable()
    var checkLevelWork = CheckLevelRunnable()
    var mStarted = false
    var mCurrentId = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)
        mTimeSetText = bind(R.id.ard_set_time_text)
        mPmTipText = bind(R.id.ard_pm_tip_text)
        mTimeTip = bind(R.id.ard_time_tip_text)
        mTmpText = bind(R.id.ard_tmp_text)
        mHdyText = bind(R.id.ard_hdy_text)
        mFanIcon = bind(R.id.ard_fan_icon)
        mHighIcon = bind(R.id.ard_high_icon)
        mMediumIcon = bind(R.id.ard_medium_icon)
        mLowIcon = bind(R.id.ard_low_icon)
        mStartIcon = bind(R.id.ard_start_icon)
        mTitleText = bind(R.id.ard_title)

        mTimeSetText?.setOnClickListener(this)
        mStartIcon?.setOnClickListener (this )
        mLowIcon?.setOnClickListener (this)
        mMediumIcon?.setOnClickListener(this)
        mHighIcon?.setOnClickListener(this)

        if (intent != null) {
            var item = intent.getSerializableExtra(INPUT_ROOM_ITEM) as Rooms.RoomItem
            if (item != null) {
                mTitleText?.setText(item.name)
                mCurrentId = item.id
            }
        }

        find<View>(R.id.ard_exit).setOnClickListener {
            view -> onBackPressed()
        }
    }

    fun refreshStartState() {
        if (mStarted) {
            mStartIcon?.setImageResource(R.drawable.icon_stop)
        } else{
            mStartIcon?.setImageResource(R.drawable.icon_start)
            mLowIcon?.isEnabled = true
            mMediumIcon?.isEnabled = true
            mHighIcon?.isEnabled = true
        }
    }

    fun getChannelFromPosition(p :Int):Char {
        if (p <= 9) return '0' + (p - 0)
        return 'a' + (p - 10)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.ard_set_time_text -> {
                startActivity(SetTimeListActivity.createIntent(this, mCurrentId))
            }
            R.id.ard_start_icon -> {
                var command = if (mStarted)  "DESMB" else "SETMB"
                dialogUtil.showWaitingDialog(this, "正在操作...", true)
                SocketManager.sendString(command+getChannelFromPosition(mCurrentId), object : TimeCheckSocket.AbsTimeSocketListener() {
                    override fun onSuccess(data: String?) {
                        onUiThread {
                            dialogUtil.dismissWaitingDialog()
                            mStarted = !mStarted
                            refreshStartState()
                            if (mStarted) {
                                TaskUtils.postOnMain(checkLevelWork)
                            }
                        }
                    }
                    override fun onError(errorCode: Int) {
                        super.onError(errorCode)
                        onUiThread {
                            dialogUtil.dismissWaitingDialog()
                            DialogUtil.showToast(this@RoomDetailActivity, "操作失败,请重试!", false)
                        }
                    }
                })
            }
            R.id.ard_low_icon -> {
                if (mStarted) {
                    dialogUtil.showWaitingDialog(this, "正在操作...", true)
                    SocketManager.sendString("SETMB"+getChannelFromPosition(mCurrentId)+"L", object : TimeCheckSocket.AbsTimeSocketListener() {
                        override fun onSuccess(data: String?) {
                            onUiThread {
                                dialogUtil.dismissWaitingDialog()
                                refreshLevel(3)
                            }
                        }
                        override fun onError(errorCode: Int) {
                            super.onError(errorCode)
                            onUiThread {
                                dialogUtil.dismissWaitingDialog()
                                DialogUtil.showToast(this@RoomDetailActivity, "操作失败,请重试!", false)
                            }
                        }
                    })
                } else {
                    DialogUtil.showToast(this, "还未启动,请先启动", false)
                }
            }
            R.id.ard_medium_icon -> {
                if (mStarted) {
                    dialogUtil.showWaitingDialog(this, "正在操作...", true)
                    SocketManager.sendString("SETMB"+getChannelFromPosition(mCurrentId)+"M", object : TimeCheckSocket.AbsTimeSocketListener() {
                        override fun onSuccess(data: String?) {
                            onUiThread {
                                dialogUtil.dismissWaitingDialog()
                                refreshLevel(6)
                            }
                        }
                        override fun onError(errorCode: Int) {
                            super.onError(errorCode)
                            onUiThread {
                                dialogUtil.dismissWaitingDialog()
                                DialogUtil.showToast(this@RoomDetailActivity, "操作失败,请重试!", false)
                            }
                        }
                    })
                } else {
                    DialogUtil.showToast(this, "还未启动,请先启动", false)
                }
            }
            R.id.ard_high_icon -> {
                if (mStarted) {
                    dialogUtil.showWaitingDialog(this, "正在操作...", true)
                    SocketManager.sendString("SETMB"+getChannelFromPosition(mCurrentId)+"H", object : TimeCheckSocket.AbsTimeSocketListener() {
                        override fun onSuccess(data: String?) {
                            onUiThread {
                                dialogUtil.dismissWaitingDialog()
                                refreshLevel(9)
                            }
                        }
                        override fun onError(errorCode: Int) {
                            super.onError(errorCode)
                            onUiThread {
                                dialogUtil.dismissWaitingDialog()
                                DialogUtil.showToast(this@RoomDetailActivity, "操作失败,请重试!", false)
                            }
                        }
                    })
                } else {
                    DialogUtil.showToast(this, "还未启动,请先启动", false)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mStoped = false
        TaskUtils.postOnMain(checkLevelWork)
        TaskUtils.postOnMain(askEnvWork)
    }

    override fun onStop() {
        super.onStop()
        mStoped = true
        TaskUtils.removePostedWork(askEnvWork)
        TaskUtils.removePostedWork(checkLevelWork)
    }

    fun refreshLevel(level : Int) {
        when(level) {
            0 -> {
                mStarted = false
                refreshStartState()
                mLowIcon?.isEnabled = true
                mMediumIcon?.isEnabled = true
                mHighIcon?.isEnabled = true
            }
            3 -> {
                mStarted = true
                refreshStartState()
                mLowIcon?.isEnabled = false
                mMediumIcon?.isEnabled = true
                mHighIcon?.isEnabled = true
            }
            6 -> {
                mStarted = true
                refreshStartState()
                mLowIcon?.isEnabled = true
                mMediumIcon?.isEnabled = false
                mHighIcon?.isEnabled = true
            }
            9 -> {
                mStarted = true
                refreshStartState()
                mLowIcon?.isEnabled = true
                mMediumIcon?.isEnabled = true
                mHighIcon?.isEnabled = false
            }
        }
    }


    inner class AskEnvRunnable : Worker() {
        override fun work() {
            if (mStoped) return
            TaskUtils.removePostedWork(this)
            var envData = SocketManager.envData
            mTmpText?.setText(envData.tmp.toString()+"°C")
            mHdyText?.setText(envData.hdy.toString()+"%")
            mPmTipText?.setText(String.format("PM2.5  %dug/m3", envData.pm))
            TaskUtils.postOnMain(this, 10000)
        }
    }

    inner class CheckLevelRunnable : Worker() {
        override fun work() {
            if (mStoped) return
            TaskUtils.removePostedWork(this)
            SocketManager.sendString("ASKMB"+getChannelFromPosition(mCurrentId), object : TimeCheckSocket.AbsTimeSocketListener() {
                override fun onSuccess(data: String?) {
                    if (data != null && data.matches(Regex("RETMB-MB=8\\d"))) {
                        var level = (data.get(10) - '0').toInt()
                        onUiThread {
                            refreshLevel(level)
                        }
                    }
                    TaskUtils.postOnMain(this@CheckLevelRunnable, 5000)
                }

                override fun onError(errorCode: Int) {
                    super.onError(errorCode)
                    TaskUtils.postOnMain(this@CheckLevelRunnable, 5000)
                }
            })
        }
    }
}
