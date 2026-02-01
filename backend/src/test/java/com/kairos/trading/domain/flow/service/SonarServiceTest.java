package com.kairos.trading.domain.flow.service;

import com.kairos.trading.common.client.KiwoomClient;
import com.kairos.trading.domain.flow.agent.SonarAiClient;
import com.kairos.trading.domain.flow.dto.SectorIndexResponse;
import com.kairos.trading.domain.flow.dto.SectorIndexResponse.SectorStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SonarServiceTest {

    @Mock
    private SonarAiClient sonarAiClient;

    @Mock
    private KiwoomClient kiwoomClient;

    @InjectMocks
    private SonarService sonarService;

    @Test
    @DisplayName("fetchSectorIndex: 업종 지수 조회가 정상적으로 수행되어야 한다")
    void fetchSectorIndex_ShouldReturnIndex_WhenCalled() {
        // Given
        String sectorCode = "001"; // 종합주가지수
        String token = "dummy-token"; // 실제 구현에서는 토큰 서비스 사용 필요

        var topStocks = List.of(new SectorStock("005930", "삼성전자", 70000, 100, 0.5, 1000000));
        var expectedResponse = new SectorIndexResponse(
                sectorCode, "종합주가지수", 2500.0, 2490.0, 10.0, 0.4, 500000, 10000000, 2495.0, 2505.0, 2490.0, topStocks);

        // KiwoomClient는 토큰이 필요하므로, 서비스 내부에서 토큰 처리를 어떻게 할지 결정해야 함
        // 여기서는 간단히 KiwoomClient.getSectorIndex가 호출되는지만 검증
        given(kiwoomClient.getSectorIndex(anyString(), anyString())).willReturn(expectedResponse);

        // When
        // 아직 메서드가 없으므로 컴파일 에러 발생 (Red)
        var result = sonarService.fetchSectorIndex(sectorCode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.sectorCode()).isEqualTo("001");
        assertThat(result.currentIndex()).isEqualTo(2500.0);
    }
}
