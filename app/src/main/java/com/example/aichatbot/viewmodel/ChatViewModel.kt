package com.example.aichatbot.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichatbot.model.Chat
import com.example.aichatbot.model.ChatData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel: ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState = _chatState.asStateFlow()

    fun onEvent(event: ChatUIEvent) {
        when(event) {
            is ChatUIEvent.SendPrompt -> {
                if(event.prompt.isNotEmpty()) {
                    addPrompt(event.prompt, event.bitmap)

                    if(event.bitmap != null) {
                        getResponseWithImage(event.prompt, event.bitmap)
                    } else {
                        getResponse(event.prompt)
                    }
                }
            }
            is ChatUIEvent.UpdatePrompt -> {
                _chatState.update {
                    it.copy(
                        prompt = event.newPrompt
                    )
                }
            }
        }
    }


    private fun addPrompt(prompt: String, bitmap: Bitmap?) {
        _chatState.update {
            it.copy(
                chatList = it.chatList.toMutableList().apply {
                    add(0, Chat(prompt, bitmap, true)) // Ensure the user prompt is added at position 0
                },
                prompt = "", // Reset the prompt after adding
                bitmap = null // Reset bitmap after adding
            )
        }
        Log.d("ChatViewModel", "Prompt added: $prompt")

    }


    private fun getResponse(prompt: String) {
        viewModelScope.launch {
            val chat = ChatData.getRespoonse(prompt)
            _chatState.update {
                it.copy(
                    chatList = it.chatList.toMutableList().apply {
                        add(0, chat) // Adding the response at position 0
                    }
                )
            }
            Log.d("ChatViewModel", "Response received: ${chat.prompt}")

        }

    }

    private fun getResponseWithImage(prompt: String, bitmap: Bitmap) {
        viewModelScope.launch {
            val chat = ChatData.getRespoonsewithImage(prompt, bitmap)
            _chatState.update {
                it.copy(
                    chatList = it.chatList.toMutableList().apply {
                        add(0, chat) // Adding the response at position 0
                    }
                )
            }
            Log.d("ChatViewModel", "Response received: ${chat.prompt}")

        }
    }

}