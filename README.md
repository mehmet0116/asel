# AI Koda Sistani - Android AI Assistant Application

## 🚀 Overview

AI Koda Sistani (AI Code Assistant) is a professional Android application that provides AI-powered code assistance, image analysis, video processing, and intelligent chat capabilities. The app supports multiple AI providers including OpenAI, Google Gemini, DeepSeek, and Qwen.

## ✨ Features

### Core Features
- **Multi-Provider AI Support**: OpenAI, Gemini, DeepSeek, and Qwen
- **Intelligent Chat**: Context-aware conversations with AI
- **Deep Thinking Mode**: Multiple analysis depth levels for complex problems
- **Code Analysis**: Automatic code detection, syntax highlighting, and analysis
- **Image Processing**: OCR, image analysis, and multi-image support
- **Video Analysis**: Frame extraction and intelligent video content analysis
- **File Processing**: Support for PDF, Word, Excel, CSV, and ZIP files
- **Session Management**: Save and restore conversation sessions
- **Dark/Light Theme**: Automatic theme switching

### Advanced Capabilities
- **Smart Code Completion**: Context-aware code suggestions
- **Multi-language Support**: Works with multiple programming languages
- **ZIP Project Analysis**: Intelligent project structure analysis
- **Web Content Extraction**: Analyze content from URLs
- **Markdown Rendering**: Beautiful message formatting

## 🏗️ Architecture

### Clean Architecture Implementation

The application follows professional Android architecture patterns with clear separation of concerns:

```
app/
├── managers/           # Business logic and operations
│   ├── AIPromptManager.kt      # AI prompt generation
│   ├── DialogManager.kt        # Dialog operations
│   ├── ImageManager.kt         # Image processing
│   ├── MessageManager.kt       # Message operations
│   └── SettingsManager.kt      # Settings management
├── models/             # Data models
│   ├── Message.kt
│   ├── ThinkingLevel.kt
│   └── TokenLimits.kt
├── ui/                 # UI components
│   └── MessageAdapter.kt
├── data/               # Database and DAOs
├── util/               # Utility classes
└── MainActivity.kt     # Main coordinating activity
```

### Recent Refactoring

The MainActivity was recently refactored from **3090 lines** to **2926 lines**, with **1,187 lines** extracted into focused manager classes. This improves:
- 📖 **Code Readability**: Easier to understand and navigate
- 🔧 **Maintainability**: Simpler to modify and extend
- 🧪 **Testability**: Better unit test coverage
- 🎯 **Focus**: Single responsibility principle

For detailed refactoring information, see [REFACTORING.md](./REFACTORING.md).

## 🛠️ Technical Stack

### Core Technologies
- **Language**: Kotlin
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle with Kotlin DSL

### Key Libraries
- **Kotlin Coroutines**: Asynchronous programming
- **AndroidX Libraries**: Modern Android components
- **Room Database**: Local data persistence
- **OkHttp**: Network operations
- **Markwon**: Markdown rendering
- **CameraX**: Camera operations
- **Apache POI**: Office document processing
- **iText**: PDF processing
- **OpenCSV**: CSV file handling

### AI Providers
- OpenAI GPT models
- Google Gemini
- DeepSeek
- Alibaba Qwen

## 📋 Setup

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK with API level 34

### API Keys Configuration

Create a `local.properties` file in the project root:

```properties
OPENAI_API_KEY=your_openai_key_here
GEMINI_API_KEY=your_gemini_key_here
DEEPSEEK_API_KEY=your_deepseek_key_here
DASHSCOPE_API_KEY=your_qwen_key_here
```

### Build and Run

```bash
# Clone the repository
git clone https://github.com/mehmet0116/asel.git

# Open in Android Studio
# Build and run on device or emulator

# Or use Gradle command line
./gradlew assembleDebug
```

## 🧪 Testing

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "*.ZipFileAnalyzerUtilTest"

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Test Coverage
- Unit tests for utility classes
- Integration tests for managers
- UI tests for critical flows

## 📱 Usage

### Basic Chat
1. Open the app
2. Select your preferred AI provider
3. Type your message or question
4. Get intelligent responses

### Image Analysis
1. Tap the attachment button
2. Select "Take Photo" or "Choose from Gallery"
3. Add your question about the image
4. Receive detailed analysis

### Deep Thinking Mode
1. Long press the brain icon
2. Select thinking level (Light, Medium, Deep, Very Deep)
3. Ask complex questions
4. Get comprehensive, multi-perspective answers

### Code Analysis
1. Paste or type code in the chat
2. App automatically detects code blocks
3. Long press on AI response to analyze code
4. Get improvement suggestions and explanations

## 🔒 Security

- API keys are stored securely in local properties
- No sensitive data sent to third parties
- All network calls use HTTPS
- User data stored locally with Room database

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the existing code style and architecture
4. Add tests for new functionality
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep classes focused and small

## 📄 License

This project is available for use under appropriate licensing terms.

## 👥 Authors

- **Mehmet** - Initial work and maintenance

## 🙏 Acknowledgments

- OpenAI for GPT models
- Google for Gemini API
- DeepSeek for their AI models
- Alibaba Cloud for Qwen models
- All open source library contributors

## 📞 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Check existing issues before creating new ones
- Provide detailed information for bug reports

## 🗺️ Roadmap

### Current Version (1.2)
- ✅ Multi-provider AI support
- ✅ Deep thinking mode
- ✅ Image and video analysis
- ✅ Code analysis features
- ✅ Refactored architecture

### Planned Features
- [ ] Voice input and output
- [ ] Real-time code collaboration
- [ ] Advanced project templates
- [ ] Plugin system for extensions
- [ ] Cloud sync for sessions
- [ ] Export conversations to various formats

## 📊 Project Stats

- **Total Lines of Code**: ~15,000+
- **Number of Classes**: 20+
- **Supported File Types**: 10+
- **AI Providers**: 4
- **Languages Supported**: Turkish and English prompts

---

**Built with ❤️ for developers by developers**
