package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ArtifactSheet
import com.example.ui.components.ChatInputBar
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.ClaudeTopBar
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val modelConfig by viewModel.modelConfig.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val activeArtifact by viewModel.activeArtifact.collectAsStateWithLifecycle()
    val artifactsList by viewModel.artifactsList.collectAsStateWithLifecycle()
    val showArtifactSheet by viewModel.showArtifactSheet.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRecordingAudio by viewModel.isRecordingAudio.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ClaudeTopBar(
                modelConfig = modelConfig,
                artifactCount = artifactsList.size,
                onModelChange = { viewModel.updateModelConfig(it) },
                onOpenArtifacts = { viewModel.openArtifactsDrawer() },
                onClearChat = { viewModel.clearChat() }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onSendMessage = {
                    val text = inputText
                    inputText = ""
                    viewModel.sendMessage(text)
                },
                onAttachFileUri = { uri, name ->
                    viewModel.attachFile(uri, name)
                },
                onLoadSample = { sampleType ->
                    viewModel.loadSample(sampleType)
                },
                onGenerateArt = { prompt, res ->
                    viewModel.generateArt(prompt, res)
                },
                onGenerateVideo = { prompt ->
                    viewModel.generateVideo(prompt)
                },
                isRecordingAudio = isRecordingAudio,
                onToggleAudioRecording = {
                    viewModel.toggleAudioRecording()
                },
                selectedFile = selectedFile,
                onRemoveFile = { viewModel.removeSelectedFile() },
                isLoading = isLoading
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("chat_screen_scaffold")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_messages_lazy_column"),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageItem(
                        message = message,
                        onOpenArtifact = { artifact ->
                            viewModel.openArtifact(artifact)
                        }
                    )
                }
            }
        }
    }

    // Slide-out / Bottom Sheet Artifact Inspector
    if (showArtifactSheet && activeArtifact != null) {
        ArtifactSheet(
            artifact = activeArtifact,
            onDismiss = { viewModel.closeArtifact() }
        )
    }
}
