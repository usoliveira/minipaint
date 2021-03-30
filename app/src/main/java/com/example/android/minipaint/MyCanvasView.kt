package com.example.android.minipaint

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat

private const val STROKE_WIDTH  = 12f   // has to be float

class MyCanvasView(context: Context) : View(context) {

    // Cache objects
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    // Caches the x-y coordinates of the current touch event
    private var motionTouchEventX = 0f;
    private var motionTouchEventY = 0f;

    // Caches the x-y coordinates of the starting point for the next path
    // (where user stops moving and lifts their touch)
    private var currentX = 0f;
    private var currentY = 0f;

    // Background & Draw colors
    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)
    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    // Touch tolerance
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    // Canvas frame
    private lateinit var frame: Rect

    // Set up the paint with which to draw
    private val paint = Paint().apply {
        color = drawColor                   // line color
        isAntiAlias = true                  // smooths out edges without affecting shape
        isDither = true                     // down-sample (ex: 256 colors) higher-precision images
        style = Paint.Style.STROKE          // default: FILL (specifies if the primitive being drawn is "filled", "stroked", or "both")
        strokeJoin = Paint.Join.ROUND       // default: MITER (specifies how lines and curve segments join a stroked path)
        strokeCap = Paint.Cap.ROUND         // default: BUTT (sets the shape of the end of the line to be a cap)
        strokeWidth = STROKE_WIDTH          // default: Hairline-width
    }

    // Stores the path that is being drawn
    private var path = Path()

    /**
     * Callback method called by the Android system with the changed screen dimensions,
     * that is, with a new width and height (to change to) and the old width and height
     * (to change from).
     *
     * @param width New width size
     * @param height New height size
     * @param oldWidth Old width size
     * @param oldHeight Old height size
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        // A new bitmap and canvas are created every time this function is executed.
        // You need a new bitmap because the size has changed!
        // "Recycling" prevents problems like "Memory Leak" leaving old bitmaps around.
        if (::extraBitmap.isInitialized) extraBitmap.recycle()

        // Width & Height here are the current screen size.
        // The ARGB_888 is a bitmap color configuration witch stores each color in 4 bytes
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)

        // Calculate a rectangular frame around the picture.
        val inset = 40
        frame = Rect(inset, inset, width - inset, height - inset)
    }

    /**
     * Paints the bitmap on current system Canvas
     *
     * @param canvas The current system Canvas
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the current bitmap
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)

        // Draw a frame around the canvas
        canvas.drawRect(frame, paint)
    }

    /**
     * Responds to motion on the display
     *
     * @param event The event itself
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }

        return true
    }

    /**
     * Resets the path, and move the coordinates
     */
    private fun touchStart() {
        path.reset()                                        // reset the path
        path.moveTo(motionTouchEventX, motionTouchEventY)   // move x-y coordinates
        currentX = motionTouchEventX                        // last X (cache)
        currentY = motionTouchEventY                        // last Y (cache)
    }

    /**
     * Calculate the traveled distance "dx" & "dy", create a curve between the two points
     * and store it in path, update the running current x-y tally and draw the path.
     */
    private fun touchMove() {
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1, y1), and ending at (x2, y2).
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY

            // Draw the path in the extra bitmap to cache it.
            extraCanvas.drawPath(path, paint)
        }
        invalidate()    // force redraw the view
    }

    /**
     * Reset the path so it doesn't get draw again.
     * (rewind the current path for the next touch)
     */
    private fun touchUp() {
        path.reset()
    }
}