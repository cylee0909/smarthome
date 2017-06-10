package com.babt.smarthome

import android.os.Bundle
import com.baidu.android.common.util.DeviceId
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.cylee.androidlib.base.BaseActivity
import com.cylee.androidlib.thread.Worker
import com.cylee.androidlib.util.PreferenceUtils
import com.cylee.androidlib.util.TaskUtils
import io.fabric.sdk.android.Fabric

/**
 * Created by cylee on 16/9/20.
 */
class SplashActivity : BaseActivity() {
    var startWork = object: Worker() {
        override fun work() {
            if (AppConfig.debugLocal || PreferenceUtils.getBoolean(HomePreference.NET_INITED)) {
                startActivity(MainActivity.createIntent(this@SplashActivity))
            } else {
                startActivity(NetSetActivity.createIntent(this@SplashActivity))
            }
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PreferenceUtils.getBoolean(HomePreference.NET_INITED)) {
            Fabric.with(this, Crashlytics())
            Fabric.with(this, Answers())
            Crashlytics.setUserIdentifier(DeviceId.getDeviceID(this))
            Crashlytics.setUserName(PreferenceUtils.getString(HomePreference.NET_LOGIN_NAME))
            Crashlytics.setString("address", PreferenceUtils.getString(HomePreference.NET_LOGIN_ADDRESS))
        }
        setContentView(R.layout.activity_splash)
        TaskUtils.postOnMain(startWork, 2000)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        TaskUtils.removePostedWork(startWork)
    }
}