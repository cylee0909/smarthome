package com.babt.smarthome

import com.cylee.androidlib.base.BaseActivity
import com.cylee.androidlib.util.PreferenceUtils
import com.cylee.lib.widget.dialog.DialogUtil

/**
 * Created by cylee on 16/12/13.
 */
open class AppBaseActivity : BaseActivity() {
    override fun onResume() {
        super.onResume()
        var lastTime = PreferenceUtils.getLong(HomePreference.CHANGE_FILTER_TIP_TIME)
        if (lastTime - System.currentTimeMillis() > 90 * 24 * 60 * 60 * 1000) { // 90天更换滤芯提醒
            dialogUtil.showDialog(this, "", "确定", object : DialogUtil.ButtonClickListener {
                override fun OnLeftButtonClick() {
                }

                override fun OnRightButtonClick() {
                    PreferenceUtils.setLong(HomePreference.CHANGE_FILTER_TIP_TIME, System.currentTimeMillis())
                }
            }, "滤芯已经工作90工作日，建议更换滤芯，以获得好的过滤效果")
        }
    }
}