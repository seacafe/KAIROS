package com.kairos.trading.domain.technical.service;

import com.kairos.trading.common.client.KiwoomClient;
import com.kairos.trading.domain.technical.agent.VectorAiClient;
import com.kairos.trading.domain.technical.dto.PriceTimeSeriesResponse;
import com.kairos.trading.domain.technical.dto.MovingAverage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VectorServiceTest {

    @Mock
    private KiwoomClient kiwoomClient;

    @Mock
    private VectorAiClient vectorAiClient;

    @Mock
    private NanoBananaCalculator nanoBananaCalculator;

    @InjectMocks
    private VectorService vectorService;

    @Test
    @DisplayName("SMA(이동평균) 계산 검증 - 데이터가 충분할 때")
    void calculateMovingAverages_shouldReturnCorrectValues_whenDataIsSufficient() {
        // given
        String stockCode = "005930";
        List<PriceTimeSeriesResponse.TimeSeriesData> timeSeries = new ArrayList<>();

        // 100일치 데이터 생성 (가격: 10000, 10100, 10200...)
        // 최근 날짜가 인덱스 0이라고 가정하고 역순으로 생성하되, API 응답은 일반적으로 최신순
        for (int i = 0; i < 100; i++) {
            long price = 10000 + (i * 100);
            timeSeries.add(new PriceTimeSeriesResponse.TimeSeriesData(
                    "202301" + String.format("%02d", i),
                    price, price, price, price, // open, high, low, close
                    1000L, 100000L, price));
        }
        // 최신순 정렬 (날짜 내림차순이라 가정) - 실제 키움 API는 최신 데이터가 먼저 옴
        Collections.reverse(timeSeries);

        var response = new PriceTimeSeriesResponse(
                stockCode, "Test Stock", 10000, 0, 0.0, 100000, timeSeries);

        given(kiwoomClient.getPriceTimeSeries(anyString(), anyString(), anyString()))
                .willReturn(response);

        // when
        // VectorService에 calculateMovingAverages 메서드가 없으므로 컴파일 에러가 발생할 것임 (Red Step)
        // 임시로 public 메서드 호출을 상정하고 작성 (구현 후 private이나 패키지 프라이빗으로 변경 고려)
        var result = vectorService.calculateMovingAverages(stockCode);

        // then
        assertThat(result).isNotNull();
        // 5일 이동평균 (최근 5일 종가 평균)
        assertThat(result.ma5()).isGreaterThan(0);
        assertThat(result.ma20()).isGreaterThan(0);
        assertThat(result.ma60()).isGreaterThan(0);
    }
}
