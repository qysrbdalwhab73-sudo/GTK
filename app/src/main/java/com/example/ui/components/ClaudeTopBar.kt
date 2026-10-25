package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModelConfig
import com.example.ui.theme.ClaudeAmberLight
import com.example.ui.theme.ClaudeDarkBorder

@Composable
fun ClaudeTopBar(
    modelConfig: ModelConfig,
    artifactCount: Int,
    onModelChange: (ModelConfig) -> Unit,
    onOpenArtifacts: () -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showModelMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Brand
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ClaudeAmberLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "GameReverse Icon",
                            tint = ClaudeAmberLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GameReverse",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Studio • Claude Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Actions: Artifacts & Clear
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Artifacts Drawer Button with badge
                    IconButton(
                        onClick = onOpenArtifacts,
                        modifier = Modifier.testTag("open_artifacts_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (artifactCount > 0) {
                                    Badge(
                                        containerColor = ClaudeAmberLight,
                                        contentColor = MaterialTheme.colorScheme.background
                                    ) {
                                        Text("$artifactCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Artifacts Drawer",
                                tint = if (artifactCount > 0) ClaudeAmberLight else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Clear Chat
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Model & Thinking Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Model Selector Pill
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showModelMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("model_selector_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ClaudeAmberLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = modelConfig.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Gemini 4 Argon (High Thinking)") },
                            onClick = {
                                onModelChange(
                                    modelConfig.copy(
                                        modelId = "gemini-4-argon",
                                        displayName = "Gemini 4 Argon",
                                        thinkingEnabled = true,
                                        searchGroundingEnabled = false
                                    )
                                )
                                showModelMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Gemini 3.8 Flash (Search Grounding)") },
                            onClick = {
                                onModelChange(
                                    modelConfig.copy(
                                        modelId = "gemini-3.8-flash",
                                        displayName = "Gemini 3.8 Flash",
                                        thinkingEnabled = false,
                                        searchGroundingEnabled = true
                                    )
                                )
                                showModelMenu = false
                            }
                        )
                    }
                }

                // Thinking Mode Toggle Pill ("وقف التفكير / تفكير عالي")
                val isThinking = modelConfig.thinkingEnabled
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isThinking) ClaudeAmberLight.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            onModelChange(modelConfig.copy(thinkingEnabled = !isThinking))
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("thinking_mode_toggle"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Thinking Mode",
                        tint = if (isThinking) ClaudeAmberLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isThinking) "Thinking: High" else "Thinking: Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isThinking) ClaudeAmberLight else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Search Grounding Indicator Pill
                if (modelConfig.searchGroundingEnabled) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Grounding",
                            tint = ClaudeAmberLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Search Grounded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
