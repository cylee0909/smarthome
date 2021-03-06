package com.babt.smarthome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import android.widget.TextView
import cn.csnbgsh.herbarium.bind
import com.cylee.lib.widget.dialog.DialogUtil
import com.cylee.socket.TimeCheckSocket

/**
 * Created by cylee on 16/12/8.
 */
class SensorNetSetActivity : AppBaseActivity() {
    var wifiName: EditText? = null
    var wifiPasswd: EditText? = null
    var confirm: TextView? = null

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SensorNetSetActivity::class.java);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_init_net)
        wifiName = bind(R.id.ain_wifi_name)
        wifiPasswd = bind(R.id.ain_wifi_passwd)
        confirm = bind(R.id.ain_confirm_text)
        confirm?.setOnClickListener {
            if (!TextUtils.isEmpty(wifiName?.text.toString())) {
                dialogUtil.showWaitingDialog(this, "正在连接...")
                connect()
            } else {
                DialogUtil.showToast(this, "参数填写错误", false)
            }
        }
    }

    fun connect() {
        SocketManager.mAddressSocket?.sendString("SETNM" + wifiName?.text.toString(), object : TimeCheckSocket.AbsTimeSocketListener() {
            override fun onSuccess(data: String?) {
                super.onSuccess(data)
                SocketManager.mAddressSocket?.sendString("SETPW" + wifiPasswd?.text.toString(), object : TimeCheckSocket.AbsTimeSocketListener() {
                    override fun onSuccess(data: String?) {
                        super.onSuccess(data)
                        SocketManager.mAddressSocket?.sendString("Save_", object : TimeCheckSocket.AbsTimeSocketListener() {
                            override fun onSuccess(data: String?) {
                                super.onSuccess(data)
                                dialogUtil.dismissWaitingDialog()
                                DialogUtil.showToast(this@SensorNetSetActivity, "设置成功", false)
                                finish()
                            }

                            override fun onError(errorCode: Int) {
                                super.onError(errorCode)
                                dialogUtil.dismissWaitingDialog()
                                DialogUtil.showToast(this@SensorNetSetActivity, "连接失败,请稍后重试", false)
                            }
                        })
                    }

                    override fun onError(errorCode: Int) {
                        super.onError(errorCode)
                        dialogUtil.dismissWaitingDialog()
                        DialogUtil.showToast(this@SensorNetSetActivity, "连接失败,请稍后重试", false)
                    }
                })
            }

            override fun onError(errorCode: Int) {
                super.onError(errorCode)
                dialogUtil.dismissWaitingDialog()
                DialogUtil.showToast(this@SensorNetSetActivity, "连接失败,请稍后重试", false)
            }
        })
    }
}