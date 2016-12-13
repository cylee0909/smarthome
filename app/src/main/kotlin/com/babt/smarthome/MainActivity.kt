package com.babt.smarthome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import cn.csnbgsh.herbarium.bind
import com.android.volley.toolbox.StringRequest
import com.cylee.androidlib.base.BaseActivity
import com.cylee.androidlib.net.Net
import com.cylee.androidlib.util.DateUtils
import com.cylee.androidlib.util.SimpleLunarCalendar
import com.cylee.lib.widget.dialog.DialogUtil
import org.jetbrains.anko.find
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppBaseActivity() {
    companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyy年MM月dd日")
        fun createIntent(context : Context):Intent {
            return Intent(context, MainActivity::class.java);
        }
    }

    var mDateText : TextView? = null
    var mDateCnText : TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        find<LinearLayout>(R.id.air_clean_container).setOnClickListener {
           v -> startActivity(AirCleanActivity.createIntent(this))
        }
        find<View>(R.id.am_exit).setOnClickListener {
            view -> onBackPressed()
        }

        mDateText = bind(R.id.am_date)
        mDateCnText = bind(R.id.am_date_cn)
        var date = Date()
        mDateCnText?.setText("农历"+SimpleLunarCalendar(date).noYearDate+"    "+DateUtils.getWeekOfDate(date))
        mDateText?.setText(DATE_FORMAT.format(date))

        find<View>(R.id.am_people_img).setOnLongClickListener {
            startActivity(NetSetActivity.createIntent(this@MainActivity))
            return@setOnLongClickListener true
        }
    }



    override fun onResume() {
        super.onResume()
        if (!SocketManager.isInitSuccess()) {
            retrySocket()
        }
    }

    fun retrySocket() {
        DialogUtil().showDialog(this, "警告", "退出", "重试", object : DialogUtil.ButtonClickListener {
            override fun OnLeftButtonClick() {
                System.exit(-1)
            }

            override fun OnRightButtonClick() {
                SocketManager.retry(object : SocketManager.InitListener {
                    override fun onInitSuccess() {
//                        DialogUtil.showToast(this@MainActivity, "设备连接成功", false)
                    }

                    override fun onInitFail() {
                        retrySocket()
                    }
                })
            }
        }, "设备连接失败,当前功能无法正常使用,是否重试?")
    }
}
