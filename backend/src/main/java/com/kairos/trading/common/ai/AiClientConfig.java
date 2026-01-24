package com.kairos.trading.common.ai;

import com.kairos.trading.domain.execution.agent.AegisReviewAiClient;
import com.kairos.trading.domain.flow.agent.SonarAiClient;
import com.kairos.trading.domain.fundamental.agent.AxiomAiClient;
import com.kairos.trading.domain.news.agent.SentinelAiClient;
import com.kairos.trading.domain.sentiment.agent.ResonanceAiClient;
import com.kairos.trading.domain.strategy.agent.NexusAiClient;
import com.kairos.trading.domain.technical.agent.VectorAiClient;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 에이전트 클라이언트 Bean 등록.
 * 
 * 각 에이전트는 전용 모델(Temperature 차등 적용)을 사용한다.
 * 
 * @see GeminiConfig
 * @see PROJECT-Specification.md §4
 */
@Slf4j
@Configuration
public class AiClientConfig {

    /**
     * Sentinel AI 클라이언트 (뉴스 분석)
     * Temperature: 0.1
     */
    @Bean
    public SentinelAiClient sentinelAiClient(@Qualifier("sentinelModel") ChatLanguageModel model) {
        log.info("SentinelAiClient 생성 (temp=0.1)");
        return AiServices.builder(SentinelAiClient.class)
                .chatLanguageModel(model)
                .build();
    }

    /**
     * Axiom AI 클라이언트 (재무 분석)
     * Temperature: 0.0 (수치 엄격 판단)
     */
    @Bean
    public AxiomAiClient axiomAiClient(@Qualifier("axiomModel") ChatLanguageModel model) {
        log.info("AxiomAiClient 생성 (temp=0.0)");
        return AiServices.builder(AxiomAiClient.class)
                .chatLanguageModel(model)
                .build();
    }

    /**
     * Vector AI 클라이언트 (차트 분석)
     * Temperature: 0.2
     */
    @Bean
    public VectorAiClient vectorAiClient(@Qualifier("vectorModel") ChatLanguageModel model) {
        log.info("VectorAiClient 생성 (temp=0.2)");
        return AiServices.builder(VectorAiClient.class)
                .chatLanguageModel(model)
                .build();
    }

    /**
     * Resonance AI 클라이언트 (시장 심리)
     * Temperature: 0.6 (뉘앙스 파악)
     */
    @Bean
    public ResonanceAiClient resonanceAiClient(@Qualifier("resonanceModel") ChatLanguageModel model) {
        log.info("ResonanceAiClient 생성 (temp=0.6)");
        return AiServices.builder(ResonanceAiClient.class)
                .chatLanguageModel(model)
                .build();
    }

    /**
     * Sonar AI 클라이언트 (수급 분석)
     * Temperature: 0.1 (패턴 매칭)
     */
    @Bean
    public SonarAiClient sonarAiClient(@Qualifier("sonarModel") ChatLanguageModel model) {
        log.info("SonarAiClient 생성 (temp=0.1)");
        return AiServices.builder(SonarAiClient.class)
                .chatLanguageModel(model)
                .build();
    }

    /**
     * Nexus AI 클라이언트 (전략가) - Pro 모델
     * Temperature: 0.2 (신중한 결정)
     */
    @Bean
    public NexusAiClient nexusAiClient(@Qualifier("nexusModel") ChatLanguageModel model) {
        log.info("NexusAiClient 생성 (gemini-2.5-pro, temp=0.2)");
        return AiServices.builder(NexusAiClient.class)
                .chatLanguageModel(model)
                .build();
    }

    /**
     * Aegis Review AI 클라이언트 (장후 슬리피지 분석)
     * Temperature: 0.1
     */
    @Bean
    public AegisReviewAiClient aegisReviewAiClient(@Qualifier("aegisReviewModel") ChatLanguageModel model) {
        log.info("AegisReviewAiClient 생성 (temp=0.1)");
        return AiServices.builder(AegisReviewAiClient.class)
                .chatLanguageModel(model)
                .build();
    }
}
