/*
 * This file is part of a larger GPL-licensed project, but is
 * separately licensed under the MIT License.
 *
 * Copyright (c) 2025 LoadingGlue
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */


package io.kitsuri.m1rage.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat

/**
 * Universal Painter that supports ALL drawable types by falling back to bitmap rasterization.
 * Fixed version with proper error handling and vector drawable support.
 */
@Composable
fun UniversalPainter(@androidx.annotation.DrawableRes id: Int): Painter {
    val context = LocalContext.current

    return remember(id) {
        try {
            Log.d("UniversalPainter", "Loading drawable $id")

            val drawable = ResourcesCompat.getDrawable(context.resources, id, context.theme)

            if (drawable == null) {
                Log.e("UniversalPainter", "Failed to load drawable $id - drawable is null")
                return@remember createFallbackPainter()
            }

            // Ensure drawable is mutable for drawing
            val mutableDrawable = drawable.mutate()

            // Special handling for vector drawables
            if (mutableDrawable is VectorDrawable) {
                // Vector drawables need to be properly measured
                val width = if (mutableDrawable.intrinsicWidth > 0) mutableDrawable.intrinsicWidth else 96
                val height = if (mutableDrawable.intrinsicHeight > 0) mutableDrawable.intrinsicHeight else 96

                // Create bitmap with proper config for vectors
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Clear canvas to transparent
                canvas.drawColor(android.graphics.Color.TRANSPARENT)

                // Set bounds and draw
                mutableDrawable.setBounds(0, 0, width, height)
                mutableDrawable.draw(canvas)

                Log.d("UniversalPainter", "Successfully rasterized vector drawable $id (${width}x${height})")
                return@remember BitmapPainter(bitmap.asImageBitmap())
            } else {
                // Handle other drawable types (PNG, JPG, etc.)
                val width = mutableDrawable.intrinsicWidth.coerceAtLeast(96)
                val height = mutableDrawable.intrinsicHeight.coerceAtLeast(96)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Clear canvas to transparent
                canvas.drawColor(android.graphics.Color.TRANSPARENT)

                mutableDrawable.setBounds(0, 0, width, height)
                mutableDrawable.draw(canvas)

                Log.d("UniversalPainter", "Successfully rasterized drawable $id (${width}x${height})")
                return@remember BitmapPainter(bitmap.asImageBitmap())
            }

        } catch (e: Exception) {
            Log.e("UniversalPainter", "Error loading drawable $id: ${e.message}", e)
            return@remember createFallbackPainter()
        }
    }
}

/**
 * Creates a simple fallback painter when drawable loading fails
 */
private fun createFallbackPainter(): Painter {
    // Create a simple 96x96 colored bitmap as fallback
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw a simple pattern so you know it's the fallback
    canvas.drawColor(android.graphics.Color.LTGRAY)

    // Draw an X pattern to indicate missing drawable
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        strokeWidth = 4f
    }
    canvas.drawLine(0f, 0f, 96f, 96f, paint)
    canvas.drawLine(96f, 0f, 0f, 96f, paint)

    return BitmapPainter(bitmap.asImageBitmap())
}