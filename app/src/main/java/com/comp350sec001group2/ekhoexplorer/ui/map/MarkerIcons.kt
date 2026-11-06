package com.comp350sec001group2.ekhoexplorer.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import com.comp350sec001group2.ekhoexplorer.R

// Creates a colored map marker with a white icon
fun createObservationIcon(
    context: Context,
    category: ObservationCategory
): Icon {
    val density = context.resources.displayMetrics.density
    val size = (48 * density).toInt()
    val iconSize = (26 * density).toInt()
    val bitmap = Bitmap.createBitmap(
        size,
        size,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    // Choose the marker color and drawable
    val backgroundColor: Int
    val drawableId: Int

    when (category) {
        ObservationCategory.PLANT -> {
            backgroundColor = Color.rgb(46, 158, 79)
            drawableId = R.drawable.ic_plant_marker
        }

        ObservationCategory.ANIMAL -> {
            backgroundColor = Color.rgb(50, 121, 200)
            drawableId = R.drawable.ic_animal_marker
        }

        ObservationCategory.INSECT -> {
            backgroundColor = Color.rgb(180, 124, 50)
            drawableId = R.drawable.ic_insect_marker
        }

        ObservationCategory.FUNGI -> {
            backgroundColor = Color.rgb(139, 99, 173)
            drawableId = R.drawable.ic_fungi_marker
        }
    }
    // Draw the colored circle
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(
        size / 2f,
        size / 2f,
        size / 2f,
        paint
    )
    // Draw the white leaf or paw in the center
    val drawable = ContextCompat.getDrawable(context, drawableId)
    drawable?.setBounds(
        (size - iconSize) / 2,
        (size - iconSize) / 2,
        (size + iconSize) / 2,
        (size + iconSize) / 2
    )

    drawable?.draw(canvas)
    // Convert the bitmap into a MapLibre marker icon
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}
