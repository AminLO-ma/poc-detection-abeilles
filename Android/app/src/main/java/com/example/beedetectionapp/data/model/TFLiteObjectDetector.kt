package com.example.beedetectionapp.data.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.beedetectionapp.domain.model.DetectionResult
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.PriorityQueue

class TFLiteObjectDetector(
    private val context: Context,
    private val modelName: String = "yolov8n_bees_v1_full_integer_quant.tflite"
) {

    private var interpreter: Interpreter? = null

    // Configuration YOLOv8
    private val inputSize = 640
    private val confThreshold = 0.55f
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

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val tflite = interpreter ?: return emptyList()

        // 1. INPUT PROCESSING
        // Mapping UINT8

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            // CastOp supprimé
            // NormalizeOp supprimé

            .build()

        var tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. PREPARE OUTPUT BUFFER
        // Vérif. tensors sortie
        val outputTensor = tflite.getOutputTensor(0)
        val outputShape = outputTensor.shape() // Shape: [1, 5, 8400]

        // Calcul taille buffer


        val dataTypeSize = if (outputTensor.dataType() == DataType.FLOAT32) 4 else 1
        val bufferSize = outputShape[1] * outputShape[2] * dataTypeSize

        val outputBuffer = ByteBuffer.allocateDirect(bufferSize)
        outputBuffer.order(ByteOrder.nativeOrder())

        // 3. RUN INFERENCE
        tflite.run(tensorImage.buffer, outputBuffer)
        outputBuffer.rewind()

        // 4. DECODING OUTPUT
        // Format: [Batch, Channels, Anchors] (xywh + score)

        val results = ArrayList<DetectionResult>()

        // Sécurité dimensions
        if (outputShape.size < 3) return emptyList()
        val numChannels = outputShape[1] // Should be 5
        val numAnchors = outputShape[2]  // Should be 8400

        // Params quantification
        val qParams = outputTensor.quantizationParams()
        val scale = qParams.scale
        val zeroPoint = qParams.zeroPoint.toFloat()

        // Parcours des ancres
        for (i in 0 until numAnchors) {
            // Lecture valeurs
            fun getVal(channelIndex: Int, anchorIndex: Int): Float {
                // Index à plat
                val index = (channelIndex * numAnchors) + anchorIndex

                if (outputTensor.dataType() == DataType.FLOAT32) {
                    return outputBuffer.getFloat(index * 4) // Lecture Float
                } else {
                    // Lecture Byte + Dequant
                    val rawValue = outputBuffer.get(index)
                    return scale * (rawValue - zeroPoint)
                }
            }

            // 1. Filtre score
            val score = getVal(4, i) // Canal 4: score

            if (score > confThreshold) {
                // 2. Lecture coord box
                val x = getVal(0, i)
                val y = getVal(1, i)
                val w = getVal(2, i)
                val h = getVal(3, i)

                // 3. Conversion xywh -> ltrb (pixels)
                val left = (x - w / 2) * inputSize
                val top = (y - h / 2) * inputSize
                val right = (x + w / 2) * inputSize
                val bottom = (y + h / 2) * inputSize

                results.add(DetectionResult(RectF(left, top, right, bottom), score, "Abeille"))
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