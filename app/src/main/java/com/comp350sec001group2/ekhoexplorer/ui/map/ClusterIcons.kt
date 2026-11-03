package com.comp350sec001group2.ekhoexplorer.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory

// Creates a numbered icon for grouped observations
fun createClusterIcon(
    context: Context,
    count: Int
): Icon {
    val size = 100
    val bitmap = Bitmap.createBitmap(
        size, size, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Purple circle for grouped observations
    paint.color = Color.rgb(147, 51, 234)
    canvas.drawCircle(50f, 50f, 45f, paint)

    // White border
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 5f
    canvas.drawCircle(50f, 50f, 45f, paint)

    // Number of observations
    paint.style = Paint.Style.FILL
    paint.textSize = 40f
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD

    val text = count.toString()
    val y = 50f - (paint.ascent() + paint.descent()) / 2
    canvas.drawText(text, 50f, y, paint)

    return IconFactory.getInstance(context)
        .fromBitmap(bitmap)
}
