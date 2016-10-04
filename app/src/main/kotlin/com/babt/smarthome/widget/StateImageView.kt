package com.babt.smarthome.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

/**
 * Created by cylee on 16/9/30.
 */
class StateImageView : ImageView {
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

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (enabled) {
            alpha = 1f
        } else{
            alpha = 0.6f
        }
        invalidate()
    }
}