package com.aikodasistani.aikodasistani.managers

import com.aikodasistani.aikodasistani.models.Message
import com.aikodasistani.aikodasistani.models.TokenLimits
import com.aikodasistani.aikodasistani.util.CodeDetectionUtil

/**
 * Manages AI prompts, token limits, and message optimization for different AI providers
 * Handles system prompts, thinking prompts, and context optimization
 */
class AIPromptManager {

    /**
     * Get token limits for specific provider and model combination
     */
    fun getModelTokenLimits(provider: String, model: String): TokenLimits {
        return when {
            provider == "OPENAI" && model.startsWith("gpt-3.5") -> TokenLimits(
                maxTokens = 3500,
                maxContext = 4096,
                historyMessages = 6
            )
            provider == "OPENAI" && (model.startsWith("gpt-4") || model.startsWith("gpt-4o")) -> TokenLimits(
                maxTokens = 3500,
                maxContext = 4096,
                historyMessages = 8
            )
            provider == "GEMINI" -> TokenLimits(
                maxTokens = 12000,
                maxContext = 128000,
                historyMessages = 15
            )
            provider == "DEEPSEEK" -> TokenLimits(
                maxTokens = 6000,
                maxContext = 64000,
                historyMessages = 10
            )
            provider == "QWEN" -> TokenLimits(
                maxTokens = 6000,
                maxContext = 64000,
                historyMessages = 10
            )
            else -> TokenLimits(
                maxTokens = 3500,
                maxContext = 4096,
                historyMessages = 8
            )
        }
    }

    /**
     * Get optimized history based on provider's token limits
     */
    fun getOptimizedHistory(history: List<Message>, provider: String, currentModel: String): List<Message> {
        val tokenLimits = getModelTokenLimits(provider, currentModel)
        return history.takeLast(tokenLimits.historyMessages).map { message ->
            optimizeMessageForModel(message, provider)
        }
    }

    /**
     * Optimize a single message for the model's context window
     */
    fun optimizeMessageForModel(message: Message, provider: String): Message {
        val isCodeMessage = message.text.contains("```") ||
                CodeDetectionUtil.detectLanguageAndCode(message.text).first != null

        val truncateLimit = when {
            isCodeMessage -> when (provider) {
                "GEMINI" -> 8000
                "OPENAI" -> 3000
                else -> 5000
            }
            else -> 2000
        }

        return if (message.text.length > truncateLimit) {
            message.copy(text = message.text.take(truncateLimit) + "\n[...devamı var]")
        } else {
            message
        }
    }

    /**
     * Get system prompt for the given provider
     */
    fun getSystemPrompt(provider: String): String {
        val basePrompt = """
        KODLAMA ASİSTANI - GEÇMİŞİ HATIRLA:
        
        KRİTİK TALİMATLAR:
        1. ✅ TÜM geçmiş konuşmayı HATIRLA
        2. ✅ Önceki kod parçalarını TEKRAR KULLAN
        3. ✅ Proje bağlamını KORU
        4. ✅ Tekrarlanan sorularda ÖNCEKİ cevapları REFERANS al
        5. ❌ ASLA "hatırlamıyorum" deme!
        
        KODLAMA ÖZEL:
        - Önceki import'ları hatırla
        - Class/function tanımlarını koru
        - Proje yapısını sürdür
    """

        return when (provider) {
            "OPENAI" -> "$basePrompt\n\nOpenAI Model: Uzun context kullan, geçmişi unutma!"
            "GEMINI" -> "$basePrompt\n\nGemini Model: 128K token kapasiten var, tüm geçmişi kullan!"
            "DEEPSEEK" -> "$basePrompt\n\nDeepSeek Model: Geçmiş bağlamı koru, kod context'ini sürdür!"
            "QWEN" -> "$basePrompt\n\nQwen Model: Chinese ve English destekli, geçmişi hatırla!"
            else -> basePrompt
        }
    }

    /**
     * Get system prompt for image-only analysis
     */
    fun getImageOnlySystemPrompt(imageCount: Int): String {
        val countNote = if (imageCount > 1) {
            "Birden fazla görseli 1), 2), 3) diye numaralandır."
        } else {
            "Tek görseli net ve kısa açıkla."
        }

        return """
        📷 GÖRSEL BETİMLEME MODU (DERİN DÜŞÜNME KAPALI)

        TALİMATLAR:
        - Sadece görselde GÖZÜKENİ Türkçe ve kısa anlat.
        - ❌ Öneri, yorum, tahmin, çözüm, aksiyon verme.
        - ❌ "İstersen" veya "öneririm" gibi yönlendirmeler yapma.
        - ✅ Nesneleri, ortamı, metinleri olduğu gibi aktar.
        - ✅ Emin değilsen "emin değilim" de, uydurma.
        - $countNote
        """.trimIndent()
    }

    /**
     * Build vision user prompt for image analysis
     */
    fun buildVisionUserPrompt(userMessage: String?, imageCount: Int): String {
        val baseInstruction = "Bu görsellerde ne görüyorsan SADECE onu anlat. Öneri veya yorum ekleme."
        val numbering = if (imageCount > 1) {
            "Her görseli 1), 2), 3) diye numaralandır ve ayrı ayrı betimle."
        } else {
            "Tek görseli kısa ve net tarif et."
        }

        val userNote = userMessage?.takeIf { it.isNotBlank() }?.let { "Kullanıcı isteği: $it" }

        return listOfNotNull(baseInstruction, numbering, userNote).joinToString("\n")
    }

    /**
     * Get leveled thinking prompt based on thinking level
     */
    fun getLeveledThinkingPrompt(userMessage: String?, level: Int): String {
        return when (level) {
            1 -> """
                🧠 HAFİF DÜŞÜNME MODU
                
                ORJİNAL SORU: $userMessage
                
                TALİMAT: Bu soruyu %30 daha detaylı cevapla.
                - 2 farklı açıdan değerlendir
                - Pratik çözüm öner
                - Kısa ve öz ol
            """.trimIndent()

            2 -> """
                🧠 ORTA DÜŞÜNME MODU
                
                ORJİNAL SORU: $userMessage
                
                TALİMAT: Bu soruyu %70 daha detaylı cevapla.
                - 3 farklı açıdan değerlendir
                - Her çözümün artı/eksilerini listele
                - En iyi çözümü seç ve nedenini açıkla
                - Uygulama adımlarını sırala
            """.trimIndent()

            3 -> """
                🧠 DERİN DÜŞÜNME MODU
                
                ORJİNAL SORU: $userMessage
                
                TALİMAT: Bu soruyu %120 daha detaylı cevapla.
                - 4+ farklı açıdan kapsamlı analiz yap
                - Her çözümü 5 kriterde değerlendir
                - Best practices ve pattern'leri dahil et
                - Detaylı implementasyon planı sun
                - Olası riskleri ve çözümlerini belirt
            """.trimIndent()

            4 -> """
                🧠 ÇOK DERİN DÜŞÜNME MODU - AKADEMİK SEVİYE
                
                ORJİNAL SORU: $userMessage
                
                TALİMAT: Bu soruyu %200 daha detaylı cevapla.
                - 5+ farklı disipliner açıdan analiz et
                - Akademik referanslar ve case study'ler kullan
                - Endüstri standartlarını ve en iyi uygulamaları dahil et
                - Multiple senaryolar ve edge case'ler için çözüm üret
                - Detaylı ROI analizi ve optimizasyon önerileri sun
                - Uzun vadeli stratejik planlama yap
            """.trimIndent()

            else -> userMessage ?: ""
        }
    }

    /**
     * Get deep thinking system prompt
     */
    fun getDeepThinkingSystemPrompt(userMessage: String?): String {
        return """
            🧠 DERİN DÜŞÜNME MODU - DETAYLI ANALİZ TALİMATI:
            
            KRİTİK GÖREV: Aşağıdaki soruyu NORMALDEN %50 DAHA DETAYLI cevapla!
            
            DÜŞÜNME ADIMLARI:
            1. 🔍 PROBLEM ANALİZİ: Sorunun kök nedenlerini araştır
            2. 💡 ÇÖZÜM ALTERNATİFLERİ: En az 3 farklı yaklaşım sun
            3. ⚖️ KARŞILAŞTIRMA: Her birinin artı/eksi yönlerini listele
            4. 🎯 TAVSİYE: En iyi çözümü seç ve nedenini açıkla
            5. 📝 UYGULAMA PLANI: Adım adım nasıl uygulanacağını anlat
            
            ÖNEMLİ: Normal yanıttan çok daha kapsamlı ve derinlemesine olmalı!
            
            SORU: ${userMessage ?: ""}
            """.trimIndent()
    }

    /**
     * Get video analysis system prompt
     */
    fun getVideoAnalysisSystemPrompt(): String {
        return """
            📹 VİDEO ANALİZ MODU - SADECE ANALİZ YAP!
            
            KRİTİK TALİMATLAR:
            1. ❌ KOD ÖNERME - SADECE ANALİZ ET
            2. ❌ TEKNİK ÇÖZÜM ÖNERME - SADECE TESPİT ET
            3. ✅ SADECE video içeriğini analiz et ve özetle
            4. ✅ Görsel öğeleri, hareketleri, ortamı tarif et
            5. ✅ Varsa metinleri oku ve aktar
            6. ✅ Senaryoyu anlat ve olası anlamları yorumla
            
            ÖNEMLİ: SADECE ANALİZ! Kod, çözüm, öneri, teknik detay YOK!
            """.trimIndent()
    }
}
