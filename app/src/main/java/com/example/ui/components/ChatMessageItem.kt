package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Artifact
import com.example.data.model.ArtifactType
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import com.example.ui.theme.ClaudeAmberLight
import com.example.ui.theme.ClaudeDarkBorder
import com.example.ui.theme.ClaudeDarkSurface
import com.example.ui.theme.ClaudeDarkSurfaceVariant

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onOpenArtifact: (Artifact) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Message Container
            Surface(
                color = ClaudeDarkSurfaceVariant,
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                modifier = Modifier.testTag("user_message_bubble")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Attached file indicator
                    if (message.fileAnalysis != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Attachment,
                                contentDescription = null,
                                tint = ClaudeAmberLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${message.fileAnalysis.fileName} (${formatFileSize(message.fileAnalysis.fileSize)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClaudeAmberLight
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // Assistant (GameReverse) Message
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assistant_message_bubble"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Thinking Process Accordion (if present)
                if (!message.thinkingTrace.isNullOrBlank()) {
                    ThinkingTraceView(thinkingText = message.thinkingTrace)
                }

                // 2. Inline Tool Execution Cards
                message.toolExecutions.forEach { tool ->
                    ToolCard(tool = tool)
                }

                // 3. Response Text
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeDarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 4. Attached Artifact Pill / Card
                if (message.attachedArtifact != null) {
                    ArtifactPreviewCard(
                        artifact = message.attachedArtifact,
                        onClick = { onOpenArtifact(message.attachedArtifact) }
                    )
                }

                // 5. Generated Image (if present)
                if (message.isMediaImage && message.mediaUrl != null) {
                    val bitmap = remember(message.mediaUrl) {
                        try {
                            val decoded = Base64.decode(message.mediaUrl, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Generated Game Asset",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, ClaudeDarkBorder, RoundedCornerShape(12.dp))
                        )
                    }
                }

                // 6. Generated Video Card (if present)
                if (message.isMediaVideo) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ClaudeDarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeDarkBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ClaudeAmberLight.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = ClaudeAmberLight
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Generated 5s Game Preview Video",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Synthesized via Gemini Omni 1.1 Flash Interactions API",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtifactPreviewCard(
    artifact: Artifact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClaudeDarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeDarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("artifact_preview_card")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ClaudeAmberLight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (artifact.type) {
                            ArtifactType.HEX_DUMP -> Icons.Default.Memory
                            else -> Icons.Default.Code
                        },
                        contentDescription = null,
                        tint = ClaudeAmberLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = artifact.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Click to open interactive ${artifact.type.name.lowercase().replace('_', ' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "View",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
