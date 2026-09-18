package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.ui.VesperaViewModel
import com.example.ui.avatar.VesperaAvatarView
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AiBubble
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.ImagePreviewBg
import com.example.ui.theme.ImagePreviewText
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosBlueActive
import com.example.ui.theme.ModalInputBg
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextHint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TopBarBg
import com.example.ui.theme.TopBarBorder
import com.example.ui.theme.TopBarTitle
import com.example.ui.theme.UserBubble
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesperaChatScreen(
    viewModel: VesperaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val audioAmplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val voicePersona by viewModel.voicePersona.collectAsStateWithLifecycle()
    val dailyVideoCount by viewModel.dailyVideoCount.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Fast auto-scroll to bottom on message updates
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Modern photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            viewModel.onImageSelected(uri, context)
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // iOS Glassmorphism Top Bar (#top-bar)
        IosTopBar(
            onOpenSettings = { showSettingsModal = true },
            onClearChat = { showClearConfirmDialog = true }
        )

        // Error Banner
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { err ->
                Surface(
                    color = AccentPink.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPink.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3D Scene Area (#canvas-container)
        VesperaAvatarView(
            isSpeaking = isSpeaking,
            audioAmplitude = audioAmplitude,
            isVoiceEnabled = isVoiceEnabled,
            onToggleVoice = { viewModel.toggleVoice() },
            onAvatarTap = { viewModel.onAvatarTapped() },
            modifier = Modifier.weight(1f)
        )

        // iOS Dark Water Chat Container (#chat-container)
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp),
            shadowElevation = 15.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                // Messages List (#messages)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .weight(1f, fill = false)
                        .testTag("messages_list"),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageItem(
                            message = message,
                            onSpeakAgain = { viewModel.speakMessage(message.text) },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Hinata", message.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (isLoading) {
                        item {
                            HinataTypingIndicator()
                        }
                    }
                }

                // Quick Prompt Chips
                QuickPromptChips(
                    onSelectPrompt = { prompt ->
                        viewModel.sendMessage(prompt)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Attached Image Preview Bar (#img-preview-bar)
                if (selectedImageUri != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(ImagePreviewBg, RoundedCornerShape(10.dp))
                            .border(1.dp, IosBlue, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📌 Photo Attached (Ready to Send)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ImagePreviewText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "✖",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ImagePreviewText,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier
                                .clickable { viewModel.clearAttachedImage() }
                                .padding(4.dp)
                        )
                    }
                }

                // Input Controls (#controls)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Plus button (.plus-btn)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach image",
                            tint = IosBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // User text input (input[type="text"])
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Ask Hinata or generate video...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0x66FFFFFF),
                                    fontSize = 14.sp
                                )
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0x0FFFFFFF),
                            unfocusedContainerColor = Color(0x0FFFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = IosBlue,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("userInput"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() || selectedImageUri != null) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        )
                    )

                    // Send Button (.btn-send)
                    Button(
                        onClick = {
                            if (inputText.isNotBlank() || selectedImageUri != null) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IosBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("send_button")
                    ) {
                        Text(
                            text = "Send",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Settings Modal (#settings-modal)
    if (showSettingsModal) {
        IosSettingsModal(
            currentApiKey = customApiKey,
            currentLanguage = selectedLanguage,
            currentPersona = voicePersona,
            dailyVideoCount = dailyVideoCount,
            onDismiss = { showSettingsModal = false },
            onSave = { apiKey, lang, persona ->
                viewModel.updateSettings(
                    language = lang,
                    persona = persona,
                    apiKey = apiKey
                )
                showSettingsModal = false
            }
        )
    }

    // Clear Chat Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Chat?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Kya aap Hinata ke saath chat history reset karna chahte hain?", color = TextSecondary) },
            containerColor = Color(0xFF1C1C1E),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPink)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = IosBlue)
                }
            }
        )
    }
}

/**
 * Top Bar matching HTML #top-bar with iOS Glassmorphism
 */
@Composable
private fun IosTopBar(
    onOpenSettings: () -> Unit,
    onClearChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TopBarBg)
            .border(width = 1.dp, color = TopBarBorder)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Title: Hinata AI v1.3
        Text(
            text = "Hinata AI v1.3",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // iOS button (.ios-btn with "⚙️ Settings")
            Surface(
                color = Color(0x14FFFFFF),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .clickable { onOpenSettings() }
                    .testTag("settings_button")
            ) {
                Text(
                    text = "⚙️ Settings",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = IosBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Clear chat icon button
            IconButton(
                onClick = onClearChat,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x0FFFFFFF))
                    .testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Conversation",
                    tint = Color(0xB3FFFFFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Video player for generated video responses
 */
@Composable
private fun VideoMessagePlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(videoUrl))
                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                }
            }
        )
    }
}

/**
 * Chat Message Item with iOS Dark Water Bubble styling
 */
@Composable
private fun ChatMessageItem(
    message: ChatMessageEntity,
    onSpeakAgain: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.sender == "user"
    val hasVideo = message.text.contains("[VIDEO:")
    val videoUrl = if (hasVideo) {
        message.text.substringAfter("[VIDEO:").substringBefore("]")
    } else null
    val cleanText = if (hasVideo) {
        message.text.substringBefore("[VIDEO:").trim()
    } else {
        message.text
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = if (hasVideo) 320.dp else 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) IosBlue else Color(0x10FFFFFF))
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = if (isUser) Color.Transparent else Color(0x0DFFFFFF),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Column {
                if (!message.imageBase64.isNullOrBlank()) {
                    val bitmap = remember(message.imageBase64) {
                        try {
                            val decoded = Base64.decode(message.imageBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Attached photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .padding(bottom = 6.dp)
                        )
                    }
                }

                val displayText = if (isUser) {
                    cleanText
                } else {
                    val stripped = cleanText.removePrefix("⚠️").trim()
                    if (stripped.startsWith("Hinata:") || stripped.startsWith("Sakura:") || stripped.startsWith("Tsunade:")) {
                        stripped
                    } else if (cleanText.startsWith("Hinata:") || cleanText.startsWith("Sakura:") || cleanText.startsWith("Tsunade:")) {
                        cleanText
                    } else {
                        "Hinata: $cleanText"
                    }
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isUser) Color.White else Color(0xE6FFFFFF),
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                )

                // Render video if present
                if (hasVideo && !videoUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hinata Rendered Video:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = IosBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    VideoMessagePlayer(videoUrl = videoUrl)
                }

                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onSpeakAgain,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen again",
                                tint = IosBlue.copy(alpha = 0.9f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = Color(0x99FFFFFF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated typing indicator
 */
@Composable
private fun HinataTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "TypingDots")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x10FFFFFF))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Hinata is thinking…",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xB3FFFFFF), fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(dotScale)
                .clip(CircleShape)
                .background(IosBlue)
        )
    }
}

/**
 * Quick prompt suggestions
 */
@Composable
private fun QuickPromptChips(onSelectPrompt: (String) -> Unit) {
    val prompts = listOf(
        "M-Main Hinata hoon... 🌸",
        "Aap kaise ho?",
        "Ek pyara sa joke sunao! 😂",
        "Kuch sweet bolo na ✨",
        "Mujhse baat karo"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(prompts) { prompt ->
            Surface(
                color = Color(0x0FFFFFFF),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF)),
                modifier = Modifier.clickable { onSelectPrompt(prompt) }
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xE6FFFFFF),
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * iOS Fullscreen Settings Modal (#settings-modal) matching Hinata AI v1.3
 */
@Composable
private fun IosSettingsModal(
    currentApiKey: String,
    currentLanguage: String,
    currentPersona: String,
    dailyVideoCount: Int,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, language: String, persona: String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf(currentLanguage) }
    var selectedPersona by remember { mutableStateOf(currentPersona) }

    val languages = listOf("Hinglish", "Hindi", "English")
    val personas = listOf(
        Triple("hinata", "Hinata", "Shy, Soft & High Pitch"),
        Triple("sakura", "Sakura", "Energetic & Bold"),
        Triple("tsunade", "Tsunade", "Mature & Deep")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF010101),
        modifier = Modifier.fillMaxWidth(0.96f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings & Customization",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                // iOS Done button
                Surface(
                    color = Color(0x0FFFFFFF),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable {
                        onSave(apiKeyInput, selectedLang, selectedPersona)
                    }
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = IosBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Setting Item: GEMINI API KEY
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GEMINI API KEY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0x99FFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (isKeyVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall.copy(color = IosBlue),
                            modifier = Modifier.clickable { isKeyVisible = !isKeyVisible }
                        )
                    }
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = {
                            Text("Paste Gemini API Key", color = Color(0x66FFFFFF), fontSize = 14.sp)
                        },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = IosBlue,
                            unfocusedBorderColor = Color(0x14FFFFFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apiKey")
                    )
                }

                // Setting Item: AI GIRL VOICE CHARACTER
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI GIRL VOICE CHARACTER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0x99FFFFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        personas.forEach { (key, name, desc) ->
                            val isSelected = selectedPersona.equals(key, ignoreCase = true)
                            Surface(
                                color = if (isSelected) IosBlue.copy(alpha = 0.25f) else Color(0x0AFFFFFF),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) IosBlue else Color(0x14FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPersona = key }
                                    .testTag("persona_$key")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isSelected) Color.White else Color(0xCCFFFFFF),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isSelected) IosBlueActive else Color(0x88FFFFFF),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = IosBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Setting Item: RESPONSE LANGUAGE
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "RESPONSE LANGUAGE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0x99FFFFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { lang ->
                            val isSelected = selectedLang.equals(lang, ignoreCase = true)

                            Surface(
                                color = if (isSelected) IosBlue else Color(0x0AFFFFFF),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) IosBlue else Color(0x14FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLang = lang }
                            ) {
                                Text(
                                    text = lang,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) Color.White else Color(0xCCFFFFFF),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Setting Item: DAILY VIDEO LIMIT TRACKER
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DAILY VIDEO LIMIT TRACKER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0x99FFFFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Surface(
                        color = Color(0x0AFFFFFF),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("videoCountDisplay")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$dailyVideoCount / 5 Videos Used Today",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (dailyVideoCount >= 5) AccentPink else IosBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = if (dailyVideoCount >= 5) "Limit Reached" else "${5 - dailyVideoCount} left",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0x88FFFFFF),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKeyInput, selectedLang, selectedPersona)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IosBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_settings_button")
            ) {
                Text(
                    text = "Save Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    )
}
