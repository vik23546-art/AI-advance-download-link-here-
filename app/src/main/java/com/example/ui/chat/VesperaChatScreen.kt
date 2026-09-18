package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
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
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceSpeed by viewModel.voiceSpeed.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on message updates
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
        // Top Bar (#top-bar)
        VesperaTopBar(
            onOpenSettings = { showSettingsModal = true },
            onClearChat = { showClearConfirmDialog = true }
        )

        // Error Banner
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { err ->
                Surface(
                    color = AccentPink.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPink),
                    shape = RoundedCornerShape(8.dp),
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

        // Canvas / 3D Avatar View Container (#canvas-container)
        VesperaAvatarView(
            isSpeaking = isSpeaking,
            audioAmplitude = audioAmplitude,
            isVoiceEnabled = isVoiceEnabled,
            onToggleVoice = { viewModel.toggleVoice() },
            onAvatarTap = { viewModel.onAvatarTapped() },
            modifier = Modifier.height(230.dp)
        )

        // Chat Container (#chat-container)
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Messages List (#messages)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
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
                                val clip = ClipData.newPlainText("Vespera", message.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (isLoading) {
                        item {
                            VesperaTypingIndicator()
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

                // Attached Image Preview Bar
                if (selectedImageUri != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .background(CardSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderViolet, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📷 Image attached for Vespera",
                            style = MaterialTheme.typography.bodySmall.copy(color = SoftLavender),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearAttachedImage() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = SoftLavender
                            )
                        }
                    }
                }

                // Controls Row (#controls)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Image / Camera button (.btn-icon)
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(TopBarBorder)
                            .border(1.dp, BorderViolet, CircleShape)
                            .testTag("camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Attach image",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // User text input (input[type="text"])
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.input_placeholder),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextHint)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = SoftLavender,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .border(1.dp, BorderViolet, RoundedCornerShape(25.dp))
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
                            containerColor = SoftLavender,
                            contentColor = DeepMidnight
                        ),
                        shape = RoundedCornerShape(25.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("send_button")
                    ) {
                        Text(
                            text = stringResource(R.string.send),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Settings Modal (#settings-modal)
    if (showSettingsModal) {
        VesperaSettingsModal(
            currentApiKey = customApiKey,
            currentLanguage = selectedLanguage,
            currentPitch = voicePitch,
            currentSpeed = voiceSpeed,
            onDismiss = { showSettingsModal = false },
            onSave = { apiKey, lang, pitch, speed ->
                viewModel.updateSettings(
                    language = lang,
                    pitch = pitch,
                    speed = speed,
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
            title = { Text("Clear Chat?", color = TextPrimary) },
            text = { Text("Kya aap Vespera ke saath conversation history delete karna chahte hain?", color = TextSecondary) },
            containerColor = DarkSurface,
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
                    Text("Cancel", color = SoftLavender)
                }
            }
        )
    }
}

/**
 * Top Bar matching HTML #top-bar
 */
@Composable
private fun VesperaTopBar(
    onOpenSettings: () -> Unit,
    onClearChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TopBarBg)
            .border(width = 1.dp, color = TopBarBorder)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Title: Vespera v1.1 (Hinata Persona)
        Text(
            text = "Vespera v1.1 (Hinata Persona)",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TopBarTitle
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Settings button (.btn-icon with "⚙️ Settings")
            Surface(
                color = TopBarBorder,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clickable { onOpenSettings() }
                    .testTag("settings_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚙️ Settings",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Clear chat icon button
            IconButton(
                onClick = onClearChat,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(TopBarBorder)
                    .testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Conversation",
                    tint = SoftLavender,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Chat Message Item with v1.1 Hinata / User Bubble styling
 */
@Composable
private fun ChatMessageItem(
    message: ChatMessageEntity,
    onSpeakAgain: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .background(if (isUser) UserBubble else AiBubble)
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = if (isUser) androidx.compose.ui.graphics.Color.Transparent else BorderViolet,
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
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

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                )

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
                                tint = SoftLavender.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
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
                                tint = SoftLavender.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
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
private fun VesperaTypingIndicator() {
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
            .clip(RoundedCornerShape(14.dp))
            .background(AiBubble)
            .border(1.dp, BorderViolet, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Vespera (Hinata) is thinking…",
            style = MaterialTheme.typography.bodySmall.copy(color = SoftLavender, fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(dotScale)
                .clip(CircleShape)
                .background(GlowingCyan)
        )
    }
}

/**
 * Quick prompt suggestions
 */
@Composable
private fun QuickPromptChips(onSelectPrompt: (String) -> Unit) {
    val prompts = listOf(
        "Hello Hinata! 🌸",
        "Kaise ho aap?",
        "Ek pyara sa Hinglish joke sunao! 😂",
        "Kuch romantic shayari bolo ✨",
        "Mujhe motivate karo! 💪"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(prompts) { prompt ->
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderViolet.copy(alpha = 0.7f)),
                modifier = Modifier.clickable { onSelectPrompt(prompt) }
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SoftLavender,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Settings Modal (#settings-modal) matching the v1.1 prototype
 */
@Composable
private fun VesperaSettingsModal(
    currentApiKey: String,
    currentLanguage: String,
    currentPitch: Float,
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, language: String, pitch: Float, speed: Float) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf(currentLanguage) }
    var pitchSlider by remember { mutableFloatStateOf(currentPitch) }
    var speedSlider by remember { mutableFloatStateOf(currentSpeed) }

    val languages = listOf("Hinglish", "Hindi", "English")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SoftLavender
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Setting Item: Gemini API Key
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini API Key",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Text(
                            text = if (isKeyVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall.copy(color = SoftLavender),
                            modifier = Modifier.clickable { isKeyVisible = !isKeyVisible }
                        )
                    }
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = {
                            Text("Paste API Key here", color = TextHint, fontSize = 13.sp)
                        },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ModalInputBg,
                            unfocusedContainerColor = ModalInputBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = SoftLavender,
                            unfocusedBorderColor = BorderViolet
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apiKey")
                    )
                }

                // Setting Item: Response Language
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Response Language",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        languages.forEach { lang ->
                            val isSelected = selectedLang.equals(lang, ignoreCase = true) ||
                                    (lang == "Hindi" && selectedLang.equals("Pure Hindi", ignoreCase = true)) ||
                                    (lang == "Hinglish" && selectedLang.startsWith("Hinglish", ignoreCase = true))

                            Surface(
                                color = if (isSelected) NeonPurple else ModalInputBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) SoftLavender else BorderViolet
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLang = lang }
                            ) {
                                Text(
                                    text = if (lang == "Hinglish") "Hinglish" else if (lang == "Hindi") "Pure Hindi" else "English",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Setting Item: Voice Pitch (Hinata Style - High)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Voice Pitch (Hinata Style - High)",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", pitchSlider),
                            style = MaterialTheme.typography.labelMedium.copy(color = SoftLavender, fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = pitchSlider,
                        onValueChange = { pitchSlider = it },
                        valueRange = 1.0f..2.0f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = SoftLavender,
                            activeTrackColor = NeonPurple,
                            inactiveTrackColor = ModalInputBg
                        ),
                        modifier = Modifier.testTag("voicePitch")
                    )
                }

                // Setting Item: Voice Speed
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Voice Speed",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", speedSlider),
                            style = MaterialTheme.typography.labelMedium.copy(color = SoftLavender, fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = speedSlider,
                        onValueChange = { speedSlider = it },
                        valueRange = 0.7f..1.3f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = SoftLavender,
                            activeTrackColor = NeonPurple,
                            inactiveTrackColor = ModalInputBg
                        ),
                        modifier = Modifier.testTag("voiceRate")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKeyInput, selectedLang, pitchSlider, speedSlider)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftLavender,
                    contentColor = DeepMidnight
                ),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_settings_button")
            ) {
                Text(
                    text = "Save & Close",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    )
}
