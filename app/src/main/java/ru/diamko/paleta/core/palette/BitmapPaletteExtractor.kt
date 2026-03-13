package ru.diamko.paleta.core.palette

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.random.Random

object BitmapPaletteExtractor {
    fun extractFromBitmap(
        bitmap: Bitmap,
        colorCount: Int,
    ): List<String> {
        val samples = samplePixels(bitmap)
        if (samples.isEmpty()) return emptyList()

        val k = colorCount.coerceIn(3, 15).coerceAtMost(samples.size)
        val result = kMeans(samples, k)

        val colors = result.sortedByDescending { it.count }
            .map { cluster ->
                Color.rgb(
                    cluster.r.toInt().coerceIn(0, 255),
                    cluster.g.toInt().coerceIn(0, 255),
                    cluster.b.toInt().coerceIn(0, 255),
                )
            }
            .map(ColorTools::colorIntToHex)
            .distinct()

        return HexColors.normalize(colors)?.take(k) ?: colors.take(k)
    }

    private data class PixelRgb(
        val r: Float,
        val g: Float,
        val b: Float,
    )

    private data class Cluster(
        val r: Float,
        val g: Float,
        val b: Float,
        val count: Int,
    )

    private fun samplePixels(bitmap: Bitmap): List<PixelRgb> {
        val maxSamples = 9_000
        val w = bitmap.width
        val h = bitmap.height
        val stride = maxOf(1, kotlin.math.sqrt((w * h / maxSamples).toDouble()).toInt())
        val pixels = ArrayList<PixelRgb>(maxSamples)

        val rowBuffer = IntArray(w)
        var y = 0
        while (y < h) {
            bitmap.getPixels(rowBuffer, 0, w, 0, y, w, 1)
            var x = 0
            while (x < w) {
                val color = rowBuffer[x]
                val alpha = Color.alpha(color)
                if (alpha >= 32) {
                    val r = Color.red(color).toFloat()
                    val g = Color.green(color).toFloat()
                    val b = Color.blue(color).toFloat()
                    if (!isNearGray(r, g, b)) {
                        pixels += PixelRgb(r, g, b)
                    }
                }
                x += stride
            }
            y += stride
        }

        if (pixels.isNotEmpty()) return pixels

        // Fallback if image is almost grayscale or transparent.
        return buildList {
            val fallbackStride = maxOf(1, stride / 2)
            var fy = 0
            while (fy < h) {
                bitmap.getPixels(rowBuffer, 0, w, 0, fy, w, 1)
                var fx = 0
                while (fx < w) {
                    val c = rowBuffer[fx]
                    if (Color.alpha(c) >= 32) {
                        add(
                            PixelRgb(
                                r = Color.red(c).toFloat(),
                                g = Color.green(c).toFloat(),
                                b = Color.blue(c).toFloat(),
                            ),
                        )
                    }
                    fx += fallbackStride
                }
                fy += fallbackStride
            }
        }
    }

    private fun isNearGray(r: Float, g: Float, b: Float): Boolean {
        return abs(r - g) < 8f && abs(g - b) < 8f && abs(r - b) < 8f
    }

    private fun kMeans(samples: List<PixelRgb>, k: Int): List<Cluster> {
        val random = Random.Default
        val centroids = MutableList(k) {
            val p = samples[random.nextInt(samples.size)]
            floatArrayOf(p.r, p.g, p.b)
        }

        val assignments = IntArray(samples.size)

        repeat(12) {
            // assign
            for (i in samples.indices) {
                val p = samples[i]
                var bestIdx = 0
                var bestDist = distanceSq(p, centroids[0])
                for (cIdx in 1 until k) {
                    val dist = distanceSq(p, centroids[cIdx])
                    if (dist < bestDist) {
                        bestDist = dist
                        bestIdx = cIdx
                    }
                }
                assignments[i] = bestIdx
            }

            // recompute
            val sumR = FloatArray(k)
            val sumG = FloatArray(k)
            val sumB = FloatArray(k)
            val count = IntArray(k)
            for (i in samples.indices) {
                val cIdx = assignments[i]
                val p = samples[i]
                sumR[cIdx] += p.r
                sumG[cIdx] += p.g
                sumB[cIdx] += p.b
                count[cIdx]++
            }

            for (cIdx in 0 until k) {
                if (count[cIdx] == 0) {
                    val p = samples[random.nextInt(samples.size)]
                    centroids[cIdx][0] = p.r
                    centroids[cIdx][1] = p.g
                    centroids[cIdx][2] = p.b
                } else {
                    centroids[cIdx][0] = sumR[cIdx] / count[cIdx]
                    centroids[cIdx][1] = sumG[cIdx] / count[cIdx]
                    centroids[cIdx][2] = sumB[cIdx] / count[cIdx]
                }
            }
        }

        val finalCount = IntArray(k)
        for (idx in assignments) {
            finalCount[idx]++
        }

        return buildList {
            for (i in 0 until k) {
                add(
                    Cluster(
                        r = centroids[i][0],
                        g = centroids[i][1],
                        b = centroids[i][2],
                        count = finalCount[i],
                    ),
                )
            }
        }
    }

    private fun distanceSq(p: PixelRgb, centroid: FloatArray): Float {
        val dr = p.r - centroid[0]
        val dg = p.g - centroid[1]
        val db = p.b - centroid[2]
        return dr * dr + dg * dg + db * db
    }
}
