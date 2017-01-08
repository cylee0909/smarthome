package com.babt.smarthome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import android.widget.TextView
import cn.csnbgsh.herbarium.bind
import com.babt.smarthome.util.EncryptUtil
import com.cylee.androidlib.util.PreferenceUtils
import com.cylee.lib.widget.dialog.DialogUtil
import com.cylee.socket.TimeCheckSocket

/**
 * Created by cylee on 16/12/8.
 */
class NetSetActivity : AppBaseActivity() {
    var wifiName: EditText? = null
    var wifiPasswd: EditText? = null
    var loginName : EditText? = null
    var loginPassd : EditText? = null
    var address : EditText? = null
    var confirm: TextView? = null

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, NetSetActivity::class.java);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init_net)
        wifiName = bind(R.id.ain_wifi_name)
        wifiPasswd = bind(R.id.ain_wifi_passwd)
        confirm = bind(R.id.ain_confirm_text)
        loginName = bind(R.id.ain_login_name)
        loginPassd = bind(R.id.ain_login_passwd)
        address = bind(R.id.ain_address)
        confirm?.setOnClickListener {
            if (TextUtils.isEmpty(loginName?.text.toString())) {
                DialogUtil.showToast(this, "登陆用户名未填写", false)
                return@setOnClickListener
            }

            if (loginName?.length() ?: 0 != 11) {
                DialogUtil.showToast(this, "不是手机号", false)
                return@setOnClickListener
            }

            if (!TextUtils.isEmpty(wifiName?.text.toString())) {
                dialogUtil.showWaitingDialog(this, "正在连接...")
                connect()
            } else {
                DialogUtil.showToast(this, "参数填写错误", false)
            }
        }
    }

    fun connect() {
        SocketManager.mAddressSocket?.sendString("WIFN_" + wifiName?.text.toString(), object : TimeCheckSocket.AbsTimeSocketListener() {
            override fun onSuccess(data: String?) {
                super.onSuccess(data)
                SocketManager.mAddressSocket?.sendString("WIFP_" + wifiPasswd?.text.toString(), object : TimeCheckSocket.AbsTimeSocketListener() {
                    override fun onSuccess(data: String?) {
                        super.onSuccess(data)
                        SocketManager.mAddressSocket?.sendString("Save_", object : TimeCheckSocket.AbsTimeSocketListener() {
                            override fun onSuccess(data: String?) {
                                super.onSuccess(data)
                                PreferenceUtils.setBoolean(HomePreference.NET_INITED, true)
                                dialogUtil.dismissWaitingDialog()
                                dialogUtil.showDialog(this@NetSetActivity, "设置成功", "", "确认", object : DialogUtil.ButtonClickListener {
                                    override fun OnLeftButtonClick() {
                                    }

                                    override fun OnRightButtonClick() {
                                        PreferenceUtils.commitString(HomePreference.NET_LOGIN_ADDRESS, address?.text.toString())
                                        PreferenceUtils.commitString(HomePreference.NET_LOGIN_NAME, loginName?.text.toString())
                                        PreferenceUtils.commitString(HomePreference.NET_LOGIN_PASSWD, EncryptUtil.getVerify(loginPassd?.text.toString()))
                                        System.exit(0)
                                    }
                                }, "设备网络设置成功,将此设备的网络切换至" + wifiName?.text.toString() + "后重新打开app")
                            }

                            override fun onError(errorCode: Int) {
                                super.onError(errorCode)
                                dialogUtil.dismissWaitingDialog()
                                DialogUtil.showToast(this@NetSetActivity, "连接失败,请稍后重试", false)
                            }
                        })
                    }

                    override fun onError(errorCode: Int) {
                        super.onError(errorCode)
                        dialogUtil.dismissWaitingDialog()
                        DialogUtil.showToast(this@NetSetActivity, "连接失败,请稍后重试", false)
                    }
                })
            }

            override fun onError(errorCode: Int) {
                super.onError(errorCode)
                dialogUtil.dismissWaitingDialog()
                DialogUtil.showToast(this@NetSetActivity, "连接失败,请稍后重试", false)
            }
        })
    }
}