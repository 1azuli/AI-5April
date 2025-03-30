package com.example.engbotonnx

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ai.onnxruntime.*
import java.io.InputStreamReader
import kotlin.math.sqrt

class OnnxHelper(private val context: Context) {

    private val env: OrtEnvironment
    private val tokenizer: Tokenizer
    private val session: OrtSession
    val embeddedResponses: List<String>
    val embeddedVectors = mutableListOf<FloatArray>()

    init {
        env = OrtEnvironment.getEnvironment()
        val modelBytes = context.assets.open("model.onnx").readBytes()
        session = env.createSession(modelBytes)

        tokenizer = Tokenizer.loadFromAssets(context, "tokenizer.json")
        embeddedResponses = loadJsonList("embed_responses.json")
        loadVectorsFromChunks("vector_chunks.json")
    }

    private fun loadVectorsFromChunks(indexFile: String) {
        val chunkFiles = loadJsonList(indexFile)
        for (file in chunkFiles) {
            val stream = context.assets.open(file)
            val reader = InputStreamReader(stream)
            val vectors: List<List<Float>> = Gson().fromJson(
                reader, object : TypeToken<List<List<Float>>>() {}.type
            )
            embeddedVectors.addAll(vectors.map { it.toFloatArray() })
        }
    }

    private fun loadJsonList(fileName: String): List<String> {
        val stream = context.assets.open(fileName)
        val reader = InputStreamReader(stream)
        return Gson().fromJson(reader, object : TypeToken<List<String>>() {}.type)
    }

    fun getEmbedding(text: String): FloatArray {
        val tokenIds = tokenizer.encode(text)
        val attentionMask = LongArray(tokenIds.size) { 1L }

        val inputs = mapOf(
            "input_ids" to OnnxTensor.createTensor(env, arrayOf(tokenIds)),
            "attention_mask" to OnnxTensor.createTensor(env, arrayOf(attentionMask))
        )

        val results = session.run(inputs)
        val output = results[0].value as Array<FloatArray>
        results.close()

        return normalize(output[0])
    }

    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        return if (norm != 0f) vector.map { it / norm }.toFloatArray() else vector
    }
}
