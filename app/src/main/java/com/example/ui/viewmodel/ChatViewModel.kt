package com.example.ui.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.model.Artifact
import com.example.data.model.ArtifactType
import com.example.data.model.ChatMessage
import com.example.data.model.FileAnalysisResult
import com.example.data.model.MessageRole
import com.example.data.model.ModelConfig
import com.example.data.model.ToolExecution
import com.example.data.model.ToolStatus
import com.example.data.remote.GeminiClient
import com.example.domain.analyzer.GameBinaryAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.chatMessageDao()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _modelConfig = MutableStateFlow(
        ModelConfig(
            modelId = "gemini-4-argon",
            displayName = "Gemini 4 Argon",
            thinkingEnabled = true,
            thinkingLevel = "HIGH",
            searchGroundingEnabled = false
        )
    )
    val modelConfig: StateFlow<ModelConfig> = _modelConfig.asStateFlow()

    private val _selectedFile = MutableStateFlow<FileAnalysisResult?>(null)
    val selectedFile: StateFlow<FileAnalysisResult?> = _selectedFile.asStateFlow()

    private val _pendingArtifact = MutableStateFlow<Artifact?>(null)

    private val _activeArtifact = MutableStateFlow<Artifact?>(null)
    val activeArtifact: StateFlow<Artifact?> = _activeArtifact.asStateFlow()

    private val _artifactsList = MutableStateFlow<List<Artifact>>(emptyList())
    val artifactsList: StateFlow<List<Artifact>> = _artifactsList.asStateFlow()

    private val _showArtifactSheet = MutableStateFlow(false)
    val showArtifactSheet: StateFlow<Boolean> = _showArtifactSheet.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private var audioOutputFile: File? = null

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val entities = dao.getAllMessages().first()
            if (entities.isNotEmpty()) {
                val loaded = entities.map { e ->
                    val artifact = if (e.artifactTitle != null && e.artifactContent != null) {
                        Artifact(
                            title = e.artifactTitle,
                            type = try { ArtifactType.valueOf(e.artifactType ?: "HEX_DUMP") } catch (_: Exception) { ArtifactType.HEX_DUMP },
                            content = e.artifactContent
                        )
                    } else null

                    ChatMessage(
                        id = e.id,
                        role = if (e.role == "USER") MessageRole.USER else MessageRole.ASSISTANT,
                        text = e.text,
                        thinkingTrace = e.thinkingTrace,
                        attachedArtifact = artifact,
                        mediaUrl = e.mediaUrl,
                        timestamp = e.timestamp
                    )
                }
                _messages.value = loaded
                _artifactsList.value = loaded.mapNotNull { it.attachedArtifact }
            } else {
                // Add initial welcoming message
                addInitialWelcomeMessage()
            }
        }
    }

    private fun addInitialWelcomeMessage() {
        val welcome = ChatMessage(
            role = MessageRole.ASSISTANT,
            text = "Welcome to **GameReverse Studio**. I am your dedicated AI game reverse engineering, modding, and asset deconstruction partner.\n\n" +
                    "• **Engine Specialization**: Unity (Mono & IL2CPP), Unreal Engine (PAK archives, reflection), Godot (PCK), and mobile native binaries.\n" +
                    "• **Capabilities**: Deep binary deconstruction with **Gemini 4 Argon High Thinking**, memory struct analysis, asset extraction, search grounding, and game art generation.\n\n" +
                    "Upload a game file (up to 10MB+) or tap one of the sample chips below to begin inspecting binary headers and weapon configs.",
            thinkingTrace = "Initialized GameReverse environment with Gemini 4 Argon (thinkingLevel = HIGH). Local binary analyzer ready for headers up to 10MB+."
        )
        _messages.value = listOf(welcome)
    }

    fun updateModelConfig(config: ModelConfig) {
        _modelConfig.value = config
    }

    fun attachFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                val (analysis, artifact) = GameBinaryAnalyzer.analyzeFile(
                    getApplication(),
                    uri,
                    fileName
                )
                _selectedFile.value = analysis
                _pendingArtifact.value = artifact
            } catch (e: Exception) {
                // Handle parsing error
            }
        }
    }

    fun removeSelectedFile() {
        _selectedFile.value = null
        _pendingArtifact.value = null
    }

    fun loadSample(sampleType: String) {
        viewModelScope.launch {
            val (bytes, fileName) = when (sampleType) {
                "unity_weapon" -> Pair(generateSampleUnityWeaponAsset(), "WeaponConfig_PlasmaRifle.asset")
                "unreal_pak" -> Pair(generateSampleUnrealPakHeader(), "GameData-WindowsNoEditor.pak")
                "godot_pck" -> Pair(generateSampleGodotPck(), "project_archive.pck")
                else -> Pair(generateSampleAndroidDex(), "classes.dex")
            }
            val (analysis, artifact) = GameBinaryAnalyzer.analyzeByteArray(bytes, fileName)
            _selectedFile.value = analysis
            _pendingArtifact.value = artifact
        }
    }

    fun sendMessage(promptText: String) {
        if (promptText.isBlank() && _selectedFile.value == null) return

        val currentFile = _selectedFile.value
        val currentArtifact = _pendingArtifact.value
        val userPrompt = if (promptText.isNotBlank()) promptText else "Analyze and deconstruct this attached game file."

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            text = userPrompt,
            fileAnalysis = currentFile,
            attachedArtifact = currentArtifact
        )

        _messages.value = _messages.value + userMessage
        _selectedFile.value = null
        _pendingArtifact.value = null
        _isLoading.value = true

        viewModelScope.launch {
            persistMessage(userMessage)

            val tools = mutableListOf<ToolExecution>()
            if (currentFile != null) {
                tools.add(
                    ToolExecution(
                        toolName = "Binary Header Prober",
                        status = ToolStatus.COMPLETED,
                        description = "Detected ${currentFile.detectedEngine} (${currentFile.fileSize} bytes, Entropy: ${"%.2f".format(currentFile.entropy)})"
                    )
                )
            }

            val config = _modelConfig.value
            val enrichedPrompt = buildString {
                append(userPrompt)
                if (currentFile != null) {
                    append("\n\n--- ATTACHED GAME FILE METADATA ---\n")
                    append("File: ${currentFile.fileName}\n")
                    append("Size: ${currentFile.fileSize} bytes\n")
                    append("Engine: ${currentFile.detectedEngine}\n")
                    append("Entropy: ${currentFile.entropy}\n")
                    append("Magic Bytes: ${currentFile.magicHeaderHex}\n")
                    append("Extracted Identifiers: ${currentFile.extractedStrings.joinToString(", ")}\n")
                    append("Hex Dump Snapshot:\n${currentFile.hexSnippet}\n")
                }
            }

            val response = if (config.searchGroundingEnabled) {
                tools.add(
                    ToolExecution(
                        toolName = "Google Search Grounding",
                        status = ToolStatus.RUNNING,
                        description = "Querying live game modding databases and engine documentation..."
                    )
                )
                GeminiClient.generateWithSearch(enrichedPrompt)
            } else {
                tools.add(
                    ToolExecution(
                        toolName = "Gemini 4 Argon Thinking Engine",
                        status = ToolStatus.RUNNING,
                        description = "Executing deep decompilation reasoning and struct alignment..."
                    )
                )
                GeminiClient.generateWithThinking(enrichedPrompt)
            }

            // Mark tools completed
            val finishedTools = tools.map { it.copy(status = ToolStatus.COMPLETED) }

            // Create resulting Artifact if appropriate
            val resultArtifact = currentArtifact ?: Artifact(
                title = "Analysis Artifact",
                type = ArtifactType.DECOMPILED_CODE,
                content = response.text,
                language = "csharp"
            )

            val assistantMessage = ChatMessage(
                role = MessageRole.ASSISTANT,
                text = response.text,
                thinkingTrace = response.thinkingTrace,
                attachedArtifact = resultArtifact,
                toolExecutions = finishedTools
            )

            _messages.value = _messages.value + assistantMessage
            _artifactsList.value = _artifactsList.value + resultArtifact
            _isLoading.value = false

            persistMessage(assistantMessage)
        }
    }

    fun generateArt(prompt: String, resolution: String) {
        _isLoading.value = true
        val userMsg = ChatMessage(
            role = MessageRole.USER,
            text = "Generate Game Texture / Art: '$prompt' ($resolution)"
        )
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            persistMessage(userMsg)

            val tool = ToolExecution(
                toolName = "Gemini 3 Pro Image Generator",
                status = ToolStatus.RUNNING,
                description = "Rendering game asset at $resolution resolution..."
            )

            val response = GeminiClient.generateImage(prompt, aspectRatio = "1:1", imageSize = resolution)

            val assistantMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                text = response.text,
                mediaUrl = response.imageBase64,
                isMediaImage = response.imageBase64 != null,
                toolExecutions = listOf(tool.copy(status = ToolStatus.COMPLETED))
            )

            _messages.value = _messages.value + assistantMsg
            _isLoading.value = false
            persistMessage(assistantMsg)
        }
    }

    fun generateVideo(prompt: String) {
        _isLoading.value = true
        val userMsg = ChatMessage(
            role = MessageRole.USER,
            text = "Generate Game Teaser Video: '$prompt'"
        )
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            persistMessage(userMsg)

            val tool = ToolExecution(
                toolName = "Gemini Omni 1.1 Flash (Interactions API)",
                status = ToolStatus.RUNNING,
                description = "Generating 5s 16:9 gameplay preview animation..."
            )

            val response = GeminiClient.generateVideo(prompt, aspectRatio = "16:9")

            val assistantMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                text = response.text,
                mediaUrl = response.videoBase64,
                isMediaVideo = true,
                toolExecutions = listOf(tool.copy(status = ToolStatus.COMPLETED))
            )

            _messages.value = _messages.value + assistantMsg
            _isLoading.value = false
            persistMessage(assistantMsg)
        }
    }

    fun toggleAudioRecording() {
        if (_isRecordingAudio.value) {
            stopAudioRecordingAndTranscribe()
        } else {
            startAudioRecording()
        }
    }

    private fun startAudioRecording() {
        try {
            val file = File(getApplication<Application>().cacheDir, "audio_record.mp4")
            audioOutputFile = file
            _isRecordingAudio.value = true
        } catch (_: Exception) {
            _isRecordingAudio.value = false
        }
    }

    private fun stopAudioRecordingAndTranscribe() {
        _isRecordingAudio.value = false
        viewModelScope.launch {
            _isLoading.value = true
            val transcribedText = GeminiClient.transcribeAudio(
                audioBytes = ByteArray(1024),
                mimeType = "audio/mp4"
            )
            _isLoading.value = false
            sendMessage(transcribedText)
        }
    }

    fun openArtifact(artifact: Artifact) {
        _activeArtifact.value = artifact
        _showArtifactSheet.value = true
    }

    fun openArtifactsDrawer() {
        val latest = _artifactsList.value.lastOrNull()
        if (latest != null) {
            _activeArtifact.value = latest
            _showArtifactSheet.value = true
        }
    }

    fun closeArtifact() {
        _showArtifactSheet.value = false
    }

    fun clearChat() {
        viewModelScope.launch {
            dao.clearHistory()
            _messages.value = emptyList()
            _artifactsList.value = emptyList()
            addInitialWelcomeMessage()
        }
    }

    private suspend fun persistMessage(message: ChatMessage) {
        val entity = ChatMessageEntity(
            id = message.id,
            role = message.role.name,
            text = message.text,
            thinkingTrace = message.thinkingTrace,
            fileName = message.fileAnalysis?.fileName,
            fileSize = message.fileAnalysis?.fileSize,
            detectedEngine = message.fileAnalysis?.detectedEngine,
            entropy = message.fileAnalysis?.entropy,
            artifactTitle = message.attachedArtifact?.title,
            artifactContent = message.attachedArtifact?.content,
            artifactType = message.attachedArtifact?.type?.name,
            mediaUrl = message.mediaUrl,
            timestamp = message.timestamp
        )
        dao.insertMessage(entity)
    }

    // --- Synthetic realistic sample binary generators for testing & demonstrations ---

    private fun generateSampleUnityWeaponAsset(): ByteArray {
        val buffer = ByteBuffer.allocate(1024).order(ByteOrder.BIG_ENDIAN)
        // UnityFS header
        buffer.put("UnityFS\u0000".toByteArray(Charsets.US_ASCII))
        buffer.putInt(7) // format version
        buffer.put("2022.3.14f1\u0000".toByteArray(Charsets.US_ASCII))
        // MonoBehaviour Weapon serialized fields
        buffer.put("MonoBehaviour\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("WeaponConfig_PlasmaRifle\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("m_BaseDamage: 145.0\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("m_FireRateRPM: 600.0\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("m_RecoilSpread: 0.12\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("m_AmmoCapacity: 30\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("m_AudioFireClip: sfx_plasma_shot.ogg\u0000".toByteArray(Charsets.US_ASCII))
        return buffer.array()
    }

    private fun generateSampleUnrealPakHeader(): ByteArray {
        val buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
        // PAK magic: 0x5A6F12E1
        buffer.put(0xE1.toByte())
        buffer.put(0x12.toByte())
        buffer.put(0x6F.toByte())
        buffer.put(0x5A.toByte())
        buffer.putInt(11) // PAK version
        buffer.putLong(512L) // Index offset
        buffer.putLong(1024L) // Index size
        buffer.put("Game/Weapons/Rifles/BP_RifleCore.uasset\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("Game/Characters/Player/PlayerController.uexp\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("bIsEncrypted: 0\u0000".toByteArray(Charsets.US_ASCII))
        return buffer.array()
    }

    private fun generateSampleGodotPck(): ByteArray {
        val buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("GDPC".toByteArray(Charsets.US_ASCII))
        buffer.putInt(2) // Godot pack format 2
        buffer.putInt(4) // Godot 4.x
        buffer.putInt(2) // 4.2
        buffer.put("res://scripts/weapons/weapon_laser.gd\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("res://scenes/levels/boss_room.tscn\u0000".toByteArray(Charsets.US_ASCII))
        return buffer.array()
    }

    private fun generateSampleAndroidDex(): ByteArray {
        val buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("dex\n035\u0000".toByteArray(Charsets.US_ASCII))
        buffer.putInt(0x12345678) // checksum
        buffer.put("Lcom/game/core/CombatSystem;\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("Lcom/game/network/PacketHandler;\u0000".toByteArray(Charsets.US_ASCII))
        buffer.put("calculateDamage(Lcom/game/Weapon;)F\u0000".toByteArray(Charsets.US_ASCII))
        return buffer.array()
    }
}
