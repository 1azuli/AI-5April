package com.example.engbotonnx

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ai.onnxruntime.*
import java.nio.LongBuffer
import kotlin.math.sqrt

class OnnxHelper(private val context: Context) {

    val tokenizer = Tokenizer.loadFromAssets(context, "tokenizer.json")
    private val session: OrtSession
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val embedItems: List<EmbedItem>

    val embeddedVectors: List<FloatArray>
    val embeddedResponses: List<String>

    init {
        val modelBytes = context.assets.open("model.onnx").readBytes()
        session = env.createSession(modelBytes)

        val json = context.assets.open("embed_vectors.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<EmbedItem>>() {}.type
        embedItems = Gson().fromJson(json, type)

        embeddedVectors = embedItems.map { it.embedding.toFloatArray() }
        embeddedResponses = embedItems.map { it.response }
    }

    fun getEmbedding(inputText: String): FloatArray {
        val tokens = tokenizer.encode(inputText)
        val inputIds = tokens
        val attentionMask = LongArray(inputIds.size) { 1 }
        return runModel(inputIds, attentionMask)
    }

    private fun runModel(inputIds: LongArray, attentionMask: LongArray): FloatArray {
        val shape = longArrayOf(1, inputIds.size.toLong())

        val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape)
        val attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape)

        val inputs = mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionMaskTensor
        )

        val output = session.run(inputs)
        val raw = output[0].value

        try {
            @Suppress("UNCHECKED_CAST")
            val result = raw as Array<Array<FloatArray>>
            return result[0][0]
        } catch (e: Exception) {
            throw IllegalStateException("Model output is not Float[][][] (actual: ${raw?.javaClass})", e)
        }
    }
}
