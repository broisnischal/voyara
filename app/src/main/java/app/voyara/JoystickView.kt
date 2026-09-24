package app.voyara

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Floating joystick drawn over other apps. The pill at the top drags it around. */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class JoystickView(context: Context, private val onStick: (Float, Float) -> Unit) : View(context) {
    var onDrag: (Int, Int) -> Unit = { _, _ -> }

    private val dp = resources.displayMetrics.density
    private val radius = 60 * dp
    private val knobRadius = 24 * dp
    private val handle = 24 * dp
    private val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x80101828.toInt() }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2 * dp
        color = 0x66FFFFFF
    }
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFFFFF.toInt() }
    private val grip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt() }
    private val gripBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x80101828.toInt() }

    private var knobX = 0f
    private var knobY = 0f
    private var dragging = false
    private var lastX = 0f
    private var lastY = 0f

    init {
        contentDescription = "Joystick. Drag to walk, drag the top handle to move it."
    }

    private val cx get() = width / 2f
    private val cy get() = handle + (height - handle) / 2f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (2 * radius + 8 * dp).toInt()
        setMeasuredDimension(size, size + handle.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(cx - 26 * dp, 2 * dp, cx + 26 * dp, 18 * dp, 8 * dp, 8 * dp, gripBg)
        canvas.drawRoundRect(cx - 14 * dp, 8.5f * dp, cx + 14 * dp, 11.5f * dp, 2 * dp, 2 * dp, grip)
        canvas.drawCircle(cx, cy, radius, base)
        canvas.drawCircle(cx, cy, radius, ring)
        canvas.drawCircle(cx + knobX, cy + knobY, knobRadius, knob)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = e.y < handle
                lastX = e.rawX
                lastY = e.rawY
                if (!dragging) setKnob(e.x - cx, e.y - cy)
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                val dx = (e.rawX - lastX).toInt()
                val dy = (e.rawY - lastY).toInt()
                if (dx != 0 || dy != 0) {
                    lastX += dx
                    lastY += dy
                    onDrag(dx, dy)
                }
            } else {
                setKnob(e.x - cx, e.y - cy)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (!dragging) setKnob(0f, 0f)
        }
        return true
    }

    private fun setKnob(x: Float, y: Float) {
        val reach = radius - knobRadius
        val scale = min(1f, reach / max(hypot(x, y), 0.001f))
        knobX = x * scale
        knobY = y * scale
        invalidate()
        onStick(knobX / reach, knobY / reach)
    }
}
