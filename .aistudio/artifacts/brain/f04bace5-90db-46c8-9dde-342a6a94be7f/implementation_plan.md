# GameReverse Studio - AI Game Reverse Engineering & Modding Assistant

A high-performance Android game reverse engineering and modding studio featuring a Claude-inspired unified workspace, Gemini 4 Argon high-thinking analysis, on-device binary/asset deconstruction, and interactive artifact inspections.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions synthesize your clarified preferences and core specifications:

- **Confirmed Priority**: All-in-one game modding and asset deconstruction (Unity Mono/IL2CPP, Unreal PAK, Godot, memory structures, textures, meshes, and audio).
- **Confirmed Interface**: Claude-inspired unified chat stream with expandable inline tool cards, thinking trace viewer, and a sliding interactive Artifacts sheet (Hex viewer, decompiled script tree, asset previews).
- **Confirmed Intelligence Engine**: Gemini 4 Argon (`gemini-4-argon`) with `thinkingLevel = HIGH` by default for deep binary analysis, combined with `gemini-3.8-flash` for real-time web search grounding, image generation (`gemini-3-pro-image-preview`), audio transcription (`gemini-3.5-transcribe`), and video synthesis (`gemini-omni-1.1-flash`).
- **File Ingestion Capacity**: Full support for uploading and parsing files up to 10MB+ (APK, DEX, DLL, PAK, AssetBundles, DAT, audio, textures, and custom formats) with on-device header probing, hex inspection, and chunked Gemini multimodal ingestion.

---

## 1. Overview & Core Concept

**GameReverse Studio** transforms mobile game modding, localization, and asset extraction into a transparent, conversational journey. Instead of disjointed command-line utilities, users interact through an elegant, minimalist chat interface inspired by Claude. 

### Key Personas & Scenarios
- **Modders & Deconstructors**: Analyze game mechanics, inspect weapon attributes or character stats from data tables, extract textures/models, and craft mods for single-player games.
- **Translators & Arabization Specialists**: Locate localization tables, decode text encodings, extract font atlases, and test RTL text shaping and pointer updates.
- **Game Devs & Security Learners**: Study game engine layouts (Unity Mono/IL2CPP, Unreal Engine, Godot), explore memory layout theories, and audit asset packaging techniques.
- **Creative Explorers**: Generate game art concept sketches, texture variations, and promotional video clips directly within the modding project.

---

## 2. User Experience & Visual Design

### Claude-Inspired Aesthetic & Tone
- **Atmosphere**: Warm Dark Luxury (`#18181B` deep surface, `#27272A` card containers, `#F4F4F5` crisp high-contrast text, `#D97706` / `#F59E0B` warm amber & terracotta accents).
- **Typography**: Clean humanist sans-serif for conversation paired with monospace (`JetBrains Mono` / Monospace) for hex offsets, disassembled instructions, and data structs.
- **Unified Chat Stream**:
  - **Thinking Trace Accordion**: A collapsible "Thinking..." block displaying Gemini 4 Argon's high-depth analytical reasoning steps before presenting conclusions.
  - **Expandable Tool Cards**: Real-time visual feedback cards when tools execute (e.g., `Probing Archive Header`, `Hex Dump`, `Search Grounding`, `Generating Texture`).
  - **Interactive Artifact Drawer**: Clicking any code block, disassembly dump, or extracted asset opens a slide-over Artifact Sheet with syntax highlighting, search, and export options.
  - **Multi-Modal Input Bar**: Attachment picker supporting up to 10MB+ files, audio recording for Gemini transcribe, model switcher, and thinking toggle.

### Key User Flows

```
┌────────────────────────────────────────────────────────┐
│  GameReverse Studio ⚡ Claude-Style Header             │
│  [Model: Gemini 4 Argon ▾]  [Thinking: HIGH ▾] [Artifacts]│
├────────────────────────────────────────────────────────┤
│  Message Stream:                                       │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 👤 User: [Uploaded weapon_data.asset]            │  │
│  │    "Deconstruct this weapon item and stats."     │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 🤖 GameReverse (Gemini 4 Argon)                   │  │
│  │  ▼ Thinking Process (4.2s)                       │  │
│  │    • Probing magic header: Unity AssetBundle v20 │  │
│  │    • Resolving type tree offsets                 │  │
│  │  ----------------------------------------------  │  │
│  │  Identified Unity AssetBundle with serialized     │  │
│  │  MonoBehaviour "WeaponConfig".                   │  │
│  │  [📦 Open Asset Artifact (4 Properties Extracted)]│  │
│  └──────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────┤
│  [ + File (≤10MB) ] [ 🎙️ Speak ] [ Ask GameReverse... ]│
└────────────────────────────────────────────────────────┘
```

---

## 3. Key Product Decisions & Trade-Offs

1. **On-Device Probing + Gemini Multi-Modal Ingestion**:
   - *Chosen Approach*: On-device Kotlin parser handles immediate binary probing (extracting magic bytes, MIME detection, string extraction, and hex rendering) while passing structured payloads and base64 chunks to Gemini 4 Argon for high-level disassembly analysis.
   - *Why*: Ensures instantaneous local responsiveness for files up to 10MB without lag or upload timeouts, while allowing Gemini's high-thinking model to reason over complex logic.

2. **Claude-Style Integrated Chat with Artifact Sheet**:
   - *Chosen Approach*: Single-activity Compose architecture where the chat remains primary and artifacts (decompiled scripts, asset details, hex viewer) slide out smoothly as a bottom/side sheet.
   - *Why*: Prevents fragmented navigation across multiple confusing screens and mirrors the high-productivity flow of desktop AI workspaces.

3. **Multi-Model Orchestration**:
   - *Gemini 4 Argon*: Deep reasoning, code disassembly, logic reverse engineering, and complex file structure parsing with High Thinking.
   - *Gemini 3.8 Flash*: Fast conversational Q&A and real-time Search Grounding (documentation, game engine wikis, modding forums).
   - *Gemini 3 Pro Image Preview & Omni 1.1 Flash*: Generating visual weapon/texture art and animated video previews.
   - *Gemini 3.5 Transcribe*: Audio speech-to-text input.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                            Presentation Layer                          │
│  MainActivity ── ChatScreen ── ArtifactSheet ── HexInspectorView       │
│         ▲                    ▲                        ▲                │
│         │                    │                        │                │
│  ChatViewModel (UI State, Thinking Trace, Active Artifact, Recording)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                              Domain Layer                              │
│  GameReverseAnalyzer ── BinaryParser ── AssetDeconstructor             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                               Data Layer                               │
│  ┌───────────────────────┐                  ┌────────────────────────┐ │
│  │   GeminiApiService    │                  │      Room Database     │ │
│  │  • gemini-4-argon     │                  │  • ChatSessionEntity   │ │
│  │  • gemini-3.8-flash   │                  │  • ChatMessageEntity   │ │
│  │  • Search Grounding   │                  │  • FileArtifactEntity  │ │
│  │  • Image & Video REST │                  └────────────────────────┘ │
│  └───────────────────────┘                                             │
└────────────────────────────────────────────────────────────────────────┘
```

### Components to Implement
1. **Core Networking & Gemini Service (`data/remote/`)**:
   - Retrofit + Kotlinx Serialization client with 60s/300s timeouts.
   - Support for `gemini-4-argon` (thinkingConfig), `gemini-3.8-flash` (googleSearch tool), image generation, and Interactions API video polling.
2. **Local Binary & Asset Engine (`domain/analyzer/`)**:
   - Magic bytes classifier: Unity AssetBundles, Unreal PAK, ZIP/APK/JAR, Godot PCK, SQLite, ELF/DEX, PNG/DDS/KTX.
   - Byte-level Hex Dump generator with ASCII decoding for rapid file inspection.
3. **Claude UI Design System (`ui/theme/`, `ui/components/`)**:
   - Anthropic Claude inspired warm dark aesthetic, custom card containers, pill chips.
   - `ThinkingTrace` accordion with animated pulse indicator.
   - `ArtifactSheet` for live viewing and copying of decompiled code or asset structures.
   - Audio recorder with waveform feedback and Gemini transcribe.
4. **Room Database (`data/local/`)**:
   - Persistent chat sessions, message history with metadata, and cached file deconstruction artifacts.
