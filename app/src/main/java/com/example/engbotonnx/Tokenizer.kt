package com.example.engbotonnx

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

class Tokenizer(private val vocab: Map<String, Long>) {

    private val clsToken = "[CLS]"
    private val sepToken = "[SEP]"

    fun encode(text: String): LongArray {
        val tokens = mutableListOf<Long>()

        // เพิ่ม [CLS] token ถ้ามีใน vocab
        vocab[clsToken]?.let { tokens.add(it) }

        // แยกคำโดยใช้ regex ที่ตัดคำและสัญลักษณ์
        val words = text.trim()
            .split(Regex("\\s+|(?=\\p{Punct})|(?<=\\p{Punct})"))
            .map { it.lowercase() }

        for (word in words) {
            val tokenId = vocab[word] ?: 0L // ใช้ 0L แทน unknown token
            tokens.add(tokenId)
        }

        // เพิ่ม [SEP] token ถ้ามีใน vocab
        vocab[sepToken]?.let { tokens.add(it) }

        return tokens.toLongArray()
    }

    companion object {
        fun loadFromAssets(context: Context, filename: String): Tokenizer {
            val assetManager = context.assets
            val inputStream = assetManager.open(filename)
            val reader = InputStreamReader(inputStream, "UTF-8")
            val gson = Gson()
            val json = gson.fromJson(reader, Map::class.java)
            val model = json["model"] as Map<*, *>
            val vocabRaw = model["vocab"] as Map<*, *>

            // แปลง key เป็น lowercase เพื่อไม่ให้ case-sensitive
            val vocab = vocabRaw.mapKeys { it.key.toString().lowercase() }
                .mapValues { (it.value as Number).toLong() }

            return Tokenizer(vocab)
        }
    }
}
