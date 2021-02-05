package com.zlxrx.library.inputview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.EditText

@SuppressLint("AppCompatCustomView")
class InputView : EditText {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var touchDownIcon: Icon? = null
    private val headerIcons = ArrayList<Icon>()
    private val footerIcons = ArrayList<Icon>()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var newPaddingStart = 0
        var headerX = 0F
        headerIcons.forEach {
            val drawable = it.drawable ?: return@forEach
            val x = headerX + it.marginStart
            val y = (measuredHeight - drawable.intrinsicHeight) / 2F
            it.bounds.set(x, y, x + drawable.intrinsicWidth, y + drawable.intrinsicHeight)
            headerX = x + drawable.intrinsicWidth + it.marginEnd
            newPaddingStart += drawable.intrinsicWidth + it.marginStart + it.marginEnd
        }
        if (newPaddingStart == 0) {
            newPaddingStart = paddingStart
        }

        var newPaddingEnd = 0
        var footerX = measuredWidth.toFloat()
        footerIcons.forEach {
            val drawable = it.drawable ?: return@forEach
            val x = footerX - it.marginEnd - drawable.intrinsicWidth
            val y = (measuredHeight - drawable.intrinsicHeight) / 2F
            it.bounds.set(x, y, x + drawable.intrinsicWidth, y + drawable.intrinsicHeight)
            footerX = x - it.marginStart
            newPaddingEnd += drawable.intrinsicWidth + it.marginStart + it.marginEnd
        }
        if (newPaddingEnd == 0) {
            newPaddingEnd = paddingEnd
        }

        setPadding(newPaddingStart, paddingTop, newPaddingEnd, paddingBottom)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) {
            return
        }

        headerIcons.forEach {
            drawIcon(canvas, it)
        }

        footerIcons.forEach {
            drawIcon(canvas, it)
        }
    }

    private fun drawIcon(canvas: Canvas, icon: Icon) {
        if (!icon.isVisible) {
            return
        }
        val drawable = icon.drawable ?: return
        if (drawable is StateListDrawable) {
            drawable.state = icon.state
        }
        drawable.setBounds(
            icon.bounds.left.toInt() + scrollX,
            icon.bounds.top.toInt() + scrollY,
            icon.bounds.right.toInt() + scrollX,
            icon.bounds.bottom.toInt() + scrollY
        )
        drawable.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                headerIcons.forEach {
                    if (event.x >= it.bounds.left - it.marginStart &&
                        event.x <= it.bounds.right + it.marginEnd
                    ) {
                        touchDownIcon = it
                    }
                }
                footerIcons.forEach {
                    if (event.x >= it.bounds.left - it.marginStart &&
                        event.x <= it.bounds.right + it.marginEnd
                    ) {
                        touchDownIcon = it
                    }
                }
                touchDownIcon?.setPressed(true)
            }
            MotionEvent.ACTION_UP -> {
                touchDownIcon?.let {
                    it.setPressed(false)
                    if (event.x >= it.bounds.left - it.marginStart &&
                        event.x <= it.bounds.right + it.marginEnd
                    ) {
                        it.onClick?.invoke(it, text.toString())
                        touchDownIcon = null
                        return true
                    }
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_OUTSIDE -> touchDownIcon?.setPressed(false)
            else -> {
                // NOOP
            }
        }
        return super.onTouchEvent(event)
    }

    fun addHeaderIcon(
        resId: Int,
        marginStart: Int = 0,
        marginEnd: Int = 0,
        onClick: ((Icon, String) -> Unit)? = null
    ) {
        headerIcons.add(Icon(resId, marginStart, marginEnd, onClick))
    }

    fun addFooterIcon(
        resId: Int,
        marginStart: Int = 0,
        marginEnd: Int = 0,
        onClick: ((Icon, String) -> Unit)? = null
    ) {
        footerIcons.add(Icon(resId, marginStart, marginEnd, onClick))
    }

    fun addClearIcon(
        resId: Int,
        marginStart: Int = 0,
        marginEnd: Int = 0,
        hideOnInputEmpty: Boolean = true,
        onCleared: (() -> Unit)? = null
    ) {
        val clearIcon = Icon(resId, marginStart, marginEnd) { _, _ ->
            setText("")
            onCleared?.invoke()
        }
        footerIcons.add(clearIcon)

        if (hideOnInputEmpty) {
            if (text.isEmpty()) {
                clearIcon.isVisible = false
            }
            doOnTextChanged { text, _, _, _ ->
                clearIcon.isVisible = !text.isNullOrEmpty()
                postInvalidate()
            }
        }
    }

    fun addPasswordIcon(
        resId: Int,
        marginStart: Int = 0,
        marginEnd: Int = 0,
        onVisibleChanged: ((visible: Boolean) -> Unit)? = null
    ) {
        if (transformationMethod == null) {
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        footerIcons.add(Icon(resId, marginStart, marginEnd) { icon, _ ->
            val isPasswordVisible = transformationMethod !is PasswordTransformationMethod
            transformationMethod = if (isPasswordVisible) {
                // 隐藏密码
                icon.setSelected(false)
                PasswordTransformationMethod.getInstance()
            } else {
                // 显示密码
                icon.setSelected(true)
                null
            }
            setSelection(text.length)
            onVisibleChanged?.invoke(transformationMethod !is PasswordTransformationMethod)
        })
    }

    inner class Icon(
        resId: Int,
        val marginStart: Int = 0,
        val marginEnd: Int = 0,
        val onClick: ((Icon, String) -> Unit)? = null
    ) {

        var isVisible = true
        val bounds = RectF()
        val drawable: Drawable? = context.getDrawable(resId)

        private var states = ArrayList<Int>()
        val state: IntArray
            get() = states.toIntArray()

        fun setPressed(pressed: Boolean) {
            if (pressed) {
                if (isPressed()) {
                    return
                }
                states.add(DRAWABLE_STATE_PRESSED)
            } else {
                if (!isPressed()) {
                    return
                }
                states.remove(DRAWABLE_STATE_PRESSED)
            }
            postInvalidate()
        }

        private fun isPressed(): Boolean {
            return states.contains(DRAWABLE_STATE_PRESSED)
        }

        fun setSelected(selected: Boolean) {
            if (selected) {
                if (isSelected()) {
                    return
                }
                states.add(DRAWABLE_STATE_SELECTED)
            } else {
                if (!isSelected()) {
                    return
                }
                states.remove(DRAWABLE_STATE_SELECTED)
            }
            postInvalidate()
        }

        fun isSelected(): Boolean {
            return states.contains(DRAWABLE_STATE_SELECTED)
        }
    }

    companion object {
        private const val DRAWABLE_STATE_PRESSED = android.R.attr.state_pressed
        private const val DRAWABLE_STATE_SELECTED = android.R.attr.state_selected
    }
}