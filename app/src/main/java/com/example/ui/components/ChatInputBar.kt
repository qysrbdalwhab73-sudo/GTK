package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileAnalysisResult
import com.example.ui.theme.ClaudeAmberLight
import com.example.ui.theme.ClaudeDarkBorder
import com.example.ui.theme.ClaudeDarkSurface
import com.example.ui.theme.ClaudeDarkSurfaceVariant

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFileUri: (android.net.Uri, String) -> Unit,
    onLoadSample: (String) -> Unit,
    onGenerateArt: (String, String) -> Unit, // prompt, resolution
    onGenerateVideo: (String) -> Unit,
    isRecordingAudio: Boolean,
    onToggleAudioRecording: () -> Unit,
    selectedFile: FileAnalysisResult?,
    onRemoveFile: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var showActionMenu by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }

    // System File Picker (supports up to 10MB+ files of any type)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "game_data.bin"
            onAttachFileUri(uri, fileName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Sample Chips & Quick Modding Scenarios
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SampleChip(label = "📦 Unity Weapon Config") {
                onLoadSample("unity_weapon")
            }
            SampleChip(label = "🎮 Unreal 5 PAK Archive") {
                onLoadSample("unreal_pak")
            }
            SampleChip(label = "⚙️ Godot PCK File") {
                onLoadSample("godot_pck")
            }
            SampleChip(label = "📱 APK / DEX Bytecode") {
                onLoadSample("android_dex")
            }
            SampleChip(label = "🎨 Generate Game Art") {
                showImageDialog = true
            }
            SampleChip(label = "🎬 Generate Video") {
                showVideoDialog = true
            }
        }

        // Attached File Banner (if active)
        if (selectedFile != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ClaudeDarkSurfaceVariant)
                    .border(1.dp, ClaudeDarkBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ClaudeAmberLight)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedFile.fileName} • ${selectedFile.detectedEngine}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = onRemoveFile,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Claude-Style Floating Input Capsule
        Surface(
            color = ClaudeDarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeDarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach (+) Button
                Box {
                    IconButton(
                        onClick = { showActionMenu = true },
                        modifier = Modifier.testTag("attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach Game File",
                            tint = ClaudeAmberLight
                        )
                    }

                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Upload Game File (≤ 10MB)") },
                            onClick = {
                                showActionMenu = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Generate Texture / Weapon Art (1K/2K/4K)") },
                            onClick = {
                                showActionMenu = false
                                showImageDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Generate Game Teaser Video (Omni Flash)") },
                            onClick = {
                                showActionMenu = false
                                showVideoDialog = true
                            }
                        )
                    }
                }

                // Text Input
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .testTag("chat_input_field"),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                    ),
                    cursorBrush = SolidColor(ClaudeAmberLight),
                    decorationBox = { innerTextField ->
                        if (inputText.isBlank()) {
                            Text(
                                text = "Ask about game files, offsets, or mods...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                )

                // Voice Recording Button
                val micScale = if (isRecordingAudio) {
                    val transition = rememberInfiniteTransition(label = "mic_pulse")
                    val scale by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    scale
                } else 1f

                IconButton(
                    onClick = onToggleAudioRecording,
                    modifier = Modifier
                        .scale(micScale)
                        .testTag("voice_record_button")
                ) {
                    Icon(
                        imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Send Button
                IconButton(
                    onClick = onSendMessage,
                    enabled = (inputText.isNotBlank() || selectedFile != null) && !isLoading,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if ((inputText.isNotBlank() || selectedFile != null) && !isLoading) ClaudeAmberLight
                            else ClaudeDarkSurfaceVariant
                        )
                        .size(36.dp)
                        .testTag("send_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() || selectedFile != null) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal dialog for Game Art Generation (gemini-3-pro-image-preview)
    if (showImageDialog) {
        ImageGenerationDialog(
            onDismiss = { showImageDialog = false },
            onConfirm = { prompt, res ->
                showImageDialog = false
                onGenerateArt(prompt, res)
            }
        )
    }

    // Modal dialog for Video Generation (gemini-omni-1.1-flash)
    if (showVideoDialog) {
        VideoGenerationDialog(
            onDismiss = { showVideoDialog = false },
            onConfirm = { prompt ->
                showVideoDialog = false
                onGenerateVideo(prompt)
            }
        )
    }
}

@Composable
fun SampleChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = ClaudeDarkSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeDarkBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ImageGenerationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var prompt by remember { mutableStateOf("Cyberpunk plasma rifle weapon icon with gold and neon circuitry, high detail game texture") }
    var selectedRes by remember { mutableStateOf("1K") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Game Art (Gemini 3 Pro)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Resolution: $selectedRes", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1K", "2K", "4K").forEach { res ->
                        androidx.compose.material3.FilterChip(
                            selected = selectedRes == res,
                            onClick = { selectedRes = res },
                            label = { Text(res) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(prompt, selectedRes) }
            ) {
                Text("Generate Art")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun VideoGenerationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("Futuristic laser weapon firing particle beams in a game showcase 3D scene") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Game Preview Video (Omni Flash)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Uses gemini-omni-1.1-flash via Interactions API (16:9 aspect ratio)",
                    style = MaterialTheme.typography.bodySmall
                )
                androidx.compose.material3.OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Video Prompt") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(prompt) }
            ) {
                Text("Generate Video")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
