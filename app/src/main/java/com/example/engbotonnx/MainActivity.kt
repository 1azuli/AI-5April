package com.example.engbotonnx

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var onnxHelper: OnnxHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewChat)
        val editTextQuestion = findViewById<EditText>(R.id.editTextQuestion)
        val buttonSend = findViewById<Button>(R.id.buttonSend)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        onnxHelper = OnnxHelper(this)

        buttonSend.setOnClickListener {
            val userText = editTextQuestion.text.toString()
            if (userText.isNotBlank()) {
                messages.add(ChatMessage(userText, isUser = true))
                val botResponse = getBestResponse(userText)
                messages.add(ChatMessage(botResponse, isUser = false))
                adapter.notifyItemRangeInserted(messages.size - 2, 2)
                recyclerView.scrollToPosition(messages.size - 1)
                editTextQuestion.text.clear()
            }
        }
    }

    private fun getBestResponse(userText: String): String {
        val inputEmbedding = onnxHelper.getEmbedding(userText).map { it.toDouble() }
        val similarities = mutableListOf<Pair<Int, Double>>()

        for ((index, responseEmbedding) in onnxHelper.embeddedVectors.withIndex()) {
            val responseEmbeddingDoubles = responseEmbedding.map { it.toDouble() }

            val dotProduct = inputEmbedding.zip(responseEmbeddingDoubles) { a, b -> a * b }.sum()
            val magnitudeA = sqrt(inputEmbedding.map { it * it }.sum())
            val magnitudeB = sqrt(responseEmbeddingDoubles.map { it * it }.sum())

            if (magnitudeA != 0.0 && magnitudeB != 0.0) {
                val similarity = dotProduct / (magnitudeA * magnitudeB)
                similarities.add(index to similarity)
            }
        }

        val topN = 3
        val topResponses = similarities.sortedByDescending { it.second }.take(topN)

        topResponses.forEach { Log.d("TOP_SIMILAR", "Index: ${it.first}, Sim: ${it.second}") }

        val randomIndex = topResponses.random().first
        return onnxHelper.embeddedResponses[randomIndex]
    }

}
