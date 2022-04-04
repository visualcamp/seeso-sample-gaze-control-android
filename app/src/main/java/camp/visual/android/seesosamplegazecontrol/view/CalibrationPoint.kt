package camp.visual.android.seesosamplegazecontrol.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class CalibrationPoint(context: Context?) : View(context) {
    private var paint: Paint? = null
    private var toDraw = true

    private val defaultColor = Color.rgb(0x00, 0xA7, 0x26)

    private var oval: RectF? = null

    private var animationPower = 0.0f
    private var centerX = 0.0f
    private var centerY = 0.0f

    companion object {
        private const val default_radius = 30.0f
    }

    init {
        paint = Paint()
        paint!!.isAntiAlias = true
        paint!!.color = defaultColor

        oval = RectF()
    }

    fun setPower(power: Float) {
        animationPower = power
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (paint != null && oval != null) {
            if (toDraw) {
                val ovalLong = default_radius
                val ovalShort = default_radius

                oval?.left = centerX - ovalLong / 2
                oval?.top = centerY - ovalShort / 2
                oval?.right = centerX + ovalLong / 2
                oval?.bottom = centerY + ovalShort / 2

                paint!!.color = Color.rgb((0xDD - (animationPower * 0x88)).toInt(), (0x88 + (animationPower * 0x77)).toInt(), 0x26)
                canvas.drawOval(oval!!, paint!!)
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        centerX = (right - left) / 2.0f
        centerY = (bottom - top) / 2.0f
    }
}