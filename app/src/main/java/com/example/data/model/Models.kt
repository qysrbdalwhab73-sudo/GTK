package com.example.data.model

import java.util.UUID

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

enum class ArtifactType {
    HEX_DUMP,
    DISASSEMBLY,
    ASSET_TREE,
    WEAPON_CONFIG,
    IMAGE_ASSET,
    VIDEO_ASSET,
    DECOMPILED_CODE
}

data class Artifact(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: ArtifactType,
    val content: String,
    val language: String = "text",
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class FileAnalysisResult(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val detectedEngine: String, // e.g., "Unity (AssetBundle v20)", "Unreal Engine PAK", "Godot PCK", "Android APK"
    val magicHeaderHex: String,
    val entropy: Double,
    val isLikelyEncrypted: Boolean,
    val extractedStrings: List<String>,
    val hexSnippet: String,
    val summary: String
)

data class ToolExecution(
    val id: String = UUID.randomUUID().toString(),
    val toolName: String,
    val status: ToolStatus,
    val description: String,
    val outputSnippet: String? = null
)

enum class ToolStatus {
    RUNNING, COMPLETED, ERROR
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val thinkingTrace: String? = null,
    val isThinkingExpanded: Boolean = false,
    val fileAnalysis: FileAnalysisResult? = null,
    val attachedArtifact: Artifact? = null,
    val toolExecutions: List<ToolExecution> = emptyList(),
    val mediaUrl: String? = null,
    val isMediaImage: Boolean = false,
    val isMediaVideo: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ModelConfig(
    val modelId: String = "gemini-4-argon",
    val displayName: String = "Gemini 4 Argon",
    val thinkingEnabled: Boolean = true,
    val thinkingLevel: String = "HIGH", // "HIGH" or "LOW"
    val searchGroundingEnabled: Boolean = false
)
