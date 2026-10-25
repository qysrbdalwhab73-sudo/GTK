package com.example.domain.analyzer

import android.content.Context
import android.net.Uri
import com.example.data.model.Artifact
import com.example.data.model.ArtifactType
import com.example.data.model.FileAnalysisResult
import java.io.InputStream
import kotlin.math.log2
import kotlin.math.min

object GameBinaryAnalyzer {

    fun analyzeFile(
        context: Context,
        uri: Uri,
        fileName: String
    ): Pair<FileAnalysisResult, Artifact> {
        val contentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val fileBytes = inputStream?.use { stream ->
            // Read up to first 256KB for deep header, entropy & hex inspection
            val buffer = ByteArray(256 * 1024)
            val bytesRead = stream.read(buffer)
            if (bytesRead > 0) buffer.copyOf(bytesRead) else ByteArray(0)
        } ?: ByteArray(0)

        val totalSize = try {
            contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: fileBytes.size.toLong()
        } catch (_: Exception) {
            fileBytes.size.toLong()
        }

        val magicHeaderHex = fileBytes.take(16).joinToString(" ") { "%02X".format(it) }
        val engine = detectEngine(fileBytes, fileName)
        val entropy = calculateEntropy(fileBytes)
        val isEncrypted = entropy > 7.55
        val extractedStrings = extractStrings(fileBytes, minLength = 4, maxCount = 20)
        val hexSnippet = generateHexDump(fileBytes, maxLines = 32)

        val summary = buildString {
            append("Detected Format: $engine\n")
            append("Entropy: ${"%.2f".format(entropy)} / 8.00 (${if (isEncrypted) "High Entropy - Likely AES/Chacha encrypted or LZMA compressed" else "Structured Data / Bytecode"})\n")
            append("Key Identifiers: ${extractedStrings.take(5).joinToString(", ")}")
        }

        val analysisResult = FileAnalysisResult(
            fileName = fileName,
            fileSize = totalSize,
            mimeType = contentResolver.getType(uri) ?: "application/octet-stream",
            detectedEngine = engine,
            magicHeaderHex = magicHeaderHex,
            entropy = entropy,
            isLikelyEncrypted = isEncrypted,
            extractedStrings = extractedStrings,
            hexSnippet = hexSnippet,
            summary = summary
        )

        // Generate corresponding Interactive Artifact
        val artifactContent = buildArtifactContent(fileName, engine, entropy, isEncrypted, extractedStrings, hexSnippet)
        val artifact = Artifact(
            title = "$fileName Analysis",
            type = if (engine.contains("Unity") || engine.contains("Unreal")) ArtifactType.ASSET_TREE else ArtifactType.HEX_DUMP,
            content = artifactContent,
            language = "yaml"
        )

        return Pair(analysisResult, artifact)
    }

    fun analyzeByteArray(
        bytes: ByteArray,
        fileName: String
    ): Pair<FileAnalysisResult, Artifact> {
        val sample = bytes.copyOf(min(bytes.size, 256 * 1024))
        val magicHeaderHex = sample.take(16).joinToString(" ") { "%02X".format(it) }
        val engine = detectEngine(sample, fileName)
        val entropy = calculateEntropy(sample)
        val isEncrypted = entropy > 7.55
        val extractedStrings = extractStrings(sample, minLength = 4, maxCount = 20)
        val hexSnippet = generateHexDump(sample, maxLines = 32)

        val summary = buildString {
            append("Detected Format: $engine\n")
            append("Entropy: ${"%.2f".format(entropy)} / 8.00\n")
            append("Symbols Extracted: ${extractedStrings.size}")
        }

        val analysisResult = FileAnalysisResult(
            fileName = fileName,
            fileSize = bytes.size.toLong(),
            mimeType = "application/octet-stream",
            detectedEngine = engine,
            magicHeaderHex = magicHeaderHex,
            entropy = entropy,
            isLikelyEncrypted = isEncrypted,
            extractedStrings = extractedStrings,
            hexSnippet = hexSnippet,
            summary = summary
        )

        val artifactContent = buildArtifactContent(fileName, engine, entropy, isEncrypted, extractedStrings, hexSnippet)
        val artifact = Artifact(
            title = "$fileName Deconstruction",
            type = ArtifactType.HEX_DUMP,
            content = artifactContent,
            language = "yaml"
        )

        return Pair(analysisResult, artifact)
    }

    private fun detectEngine(bytes: ByteArray, fileName: String): String {
        if (bytes.size < 4) return "Unknown Binary"

        // Unity AssetBundle magic
        val header4 = String(bytes.take(min(bytes.size, 16)).toByteArray(), Charsets.US_ASCII)
        if (header4.startsWith("UnityFS")) return "Unity AssetBundle (UnityFS Archive)"
        if (header4.startsWith("UnityRaw")) return "Unity Raw Asset Stream"
        if (header4.startsWith("UnityWeb")) return "Unity Web Archive"

        // Unreal Engine PAK magic (\x5A\x6F\x12\xE1 in little endian)
        if (bytes.size >= 8 && bytes[0] == 0xE1.toByte() && bytes[1] == 0x12.toByte() && bytes[2] == 0x6F.toByte() && bytes[3] == 0x5A.toByte()) {
            return "Unreal Engine PAK Archive"
        }
        if (fileName.endsWith(".pak", ignoreCase = true)) {
            return "Unreal Engine PAK / Quake Archive"
        }

        // Godot Engine PCK magic
        if (header4.startsWith("GDPC")) return "Godot PCK Package"

        // ZIP / APK / JAR (PK\x03\x04)
        if (bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && bytes[2] == 3.toByte() && bytes[3] == 4.toByte()) {
            return if (fileName.endsWith(".apk", ignoreCase = true)) "Android APK Package" else "ZIP Archive"
        }

        // ELF binary (\x7fELF)
        if (bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            return "Native ELF Binary / Shared Library (.so)"
        }

        // DEX file (dex\n035)
        if (bytes[0] == 'd'.code.toByte() && bytes[1] == 'e'.code.toByte() && bytes[2] == 'x'.code.toByte()) {
            return "Dalvik Executable (DEX Bytecode)"
        }

        // PE / DLL (MZ)
        if (bytes[0] == 'M'.code.toByte() && bytes[1] == 'Z'.code.toByte()) {
            return "Windows PE / .NET Assembly (.dll / .exe)"
        }

        // SQLite
        if (header4.startsWith("SQLite")) return "SQLite 3 Database"

        // Texture DDS
        if (header4.startsWith("DDS ")) return "DirectDraw Surface Texture (DDS)"

        // Texture PNG
        if (bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()) {
            return "PNG Image Asset"
        }

        // Ogg audio
        if (header4.startsWith("OggS")) return "Ogg Vorbis Audio"

        return if (fileName.endsWith(".asset", ignoreCase = true)) "Unity Serialized MonoBehaviour (.asset)"
        else if (fileName.endsWith(".dat", ignoreCase = true)) "Custom Game Data Container (.dat)"
        else "Raw Binary / Unknown Engine"
    }

    private fun calculateEntropy(bytes: ByteArray): Double {
        if (bytes.isEmpty()) return 0.0
        val frequency = IntArray(256)
        for (b in bytes) {
            frequency[b.toInt() and 0xFF]++
        }
        var entropy = 0.0
        val total = bytes.size.toDouble()
        for (f in frequency) {
            if (f > 0) {
                val p = f / total
                entropy -= p * log2(p)
            }
        }
        return entropy
    }

    fun generateHexDump(bytes: ByteArray, maxLines: Int = 32): String {
        val sb = StringBuilder()
        val totalToDump = min(bytes.size, maxLines * 16)
        for (offset in 0 until totalToDump step 16) {
            sb.append("%08X  ".format(offset))
            val lineLength = min(16, bytes.size - offset)

            // Hex values
            for (i in 0 until 16) {
                if (i < lineLength) {
                    sb.append("%02X ".format(bytes[offset + i]))
                } else {
                    sb.append("   ")
                }
                if (i == 7) sb.append(" ")
            }

            sb.append(" |")
            // ASCII preview
            for (i in 0 until lineLength) {
                val b = bytes[offset + i].toInt() and 0xFF
                if (b in 32..126) {
                    sb.append(b.toChar())
                } else {
                    sb.append('.')
                }
            }
            sb.append("|\n")
        }
        return sb.toString()
    }

    private fun extractStrings(bytes: ByteArray, minLength: Int = 4, maxCount: Int = 20): List<String> {
        val list = mutableListOf<String>()
        val current = StringBuilder()
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            if (v in 32..126) {
                current.append(v.toChar())
            } else {
                if (current.length >= minLength) {
                    val s = current.toString().trim()
                    if (s.length >= minLength && !list.contains(s)) {
                        list.add(s)
                        if (list.size >= maxCount) break
                    }
                }
                current.clear()
            }
        }
        return list
    }

    private fun buildArtifactContent(
        fileName: String,
        engine: String,
        entropy: Double,
        isEncrypted: Boolean,
        symbols: List<String>,
        hexDump: String
    ): String {
        return buildString {
            append("file_name: \"$fileName\"\n")
            append("detected_engine: \"$engine\"\n")
            append("entropy_score: ${"%.3f".format(entropy)} # scale: 0.0 to 8.0\n")
            append("encryption_status: ${if (isEncrypted) "ENCRYPTED_OR_COMPRESSED" else "CLEARTEXT_STRUCTURED"}\n")
            append("extracted_symbols:\n")
            symbols.take(12).forEach { s ->
                append("  - \"${s.replace("\"", "\\\"")}\"\n")
            }
            append("\n# Hex Memory Inspection Snapshot:\n")
            append(hexDump)
        }
    }
}
