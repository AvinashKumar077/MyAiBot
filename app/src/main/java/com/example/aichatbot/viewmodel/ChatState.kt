package com.example.aichatbot.viewmodel

import android.graphics.Bitmap
import com.example.aichatbot.model.Chat

data class ChatState(
    val chatList:MutableList<Chat> = mutableListOf(),
    val prompt : String ="",
    val bitmap: Bitmap?=null
)
