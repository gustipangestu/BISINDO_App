package com.example.handgesture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.use

class GestureRecognizerHelper(
    val context: Context,
    val resultListener: (GestureRecognizerResult) -> Unit,
    val errorListener: (String) -> Unit
) {
    private var gestureRecognizer: GestureRecognizer? = null

    init {
        setupGestureRecognizer()
    }

    private fun setupGestureRecognizer() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath("gesture_recognizer.task")

        val optionsBuilder = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener(this::returnLivestreamError)

        try {
            gestureRecognizer = GestureRecognizer.createFromOptions(
                context,
                optionsBuilder.build()
            )
        } catch (e: Exception) {
            errorListener(e.message ?: "Error initializing gesture recognizer")
        }
    }

    fun recognizeLiveStream(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()

        // Copy the RGB bits from the ImageProxy into a Bitmap
        val bitmapBuffer = createBitmap(imageProxy.width, imageProxy.height)

        // Note: This requires the ImageAnalysis to use OUTPUT_IMAGE_FORMAT_RGBA_8888
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

        // Handle Rotation
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
        )

        // Convert to MPImage
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(result: GestureRecognizerResult, input: MPImage) {
        resultListener(result)
    }

    private fun returnLivestreamError(error: RuntimeException) {
        errorListener(error.message ?: "Unknown errorr")
    }
}