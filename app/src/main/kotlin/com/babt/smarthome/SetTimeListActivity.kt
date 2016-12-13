package com.babt.smarthome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import cn.csnbgsh.herbarium.bind
import com.babt.smarthome.entity.TimeSet
import com.cylee.androidlib.util.PreferenceUtils
import org.jetbrains.anko.find
import java.util.*

/**
 * Created by cylee on 16/10/9.
 */
class SetTimeListActivity : AppBaseActivity() {
    companion object {
        val INPUT_ROOM_ID = "INPUT_ROOM_ID"
        fun createIntent(context : Context, roomId: Int): Intent {
            var intent = Intent(context, SetTimeListActivity::class.java)
            intent.putExtra(INPUT_ROOM_ID, roomId)
            return intent
        }
    }

    var mList : ListView? = null
    var mAdapter : InnerAdapter? = null
    var times : MutableList<TimeSet.TimeItem>? = null
    var mTimeSet : TimeSet? = null
    var timeChanged = false
    var roomId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_time_list)

        roomId = intent.getIntExtra(INPUT_ROOM_ID, 0)
        mList = bind(R.id.astl_list)

        find<View>(R.id.astl_exit).setOnClickListener {
            view -> onBackPressed()
        }
        mAdapter = InnerAdapter()
        mList?.adapter = mAdapter
    }

    override fun onStart() {
        super.onStart()
        mTimeSet = PreferenceUtils.getObject(HomePreference.TIMES, TimeSet::class.java)
        times = mTimeSet?.timeMap?.get(roomId) ?: null
        processTimes(times)
        mAdapter?.notifyDataSetChanged()
    }

    fun processTimes(times : MutableList<TimeSet.TimeItem>?) {
        if (times != null) {
            times.forEach {
                it.nextDate = HomeUtil.getNextDateByWeek(it.week, it.time)
            }
            Collections.sort(times, {
                a, b ->
                return@sort (a.createTime - b.createTime).toInt()
            })
        }
    }

    override fun onStop() {
        super.onStop()
        if (timeChanged) {
            PreferenceUtils.setObject(HomePreference.TIMES, mTimeSet)
        }
    }

    inner class InnerAdapter : BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            if (position == 0) {
                var addView = View.inflate(this@SetTimeListActivity, R.layout.set_time_add, null)
                addView.setOnClickListener {
                    startActivity(AddTimeActivity.createIntent(this@SetTimeListActivity, roomId))
                }
                addView?.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, resources.getDimension(R.dimen.list_item_height).toInt())
                return addView
            }
            var returnView : View? = null
            var holder : Holder? = null
            if (convertView == null || convertView.tag == null) {
                returnView = View.inflate(this@SetTimeListActivity, R.layout.time_set_item, null)
                holder = Holder()
                holder.topText = returnView.bind(R.id.tsi_top_text)
                holder.bottomText = returnView.bind(R.id.tsi_bottom_text)
                holder.delText = returnView.bind(R.id.tsi_del_text)
                returnView.setTag(holder)
            } else{
                returnView = convertView
                holder = convertView.tag as Holder
            }
            var timeItem = times?.get(position -1)
            if (timeItem != null) {
                holder.topText?.setText(position.toString()+
                        ". 星期"+HomeUtil.getAllWeek(timeItem.week)+", "+
                        (if (timeItem.action == 0) "打开" else "关闭") +
                        (if (timeItem.repeat == 0) "重复执行" else "执行一次")
                )

                holder.bottomText?.setText(timeItem.nextDate+" "+timeItem.time)
                holder.delText?.setOnClickListener {
                    times?.remove(timeItem)
                    mAdapter?.notifyDataSetChanged()
                    timeChanged = true
                }
            }
            returnView?.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, resources.getDimension(R.dimen.list_item_height).toInt())
            return returnView
        }

        override fun getItem(position: Int): Any? {
            return times?.get(position -1) ?: null
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return (times?.size ?: 0) + 1
        }
    }

    class Holder {
        var topText : TextView? = null
        var bottomText : TextView? = null
        var delText : TextView? = null
    }
}