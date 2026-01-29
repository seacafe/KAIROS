package com.kairos.trading.common.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j + Gemini API 설정.
 * 
 * PROJECT-Specification.md §4 기준:
 * - Flash 2.5: 분석가용 (5인)
 * - Pro 2.5: 전략가용 (Nexus)
 * 
 * 에이전트별 Temperature 설정:
 * - Sentinel: 0.1 (사실 기반)
 * - Axiom: 0.0 (수치 엄격 판단)
 * - Vector: 0.2 (차트 분석)
 * - Resonance: 0.6 (뉘앙스 파악)
 * - Sonar: 0.1 (패턴 매칭)
 * - Nexus: 0.2 (신중한 결정)
 * - Aegis Review: 0.1 (정확한 분석)
 */
@Slf4j
@Configuration
public class GeminiConfig {

    @Value("${gemini.api.key:}")
    private String apiKey;

    // ===== Sentinel (뉴스 분석) - Temp: 0.1 =====
    @Bean(name = "sentinelModel")
    public ChatLanguageModel sentinelModel() {
        log.info("Sentinel 모델 초기화 (gemini-2.5-flash, temp=0.1)");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.1)
                .topP(0.8)
                .maxOutputTokens(4096)

                .build();
    }

    // ===== Axiom (재무 분석) - Temp: 0.0 (엄격) =====
    @Bean(name = "axiomModel")
    public ChatLanguageModel axiomModel() {
        log.info("Axiom 모델 초기화 (gemini-2.5-flash, temp=0.0)");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.0)
                .topP(0.95)
                .maxOutputTokens(4096)

                .build();
    }

    // ===== Vector (차트 분석) - Temp: 0.2 =====
    @Bean(name = "vectorModel")
    public ChatLanguageModel vectorModel() {
        log.info("Vector 모델 초기화 (gemini-2.5-flash, temp=0.2)");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.2)
                .topP(0.85)
                .maxOutputTokens(4096)

                .build();
    }

    // ===== Resonance (심리 분석) - Temp: 0.6 (뉘앙스) =====
    @Bean(name = "resonanceModel")
    public ChatLanguageModel resonanceModel() {
        log.info("Resonance 모델 초기화 (gemini-2.5-flash, temp=0.6)");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.6)
                .topP(0.9)
                .maxOutputTokens(4096)

                .build();
    }

    // ===== Sonar (수급 분석) - Temp: 0.1 =====
    @Bean(name = "sonarModel")
    public ChatLanguageModel sonarModel() {
        log.info("Sonar 모델 초기화 (gemini-2.5-flash, temp=0.1)");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.1)
                .topP(0.8)
                .maxOutputTokens(4096)

                .build();
    }

    // ===== Nexus (전략가) - Pro 모델, Temp: 0.2 =====
    @Bean(name = "nexusModel")
    public ChatLanguageModel nexusModel() {
        log.info("Nexus 모델 초기화 (gemini-2.5-pro, temp=0.2)");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-pro")
                .temperature(0.2)
                .topP(0.9)
                .maxOutputTokens(8192)

                .build();
    }

    // ===== Aegis Review (장후 분석) - Temp: 0.1 =====
    @Bean(name = "aegisReviewModel")
    public ChatLanguageModel aegisReviewModel() {
        log.info("Aegis Review 모델 초기화 (gemini-2.5-flash, temp=0.1)");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.1)
                .topP(0.8)
                .maxOutputTokens(4096)

                .build();
    }
}
