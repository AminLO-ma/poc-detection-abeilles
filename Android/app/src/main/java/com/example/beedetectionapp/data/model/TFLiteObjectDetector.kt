package com.example.beedetectionapp.data.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.beedetectionapp.domain.model.DetectionResult
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.PriorityQueue
import kotlin.math.min

class TFLiteObjectDetector(
    private val context: Context,
    private val modelName: String = "yolov8n_bees_v1_full_integer_quant.tflite"
) {

    private var interpreter: Interpreter? = null

    // Configuration YOLOv8
    private val inputSize = 640
    private val confThreshold = 0.20f
    private val iouThreshold = 0.5f

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        try {
            val options = Interpreter.Options()
            options.numThreads = 4 // 4 threads CPU
            interpreter = Interpreter(loadModelFile(), options)
            Log.d("TFLite", "Interpreter initialized successfully")
        } catch (e: Exception) {
            Log.e("TFLite", "Error initializing interpreter: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    /* fun detect(bitmap: Bitmap): List<DetectionResult> {
        val tflite = interpreter ?: return emptyList()

        val size = min(bitmap.width, bitmap.height)

        // 1. INPUT PROCESSING
        // Mapping UINT8

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(size, size)) // Garde le ratio (carré central)
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f)) // Rend float 0..1
            .add(QuantizeOp(-128f, 0.0039215686f)) // Convertit en INT8 [-128, 127]
            .add(CastOp(DataType.UINT8)) // Cast final pour le buffer
            .build()

        var tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. PREPARE OUTPUT
        val outputTensor = tflite.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val dataTypeSize = if (outputTensor.dataType() == DataType.FLOAT32) 4 else 1
        val bufferSize = outputShape[1] * outputShape[2] * dataTypeSize
        val outputBuffer = ByteBuffer.allocateDirect(bufferSize)
        outputBuffer.order(ByteOrder.nativeOrder())

        // 3. RUN INFERENCE
        tflite.run(tensorImage.buffer, outputBuffer)
        outputBuffer.rewind()

        // 4. DECODING
        val results = ArrayList<DetectionResult>()
        if (outputShape.size < 3) return emptyList()
        val numAnchors = outputShape[2]
        val qParams = outputTensor.quantizationParams()
        val scale = qParams.scale
        val zeroPoint = qParams.zeroPoint.toFloat()

        var maxScoreInFrame = 0f

        // Parcours des ancres
        for (i in 0 until numAnchors) {
            fun getVal(channelIndex: Int, anchorIndex: Int): Float {
                val index = (channelIndex * numAnchors) + anchorIndex
                if (outputTensor.dataType() == DataType.FLOAT32) {
                    return outputBuffer.getFloat(index * 4)
                } else {
                    val rawValue = outputBuffer.get(index)
                    return scale * (rawValue - zeroPoint)
                }
            }

            // 1. Filtre score
            val score = getVal(4, i) // Canal 4: score

            if (score > maxScoreInFrame) maxScoreInFrame = score

            if (score > confThreshold) {
                // 2. Lecture coord box
                val x = getVal(0, i)
                val y = getVal(1, i)
                val w = getVal(2, i)
                val h = getVal(3, i)

                if (score == maxScoreInFrame) {
                    Log.d("ML_COORDS", "Score: $score | X: $x, Y: $y, W: $w, H: $h")
                }

                val left: Float
                val top: Float
                val right: Float
                val bottom: Float

                if (x > 5.0f || h > 5.0f) {
                    left = (x - w / 2)
                    top = (y - h / 2)
                    right = (x + w / 2)
                    bottom = (y + h / 2)
                } else {
                    left = (x - w / 2) * inputSize
                    top = (y - h / 2) * inputSize
                    right = (x + w / 2) * inputSize
                    bottom = (y + h / 2) * inputSize
                }


                results.add(DetectionResult(RectF(left, top, right, bottom), score, "Abeille"))
            }
        }
        Log.d("ML_DEBUG", "Meilleur score vu : ${maxScoreInFrame * 100}% (Seuil: ${confThreshold * 100}%)")
        return applyNMS(results)
    }*/

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val tflite = interpreter ?: return emptyList()

        // 1. INPUT : ON ÉCRASE TOUT (Stretch)
        // On supprime le "ResizeWithCropOrPadOp". On veut que TOUTE l'image rentre.
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .add(QuantizeOp(-128f, 0.0039215686f))
            .add(CastOp(DataType.UINT8))
            .build()

        var tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. OUTPUT
        val outputTensor = tflite.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val outputScale = outputTensor.quantizationParams().scale
        val outputZeroPoint = outputTensor.quantizationParams().zeroPoint.toFloat()

        val bufferSize = outputShape[1] * outputShape[2] * 1 // UINT8
        val outputBuffer = ByteBuffer.allocateDirect(bufferSize)
        outputBuffer.order(ByteOrder.nativeOrder())

        tflite.run(tensorImage.buffer, outputBuffer)
        outputBuffer.rewind()

        val results = ArrayList<DetectionResult>()
        val numAnchors = outputShape[2]

        for (i in 0 until numAnchors) {
            fun getVal(channelIndex: Int, anchorIndex: Int): Float {
                val index = (channelIndex * numAnchors) + anchorIndex
                val rawValue = outputBuffer.get(index)
                return outputScale * (rawValue - outputZeroPoint)
            }

            val score = getVal(4, i)

            if (score > confThreshold) {
                // Lecture brute (Pixels dans le référentiel 640x640)
                val x = getVal(0, i)
                val y = getVal(1, i)
                val w = getVal(2, i)
                val h = getVal(3, i)

                // 3. NORMALISATION ABSOLUE (0..1)

                // Note : Parfois YOLO sort déjà du 0..1. Le test :
                val finalX = if (x > 10) x / inputSize else x
                val finalY = if (y > 10) y / inputSize else y
                val finalW = if (w > 10) w / inputSize else w
                val finalH = if (h > 10) h / inputSize else h

                val left = finalX - finalW / 2
                val top = finalY - finalH / 2
                val right = finalX + finalW / 2
                val bottom = finalY + finalH / 2

                // On renvoie des coordonnées entre 0.0 et 1.0
                results.add(
                    DetectionResult(
                        boundingBox = RectF(left, top, right, bottom),
                        score = score,
                        label = "Abeille"
                    )
                )
            }
        }

        return applyNMS(results)
    }



    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        val parsedList = ArrayList<DetectionResult>()
        val pq = PriorityQueue<DetectionResult> { o1, o2 -> o2.score.compareTo(o1.score) }
        pq.addAll(detections)
        while (pq.isNotEmpty()) {
            val best = pq.poll() ?: continue
            parsedList.add(best)
            val iterator = pq.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (calculateIoU(best.boundingBox, other.boundingBox) > iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return parsedList
    }

    private fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val intersectLeft = maxOf(boxA.left, boxB.left)
        val intersectTop = maxOf(boxA.top, boxB.top)
        val intersectRight = minOf(boxA.right, boxB.right)
        val intersectBottom = minOf(boxA.bottom, boxB.bottom)
        if (intersectRight < intersectLeft || intersectBottom < intersectTop) return 0f
        val intersectionArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val boxAArea = (boxA.right - boxA.left) * (boxA.bottom - boxA.top)
        val boxBArea = (boxB.right - boxB.left) * (boxB.bottom - boxB.top)
        return intersectionArea / (boxAArea + boxBArea - intersectionArea)
    }
}