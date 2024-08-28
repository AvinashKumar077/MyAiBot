package com.example.aichatbot.view

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.aichatbot.viewmodel.ChatUIEvent
import com.example.aichatbot.viewmodel.ChatViewModel
import com.example.aichatbot.R
import com.example.aichatbot.ui.theme.AIChatBotTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.round

class MainActivity : ComponentActivity() {

    private val uriState = MutableStateFlow("")

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("com.example.aichatbot.view.MainActivity", "Image URI selected: $it")
            uriState.value = it.toString() // Update to string URI
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIChatBotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary)
                                    .height(100.dp)
                                    .padding( horizontal = 16.dp)
                            ) {
                                Text(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    text = "My Ai ChatBot",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    ) {
                        ChatScreen(paddingValues = it)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun ChatScreen(paddingValues: PaddingValues) {
        val chatViewModel = viewModel<ChatViewModel>()
        val chatState = chatViewModel.chatState.collectAsState().value

        // Get the bitmap, which may or may not be null
        val bitmap = getBitmap()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            verticalArrangement = Arrangement.Bottom
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                reverseLayout = true
            ) {
                itemsIndexed(chatState.chatList) { index, chat ->
                    if (chat.isFromUser) {
                        UserChatItem(
                            prompt = chat.prompt,
                            bitmap = chat.bitmap
                        )
                    } else {
                        ModelChatItem(
                            response = chat.prompt
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (bitmap != null) {
                        Image(
                            modifier = Modifier
                                .size(50.dp)
                                .padding(bottom = 2.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentDescription = "picked image",
                            contentScale = ContentScale.Crop,
                            bitmap = bitmap.asImageBitmap()
                        )
                    }
                    Icon(
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                imagePicker.launch(
                                    PickVisualMediaRequest
                                        .Builder()
                                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        .build()
                                )
                            },
                        painter = painterResource(id = R.drawable.gallery),
                        contentDescription = "Add Photo",
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    value = chatState.prompt,
                    onValueChange = {
                        chatViewModel.onEvent(ChatUIEvent.UpdatePrompt(it))
                    },
                    placeholder = {
                        Text(text = "Enter your prompt")
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent, // Hide the underline when focused
                        unfocusedIndicatorColor = Color.Transparent // Hide the underline when unfocused
                    )
                )


                Icon(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(30.dp)
                        .clickable {
                            chatViewModel.onEvent(ChatUIEvent.SendPrompt(chatState.prompt, bitmap))
                            uriState.update { "" }
                        },
                    painter = painterResource(id = R.drawable.send),
                    contentDescription = "Send Prompt",
                )
            }
        }
    }

    @Composable
    fun UserChatItem(prompt: String, bitmap: Bitmap?) {
        Column(
            modifier = Modifier.padding(start = 100.dp, bottom = 16.dp)
        ) {
            bitmap?.let {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentDescription = "image",
                    contentScale = ContentScale.Crop,
                    bitmap = it.asImageBitmap()
                )
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp),
                text = prompt,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun ModelChatItem(response: String) {
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        var showToast by remember { mutableStateOf(false) }
        var toast: Toast? = null

        Column(
            modifier = Modifier
                .padding(end = 100.dp, bottom = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            // Copy text to clipboard
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Copied Text", response))
                            showToast = true

                            // Trigger a short vibration
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                // For Android 12 (API level 31) and above
                                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                val vibrator = vibratorManager.defaultVibrator
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(
                                        100, VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            } else {
                                // For older Android versions
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(
                                        100, VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            }

                            // Show toast
                            toast?.cancel()
                            toast = Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).also {
                                it.show()
                            }
                        }
                    )
                }
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(16.dp),
                text = response,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onTertiary,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    }



    @Composable
    private fun getBitmap(): Bitmap? {
        val uri = uriState.collectAsState().value

        if (uri.isEmpty()) {
            return null
        }

        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(uri)) // Ensure URI is parsed correctly
                .size(Size.ORIGINAL)
                .build()
        )

        when (val imageState = painter.state) {
            is AsyncImagePainter.State.Success -> {
                return imageState.result.drawable.toBitmap()
            }
            is AsyncImagePainter.State.Error -> {
                Log.e("getBitmap", "Error loading image: ${imageState.result.throwable}")
            }
            else -> {
                Log.d("getBitmap", "Image load state: $imageState, URI: $uri")
            }
        }

        return null
    }
}
