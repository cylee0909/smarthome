package com.babt.smarthome.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Created by cylee on 16/9/22.
 */
class StateLinearLayout : LinearLayout {
    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        if (pressed) {
            alpha = 0.6f
        } else{
            alpha = 1f
        }
        invalidate()
    }
}