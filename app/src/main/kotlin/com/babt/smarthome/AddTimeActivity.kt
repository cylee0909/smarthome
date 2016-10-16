package com.babt.smarthome

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import android.widget.TimePicker
import cn.csnbgsh.herbarium.bind
import com.babt.smarthome.entity.TimeSet
import com.cylee.androidlib.base.BaseActivity
import com.cylee.androidlib.util.PreferenceUtils
import com.cylee.lib.widget.dialog.DialogUtil
import org.jetbrains.anko.find
import java.util.*

/**
 * Created by cylee on 16/10/15.
 */
class AddTimeActivity : BaseActivity() , View.OnClickListener{
    companion object {
        val INPUT_ROOM_ID = "INPUT_ROOM_ID"
        fun createIntent(context : Context, roomId: Int): Intent {
            var intent = Intent(context, AddTimeActivity::class.java)
            intent.putExtra(INPUT_ROOM_ID, roomId)
            return intent
        }
    }
    val date = arrayListOf("星期一", "星期二","星期三","星期四","星期五","星期六","星期日")
    val repeats = arrayListOf("反复", "一次")
    val actions = arrayListOf("打开", "关闭")
    var dateText : TextView? = null
    var timeText : TextView? = null
    var repeatText : TextView? = null
    var actionText : TextView? = null
    var confirmText : TextView? = null
    var weekPosition = 0
    var setTime = ""
    var repeat = -1
    var action = -1
    var roomId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_time)
        find<View>(R.id.aat_exit).setOnClickListener {
            view -> onBackPressed()
        }

        roomId = intent.getIntExtra(INPUT_ROOM_ID, 0)

        dateText = bind(R.id.aat_date_text)
        timeText = bind(R.id.aat_time_text)
        repeatText = bind(R.id.aat_repeat_text)
        actionText = bind(R.id.aat_action_text)
        confirmText = bind(R.id.aat_confirm_text)

        dateText?.setOnClickListener(this)
        timeText?.setOnClickListener(this)
        repeatText?.setOnClickListener(this)
        actionText?.setOnClickListener(this)
        confirmText?.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.aat_date_text -> {
                weekPosition = 0
                DialogUtil().showMultiListDialog(this, "选择日期", null, "确定", object : DialogUtil.ButtonClickListener {
                    override fun OnLeftButtonClick() {
                    }

                    override fun OnRightButtonClick() {
                        var weeks = getWeeks()
                        if (TextUtils.isEmpty(weeks)) {
                            weeks = "选择日期"
                        }
                        dateText?.setText(weeks)
                    }

                }, date, object : DialogUtil.MultiListItemClickListener {
                    override fun onItemClick(position: Int, checked: Boolean) {
                        if (checked) {
                            weekPosition = weekPosition or (1 shl position)
                        } else{
                            weekPosition = weekPosition and ((1 shl position).inv())
                        }
                    }
                }, null, null)
            }
            R.id.aat_time_text -> {
                var c = Calendar.getInstance()
                var dialog = TimePickerDialog(
                        this,
                        object : TimePickerDialog.OnTimeSetListener {
                            override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                                setTime = hourOfDay.toString() + ":"+minute.toString()
                                timeText?.text = setTime
                            }
                        },
                        c.get(Calendar.HOUR_OF_DAY),
                        c.get(Calendar.MINUTE),
                        true)
                dialog.show()
            }
            R.id.aat_repeat_text -> {
                DialogUtil().showListDialog(this, "重复次数", null, null, null, repeats, object :DialogUtil.ListItemClickListener {
                    override fun onItemClick(position: Int) {
                        repeat = position
                        repeatText?.text = repeats[position]
                    }
                }, null, null)
            }
            R.id.aat_action_text -> {
                DialogUtil().showListDialog(this, "设置动作", null, null, null, actions, object :DialogUtil.ListItemClickListener {
                    override fun onItemClick(position: Int) {
                        action = position
                        actionText?.text = actions[position]
                    }
                }, null, null)
            }
            R.id.aat_confirm_text -> {
                if (weekPosition == 0) {
                    DialogUtil.showToast(this, "没有选择日期!", false)
                    return
                }
                if (TextUtils.isEmpty(setTime)) {
                    DialogUtil.showToast(this, "没有选择时间!", false)
                    return
                }
                if (action == -1) {
                    DialogUtil.showToast(this, "没有选择动作!", false)
                    return
                }
                if (repeat == -1) {
                    DialogUtil.showToast(this, "没有选择重复!", false)
                    return
                }
                var timeItem = TimeSet.TimeItem()
                timeItem.action = action
                timeItem.repeat = repeat
                timeItem.time = setTime
                timeItem.createTime = System.currentTimeMillis()
                timeItem.week = weekPosition
                var timeSet = PreferenceUtils.getObject(HomePreference.TIMES, TimeSet::class.java)
                if (timeSet == null) {
                    timeSet = TimeSet()
                }
                var timeList = timeSet.timeMap.get(roomId)
                if (timeList == null) {
                    timeList = arrayListOf()
                    timeSet.timeMap.put(roomId, timeList)
                }
                timeList.add(timeItem)
                PreferenceUtils.setObject(HomePreference.TIMES, timeSet)
                DialogUtil.showToast(this, "新增成功", false)
                finish()
            }
        }
    }

    fun getWeeks() : String {
        var result = ""
        for (i in 0 .. date.size) {
            if (weekPosition and (1 shl i) > 0) {
                result += date[i] + ","
            }
        }
        if (result.endsWith(",")) {
            result = result.substring(0, result.length - 1)
        }
        return result
    }
}