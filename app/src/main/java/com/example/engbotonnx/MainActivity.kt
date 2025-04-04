package com.example.engbotonnx

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var editTextQuestion: EditText
    private lateinit var buttonSend: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewChat)
        editTextQuestion = findViewById(R.id.editTextQuestion)
        buttonSend = findViewById(R.id.buttonSend)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        buttonSend.setOnClickListener {
            val userText = editTextQuestion.text.toString().trim()
            if (userText.isNotEmpty()) {
                addMessage(ChatMessage(userText, isUser = true))
                sendToApi(userText)
                editTextQuestion.text.clear()
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun sendToApi(userText: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ChatApi::class.java)
        val request = MessageRequest(userText)

        api.sendMessage(request).enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val reply = response.body()!!.response
                    addMessage(ChatMessage(reply, isUser = false))
                } else {
                    addMessage(ChatMessage("เกิดข้อผิดพลาดจากเซิร์ฟเวอร์", isUser = false))
                }
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                addMessage(ChatMessage("ไม่สามารถเชื่อมต่อ API ได้: ${t.message}", isUser = false))
            }
        })
    }
}

// -------- Retrofit API ด้านล่าง --------
data class MessageRequest(val message: String)
data class MessageResponse(val response: String, val similarity: Float)

interface ChatApi {
    @Headers("Content-Type: application/json")
    @POST("/engbot")
    fun sendMessage(@Body request: MessageRequest): Call<MessageResponse>
}
