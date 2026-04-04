package io.github.sj42tech.route42.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.datamatrix.DataMatrixWriter

object DataMatrixBitmapFactory {
    fun create(
        contents: String,
        sizePx: Int = 1_024,
    ): Bitmap {
        require(contents.isNotBlank()) { "Share code contents cannot be empty" }
        val hints = mapOf(
            EncodeHintType.MARGIN to 4,
        )
        val matrix = DataMatrixWriter().encode(
            contents,
            BarcodeFormat.DATA_MATRIX,
            sizePx,
            sizePx,
            hints,
        )
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            val x = index % matrix.width
            val y = index / matrix.width
            if (matrix[x, y]) Color.BLACK else Color.WHITE
        }

        return Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
            setPixels(
                pixels,
                0,
                matrix.width,
                0,
                0,
                matrix.width,
                matrix.height,
            )
        }
    }
}
