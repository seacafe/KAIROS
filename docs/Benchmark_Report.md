# [기술 심층 보고서] Prism-Insight 소스 코드 분석 및 KAIROS 적용 전략

**작성일:** 2026-01-17
**대상 리포지토리:** [dragon1086/prism-insight](https://github.com/dragon1086/prism-insight)
**적용 프로젝트:** Project KAIROS (Java 21, Spring Boot 3.5.7 기반)

---

## 1. 종목 선정 및 스크리닝 (Stock Selection)

`prism-insight`는 시장의 모든 종목을 분석하는 대신, **'변동성'**과 **'이슈'**가 발생한 종목으로 분석 범위를 좁히는 3단계 필터링 아키텍처를 채택하고 있습니다.

### 1.1 유니버스 필터링 (Quantitative Filter)

- **거래대금 기반:** 상장 종목 중 거래대금 상위 N% 이내의 종목만 추출하여 유동성이 확보된 종목으로 제한합니다.
- **기술적 정적 필터:** 주가가 이동평균선(MA20) 위에 위치하거나, 특정 가격대(동전주 제외) 이상인 종목을 1차 후보군으로 선정합니다.

### 1.2 뉴스 트리거 분석 (Event-Driven Trigger)

- **`news_collector.py` 로직:** 특정 종목에 대해 최근 24시간 내 발생한 뉴스의 빈도를 계산합니다.
- **임계값 설정:** 뉴스 발생 빈도가 과거 평균 대비 **300% 이상 급증(Surge)**할 경우, 해당 종목을 '심층 분석 대상'으로 확정하고 에이전트 파이프라인에 투입합니다.

---

## 2. 매수/매도 시그널 생성 (Trading Signals)

단일 지표의 골든크로스가 아닌, **정형 데이터(지표)와 비정형 데이터(LLM 해석)**의 일치성(Alignment)을 핵심 시그널로 봅니다.

### 2.1 매수 시그널 (Entry Logic)

- **기술적 지표 (Technical Agent):**
  - RSI가 30 이하에서 탈출하거나 50을 상향 돌파할 때.
  - 단기/중기 이평선이 정배열을 유지하며 거래량이 전일 대비 200% 이상 실릴 때.
- **감성 점수 (Sentiment Agent):**
  - 뉴스 분석 결과 호재의 강도가 70점(0~100 기준) 이상일 때.
- **합의 조건:**
  - `Sentiment Score > 0.7` 이고 `Technical Signal == BUY`인 경우에만 실제 매수 주문을 생성합니다.

### 2.2 매도 시그널 (Exit Logic)

- **익절 (Take-Profit):**
  - **ATR 기반 Trailing Stop:** ATR(변동폭)의 2~3배 수준을 익절가로 설정하고, 주가가 오를 때마다 매도 라인을 따라 올립니다.
- **손절 (Stop-Loss):**
  - 매수가 대비 -3% 또는 MA20 이탈 시 기계적으로 매도합니다.
- **뉴스 반전:** 보유 중인 종목에 대해 LLM이 '악재 발생' 또는 '재료 소멸'을 감지하면 기술적 지표와 상관없이 즉시 매도합니다.

---

## 3. 에이전트 합의 메커니즘 (Consensus Engine)

`prism-insight`의 핵심인 `consensus.py`는 **가중치 부여(Weighting)**와 **거부권(Veto)**이라는 두 가지 논리를 사용합니다.

### 3.1 에이전트별 가중치 배분

| 에이전트 역할 | 가중치 | 주요 판단 요소 |
| :--- | :--- | :--- |
| **News Agent** | 40% | 재료의 강도, 시장 파급력, 뉴스 지속성 |
| **Technical Agent** | 40% | 추세 정배열 여부, 수급(거래량), 과매수/과매도 |
| **Fundamental Agent**| 20% | 자본잠식 여부, 최근 분기 영업이익률, 부채비율 |

### 3.2 거부권 (Veto Logic)

아무리 뉴스나 차트 점수가 좋아도 아래 조건 중 하나라도 해당하면 **매매를 원천 차단**합니다.

- **Fundamental Veto:** 부채비율 급증, 횡령/배임 공시, 관리종목 지정 우려.
- **Technical Veto:** 현재 주가가 볼린저 밴드 상단을 크게 이탈하여 이격도가 과도하게 벌어진 경우(상투 방지).

---

## 4. KAIROS 프로젝트 이식 및 고도화 설계

### 4.1 가상 스레드 기반 병렬 추론 (Java 21 특화)

Python의 순차적 처리를 개선하여, Java 21의 가상 스레드를 통해 5대 에이전트를 동시 가동합니다.

```java
// KAIROS 에이전트 병렬 호출 예시
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<NewsScore> news = executor.submit(() -> sentinel.analyze(stock));
    Future<TechScore> tech = executor.submit(() -> vector.analyze(stock));
    Future<FundScore> fund = executor.submit(() -> axiom.analyze(stock));
    Future<SentScore> sent = executor.submit(() -> resonance.analyze(stock));
    Future<FlowScore> flow = executor.submit(() -> sonar.analyze(stock));

    // Nexus가 모든 결과를 취합하여 최종 결정 (Wait-Free)
    return nexus.decide(news.get(), tech.get(), fund.get(), sent.get(), flow.get());
}
```

### 4.2 NanoBanana 알고리즘의 결합

- **TechnicalAgent의 진화:** 단순 지표 대신, MA5/20/60의 수렴도(Convergence)를 계산하여 바나나 형태의 급등 직전 구간을 정밀 타격합니다.
- **수치 해석:** Java에서 계산된 수치(예: 수렴도 2.5%)를 Technical Agent(LLM)에게 전달하여 "이 수렴도가 역사적 저점인가?"를 묻는 하이브리드 방식을 채택합니다.

### 4.3 자가 보강 루프 (Self-Correction Loop)

- **Journaling 연동:** 장 마감 후 매매 결과를 복기합니다.
- **Feedback 로직:** "뉴스 점수는 높았으나 주가가 하락한 사례"를 수집하여 LLM에게 분석을 맡기고, 분석된 원인(예: "재료 선반영")을 다음 날 에이전트의 프롬프트 컨텍스트에 추가합니다.

---

## 5. 결론 및 향후 검증 지표

KAIROS는 `prism-insight`의 **멀티 에이전트 철학**을 계승하되, **Java 21의 고성능 병렬성**과 **NanoBanana 알고리즘의 정밀도**를 더해 실전 매매에 적합한 시스템으로 재설계되었습니다.

### 주요 검증 지표 (KPI)

1. **에이전트 합의 일치율:** 에이전트들의 합의 점수와 실제 익일 수익률 간의 상관관계.
2. **NanoBanana 정확도:** 이평선 수렴 탐지 후 3시간 이내 슈팅 발생 확률.
3. **복기 효율성:** 자가 보강 로직 적용 후 승률 변화 추이.
