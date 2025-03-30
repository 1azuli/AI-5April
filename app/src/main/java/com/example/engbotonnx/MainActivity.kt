package com.example.engbotonnx

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var onnxHelper: OnnxHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewChat) // ← แก้ตรงนี้
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
        val inputEmbedding = onnxHelper.getEmbedding(userText).toList()
        var bestSimilarity = -1.0
        var bestResponse = "🤖 ฉันไม่เข้าใจคำถามนี้"

        for ((index, responseEmbedding) in onnxHelper.embeddedVectors.withIndex()) {
            val dotProduct = inputEmbedding.zip(responseEmbedding.toList()) { a, b ->
                a.toDouble() * b.toDouble()
            }.sum()

            val magnitudeA = inputEmbedding.sumOf { it.toDouble() * it.toDouble() }
            val magnitudeB = responseEmbedding.sumOf { it.toDouble() * it.toDouble() }

            if (magnitudeA != 0.0 && magnitudeB != 0.0) {
                val similarity = dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB))
                Log.d("SIMILARITY", "[$index] $similarity")

                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity
                    bestResponse = onnxHelper.embeddedResponses[index]
                }
            }
        }

        return bestResponse
    }
}
