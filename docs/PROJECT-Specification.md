# 🚀 Project KAIROS: High-Frequency AI Trading System Specification (Final)

## 1. Project Overview & Strategy

**Project KAIROS**는 Java 21 **Virtual Threads**의 고성능 동시성 처리 능력과 **7인 AI 에이전트(7-Agent System)**의 집단 지성을 결합한 하이브리드 알고리즘 트레이딩 시스템입니다.
기존의 단순 분석을 넘어, **'3-Layer Data Strategy'**를 통해 정보의 사각지대를 없애고, **'Dual-Speed Architecture'**를 통해 전략의 안정성과 매매의 기민함을 동시에 달성합니다.

### 1.1 Core Workflow (Daily Cycle)

*상세 시나리오는 `Workflow_and_UI_Spec.md`를 따릅니다.*

1. **장전 (07:00~08:30) - Macro Discovery & Strategy**
    * **Sentinel(News)**이 네이버 검색 API(능동 검색)와 RSS를 기반으로 '특징주', '수주' 등 주도 테마를 발굴합니다.
    * **Axiom(Fund)**이 재무 리스크를 필터링하고, **Sonar(Flow)**가 전일 수급을 체크하며, **Vector(Tech)**가 차트 정배열 여부를 체크합니다.
    * **Nexus(Strategy)**가 위 4인의 리포트와 **사용자 설정(Aggressive/Neutral/Stable)**을 종합하여 `TargetStock`을 확정합니다.
2. **장전 (08:40~09:00) - Pre-Market Check**
    * **Aegis(PM)**가 장전 시간외 거래 및 예상 체결가(`ka10029`)를 모니터링하여 **과도한 갭상승(+15%)** 종목을 제외합니다.
    * 키움 API 토큰 갱신(`au10001`) 후 WebSocket(`00`, `0w`, `0A`) 접속 및 실시간 감시 체계로 전환합니다.
3. **장중 (09:00~15:00) - Hybrid Execution**
    * **Resonance(Sent)**가 시장 점수를 실시간 산출하여 하락장일 경우 **신규 진입을 원천 차단(Gatekeeping)**합니다.
    * **Vector(Tech)**가 실시간 틱 데이터에서 **NanoBanana 패턴**을 포착하고 **Sonar**가 수급을 검증하면 `BuySignal`을 보냅니다.
    * **Nexus**가 **재진입 여부(Re-entry)**를 판단하여 최종 승인하면, **Aegis**가 **PEQ(Priority Queue)**를 통해 주문을 집행합니다.
    * **Sentinel**이 VI(`1h`) 및 공시(`RSS`)를 감시하며 악재 발생 시 **Kill Switch**를 발동합니다.
4. **장중 (상시) - Dynamic Exit**
    * **Vector**가 실시간으로 **트레일링 스탑(ATR)**과 **추세 이탈(20일선)**을 감시하여 청산 신호를 보냅니다.
    * **Sentinel**이 '횡령/배임' 공시 감지 시 **Kill Switch**를 발동하여 **시장가 전량 매도**를 지시합니다.
5. **오후 (15:20) - Liquidation**
    * **Aegis**가 장 마감 전 잔여 물량을 전량 매도하여 오버나잇 리스크를 제거합니다.
6. **장후 (16:00~) - Self-Correction**
    * **Nexus**가 `TradeLog`를 분석하여 매매일지(`Journal`)를 자동 생성하고, AI 복기를 통해 파라미터 보정을 제안합니다.

---

## 2. Information Gathering (Hybrid Data Strategy)

### 2.1 Active Discovery Layer (Naver Search API)

* **Target Agent:** Sentinel (NewsAgent).
* **Purpose:** 장전(07:00~08:30) 주도 테마 발굴 및 특정 키워드에 대한 심층 검증.
* **Constraint:** **일일 25,000건 (Daily Quota)** 제한.
* **Strategy:**
  * **Burst Mode:** 장전 시간대에는 초당 10회까지 호출을 허용하여 정보를 선점합니다.
  * **Conservation:** 장중에는 RSS로 커버되지 않는 특이 이슈(예: 찌라시 검증)가 발생했을 때만 제한적으로 호출합니다.

### 2.2 Passive Surveillance Layer (RSS Feeds)

* **Target Agent:** Sentinel (NewsAgent) & Resonance (SentimentAgent).
* **Purpose:** 장중(09:00~15:00) 실시간 이슈, 공시, 글로벌 매크로 감시 (비용/제한 없음).
* **Mechanism:** `rss_feed_config` 테이블에 등록된 URL을 1분 주기로 Round-Robin Polling (`Rome` Library 사용).
* **Target Sources:**
  * **Domestic:** Hankyung, MK, Yonhap (General Market).
  * **Disclosure (Critical):** **DART (전자공시)** - '횡령', '배임', '거래정지' 등 Kill Switch 트리거.
  * **Global:** Investing.com, Nasdaq (Macro Sentiment).

---

## 3. Technical Architecture (Anti-gravity)

**"Blocking is Fine."** Java 21의 Virtual Threads를 전면 도입하여 I/O Blocking 비용을 제로에 가깝게 만들고, 복잡한 Reactive(WebFlux/R2DBC) 코드를 제거하여 유지보수성을 극대화합니다.

### 3.1 Backend Stack

* **Runtime:** Java 21 (LTS) - `ExecutorService.newVirtualThreadPerTaskExecutor()` 필수 적용.
* **Framework:** Spring Boot 3.5.7 (Latest Stable).
* **Concurrency:** Structured Concurrency (구조적 동시성) 적용.
* **AI Engine:** **LangChain4j** + Google Gemini API (`gemini-3.0` Series, mainly `gemini-3.0-pro-preview`).
* **Networking:**
  * **REST (Default):** `RestClient` (Sync Interface on Virtual Threads - 기본 사용).
  * **REST (Async):** `WebClient` (Non-blocking I/O, Streaming, 고성능 비동기 처리가 필요한 경우 제한적 사용).
  * **WebSocket:** `ReactorNettyWebSocketClient` (키움 실시간 시세 수신용).
* **RSS Parser:** `Rome` Tools (DB 기반 동적 URL 관리, 1분 주기 Polling).
* **Data Persistence (Standard JDBC):**
  * **DB:** **JDBC** + H2 (Dev) / PostgreSQL (Prod).
  * **Connection Pool:** **HikariCP** (Virtual Thread 환경에서도 안정적 성능 보장).
  * **ORM:** **Spring Data JPA** (Hibernate) - 표준 Blocking 방식 채택.
* **Resilience:** **Resilience4j** (Rate Limiter - Kiwoom: 5 req/sec, Naver: 25,000 req/day).
* **Testing:** JUnit 5, AssertJ, **WireMock** (External API Mocking).

### 3.2 Frontend Stack (Dashboard)

* **Core:** React 19, Vite, TypeScript.
* **Architecture:** **FSD (Feature-Sliced Design)** - `app`, `features`, `widgets`, `shared`.
* **State:** TanStack Query v5 (Server), Zustand (Client - Settings/Theme).
* **UI/UX:** Shadcn UI, Tailwind CSS (Responsive Design).
* **Visualization:** Recharts (Candle Chart + Signal Overlay), React-TreeMap (Portfolio Heatmap).

### 3.3 Global Traffic Governance (The Gatekeeper)

* **Virtual Thread Safety:** 수백 개의 가상 스레드가 동시에 외부 API를 호출할 때 발생할 수 있는 경쟁 상태(Race Condition)를 방지하기 위해 **중앙 집중식 토큰 버킷(Token Bucket)** 시스템을 운영합니다.
* **Implementation:** `Bucket4j` 라이브러리를 사용하여 API별(Kiwoom, Naver, Gemini)로 독립적인 버킷을 생성하고, 모든 에이전트는 **`ApiGatekeeper`** 컴포넌트를 통해서만 외부 통신을 수행할 수 있습니다.
* **Blocking Policy:** 토큰이 부족할 경우 Virtual Thread는 `park()` 상태로 전환되어 OS 자원을 점유하지 않고 대기하므로, 대량의 대기열이 발생해도 시스템은 멈추지 않습니다.

---

## 4. The 7-Agent System (Decision Pipeline)

KAIROS는 **[5인의 분석가] → [1인의 전략가] → [1인의 집행관]**으로 이어지는 직렬 파이프라인 구조를 가집니다. 각 에이전트는 독립적인 페르소나와 권한을 가집니다.

**[공통 설정]**

* **Safety Settings:** `BLOCK_NONE` (금융/뉴스 데이터 필터링 방지).
* **Response Format:** `application/json` (Backend 파싱 용이성 확보).

### 4.1 The Analysts (분석 위원회 - 5인)

#### 1. NewsAgent (Code Name: Sentinel)

* **역할:** 기계화된 감시자 (Surveillance & Alert).
* **데이터 소스:**
  * [Macro] Naver Search API (장전 주도주 발굴).
  * [Meso] RSS (DART, 연합인포맥스 - 공시/속보).
  * [Micro] 실시간 VI 발동 (`1h`).
* **핵심 임무:**
  * 네이버 검색 API를 사용하여 '특징주', '수주', '공시' 등의 키워드로 광범위하게 검색 후 종목 선정.
  * RSS Feed를 1분 주기로 감시. `DailyTargetStock`에 등록된 종목명이나 **'거래정지', '횡령', '배임', '감자'** 등의 치명적 키워드 감지 시 즉시 **'Kill Switch'** 시그널 생성.
  * VI 발동 시 관련 뉴스를 역추적하여 급등락 원인 분석.
* **설정값:** Model: `gemini-2.5-flash`, Temp: 0.1
* **System Prompt:**
    > "당신은 월스트리트의 냉철한 뉴스 분석가 'Sentinel'입니다. 입력된 뉴스/공시 텍스트에서 '종목명', '핵심 키워드(3개)', '호재/악재 여부(Positive/Negative)', '재료의 강도(0~100)', '즉각 대응 필요성(High/Low)'을 분석하여 감정은 배제하고 팩트 기준으로 JSON 형식으로 출력하십시오. 특히 DART 공시에서 '횡령', '배임', '감자', '거래정지' 키워드가 발견되면, 차트가 아무리 좋아도 재료 강도를 -100으로 설정하고 즉시 'Kill Switch' 시그널을 생성하십시오. VI(1h) 발동 시 관련 뉴스를 역추적하여 급등락 원인을 분석 보고하십시오."

#### 2. FundamentalAgent (Code Name: Axiom)

* **역할:** 재무 감사관 (Financial Auditor).
* **데이터 소스:** 주식기본정보 (`ka10001` - PER, PBR, 영업이익, 유보율).
* **핵심 임무:**
  * **'상장폐지 위험'**이 있는 한계 기업(3년 연속 적자, 자본 잠식) 원천 차단.
  * 저평가 우량주(Low PBR, High ROE) 선별.
* **설정값:** Model: `gemini-2.5-flash`, Temp: 0.0
* **System Prompt:**
    > "당신은 보수적인 회계 감사관 'Axiom'입니다. 기업의 기초 체력(Fundamental) 외에는 아무것도 믿지 마십시오. 재무제표를 분석하여 3년 연속 영업이익 적자이거나 부채비율이 업종 평균 대비 과도하게 높다면, 다른 에이전트가 매수를 추천해도 가차 없이 'Reject' 의견을 내십시오. 관리종목 지정 사유나 자본 잠식 여부를 최우선으로 검토해야 합니다. 당신의 목표는 '수익'이 아니라 '생존'입니다."

#### 3. TechnicalAgent (Code Name: Vector)

* **역할:** 차트 전략가 (Pattern Recognition) - **NanoBanana & Order Book Analysis**.
* **데이터 소스:** 주식일봉(`ka10081`), 분봉(`ka10080`), **시계열 차트(`ka10005`)**, 실시간 기세(`0A`), 체결(`0B`), 호가잔량(`0D`).
* **핵심 임무:**
  * **NanoBanana Pattern:** 이평선(5/20/60)이 밀집(Squeeze) 후 거래량을 동반하며 확산(Expansion)하는 구간을 1차적으로 포착합니다.
  * **Order Book Depth:** 실시간 호가 잔량(`0D`) 분포를 분석하여, 매수 잔량이 매도 잔량보다 비정상적으로 두터운 **'허매수 벽(Fake Wall)'**을 식별하고 진입을 차단합니다.
  * **Precision Targeting:** 단순한 방향성 예측을 넘어, 현재 호가 공백을 고려한 **'정밀 진입가(Entry Price)'**와 저항 매물대에 기반한 **'1차 목표가(Target Price)'**를 구체적 수치로 산출합니다.
  * **Exit:** 실시간 **ATR 트레일링 스탑** 및 **추세 이탈** 감시.
* **설정값:** Model: `gemini-2.5-flash`, Temp: 0.2
* **System Prompt:**
    > "당신은 20년 경력의 스캘핑 전문 트레이더 'Vector'입니다. 제공된 캔들 데이터와 **호가창 스냅샷(Order Book Snapshot)**을 정밀 분석하십시오.
    >
    > 1. **패턴 검증:** 이평선 밀집 후 거래량 폭발이 동반된 'NanoBanana' 패턴이 유효한지 확인하십시오.
    > 2. **호가 분석:** 매수 잔량이 매도 잔량 대비 3배 이상 쌓여 있다면 이를 '세력의 허매수'로 의심하고 패널티를 부여하십시오.
    > 3. **가격 산출:** 진입 승인 시, 시장가보다는 유리하고 체결 확률이 높은 **'최적 진입가'**와 **'1차 목표가'**를 정확한 숫자로 제시하십시오."

#### 4. SentimentAgent (Code Name: Resonance)

* **역할:** 시장 심리 분석가 (Market Psychologist).
* **데이터 소스:** Investing.com RSS (나스닥, 환율), 업종지수(`0J`).
* **핵심 임무:**
  * 시장이 '공포(Fear)'인지 '탐욕(Greed)'인지 판단.
  * 하락장(Bear Market) 시그널 감지 시 현금 비중 확대 강력 권고.
  * 점수 미달(안정형 50/중립형 40/공격형 30) 시 **신규 진입 차단(Veto)**.
* **설정값:** Model: `gemini-2.5-flash`, Temp: 0.6
* **System Prompt:**
    > "당신은 거시경제학자 'Resonance'입니다. 미국 나스닥 선물 지수와 주요 원자재 가격, 환율, 유가 및 뉴스 헤드라인의 어조, 커뮤니티의 반응, 섹터의 수급 쏠림 현상 등 을 분석 및 종합하여 'Market Heat Score(0~100)'를 산출하십시오. 시장 전체가 공포에 질려 있다면(40점 미만), 개별 종목의 호재를 무시하고 '보수적 대응'을 강력히 권고하십시오."

#### 5. TradingFlowAgent (Code Name: Sonar)

* **역할:** 수급 추적자 (Whale Tracker).
* **데이터 소스:** 실시간 프로그램(`0w`), 외인(`ka10008`), 기관(`ka10009`), 상위거래원(`ka10040`), **업종지수(`ka20001`)**.
* **핵심 임무:**
  * **'개미 털기' 식별:** 주가는 오르는데 실시간 프로그램 매도(`0w` 음수)가 쏟아지면 매수 거부.
  * 외인/기관의 양매수(Double Buy) 유입 시 강력 매수 시그널.
* **설정값:** Model: `gemini-2.5-flash`, Temp: 0.1
* **System Prompt:**
    > "당신은 수급 추적 전문가 'Sonar'입니다. 차트의 캔들은 조작될 수 있어도, 자금의 흐름(Program Trading)은 거짓말을 하지 않습니다. 실시간 프로그램 매매(0w) 추이를 감시하여, 주가 상승 시 프로그램 매도가 쏟아진다면 이를 '설거지(Fake Buying)'로 판정하고 매수 거부 의견을 제시하십시오."

---

### 4.2 The Strategist (전략가 - 1인)

#### 6. Investment Strategist (Code Name: Nexus)

* **역할:** 최종 의사결정권자 (The Brain).
* **입력 데이터:** 위 5명(Sentinel, Axiom, Vector, Resonance, Sonar)의 분석 리포트  + **User Strategy (Aggressive/Neutral/Stable)**.
* **핵심 임무:**
  * **Decision:** 매수 승인/거절 및 **Risk Level(비중 제한)** 결정.
  * **Dynamic Pricing:** 종목별 **목표가(TP)**와 **손절가(SL)** 계산.
  * **Re-entry:** `TradeLog`와 쿨타임을 고려하여 재진입 여부 판단.
  * `ExecutionOrder` 생성 후 Aegis에게 전달.
* **설정값:** Model: `gemini-2.5-pro`, Temp: 0.2
* **System Prompt:**
    > "당신은 KAIROS 헤지펀드의 수석 포트폴리오 매니저 'Nexus'입니다. 5인의 분석가가 제출한 보고서를 종합 검토하고 사용자 성향('${StrategyMode}')에 맞춰 판단하세요.
    > 1. **Aggressive:** 차트(Vector)와 뉴스(Sentinel) 가중치 1.5배. 리스크(High Risk) 감수하고 승인.
    > 2. **Neutral:** 펀더멘털과 기술적 분석의 균형 중시(50:50). 4명의 분석가 중 3명 이상이 긍정적이어야 승인.
    > 3. **Stable:** 실적(Axiom) 부실 시 무조건 기각. 수급(Sonar) 미비 시 보류. Risk Level Low인 종목만 승인.
    > 위 기준에 따라 Decision(BUY/WATCH/REJECT), Risk Level, Reason을 도출하고 최종 결정과 논리적 근거를 요약하여 Aegis에게 하달하십시오."

---

### 4.3 The Executor (집행관 - 1인)

#### 7. Portfolio Manager (Code Name: Aegis)

* **역할:** 자금 집행관 (The Wallet) & 매매 분석관 (The Reviewer).
* **구현 방식:**
  * **Runtime:** Pure Java Service (Virtual Threads) for Zero Latency.
  * **Post-time:** Gemini API Client for Trade Review.
* **데이터 소스:** 계좌평가(`kt00004`), 체결잔고(`kt00005`), **체결내역상세(`kt00007`)**, 주문(`kt10000`), 우선호가(`0C`).
* **핵심 임무:**
  * **The Button:** Nexus의 지시를 받아 실제 주문을 실행하는 유일한 권한.
  * **Veto Power:** 예수금 부족하거나 시장 점수 급락 시 매수 거부.
  * **Zero Latency Execution (장중):** AI 추론 없이 **100% Java 알고리즘**으로 예수금 확인 및 호가 스프레드 계산을 수행하여 1ms 내에 주문을 집행합니다.
  * **Slippage Guard:** 호가 스프레드가 넓을 경우 시장가 대신 **'유리한 지정가(Limit)'**로 정밀 타격.
  * **Rate Limit Guard:** 키움 API 호출을 **초당 4회**로 엄격히 제한(`ApiGatekeeper`)하여 계좌 동결(Ban)을 방지하고, 초과 요청은 큐에서 대기시킵니다.
  * **Correction:** 60초 미체결 시 Java 로직에 의해 자동으로 정정/취소를 수행합니다.
  * **Execution Analysis (장후):** 장 마감 후 당일 매매 로그를 전수 조사하여, 주문가 대비 체결가 오차(Slippage)가 0.5% 이상 발생한 건에 대해 AI에게 원인 분석을 요청합니다.
* **설정값:**
  * **Runtime:** Java Native (No AI Model)
  * **Review:** Model: `gemini-2.5-flash`, Temp: 0.1
* **System Prompt (For Post-Market Review Only):**
    > "당신은 냉철한 매매 분석관 'Aegis'입니다. 장 마감 후 `TradeLog`를 전수 조사하여, 오늘 발생한 거래 중 '슬리피지(Slippage) 과다 발생 거래'의 로그를 분석하십시오. 주문 시점의 호가 잔량(0C)과 체결 강도를 바탕으로, 내가 주문을 늦게 낸 것인지 아니면 호가 공백으로 인한 불가피한 현상이었는지 진단하고, 내일 적용할 **'주문 타이밍 보정값(Time Offset)'**을 제안하십시오."

---

## 5. Decision Algorithm & Data Strategy

### 5.1 3-Layer Data Strategy (The Funnel)

1. **Macro (광의 - Naver API):** "시장의 맥락 파악" (장전)
    * Sentinel이 주도 테마와 내러티브를 파악하여 종목 풀(Pool)을 구성.
2. **Meso (중의 - RSS Feeds):** "이벤트/트리거 감지" (장중)
    * DART 공시, 속보를 통해 펀더멘털 변화나 치명적 리스크(횡령 등) 감지.
3. **Micro (협의 - WebSocket):** "팩트/시그널 확인" **(Execution Trigger)**
    * 실시간 `1h`(VI), `0w`(프로그램), `00`(체결) 데이터를 통해 **뉴스보다 빠르게 시장의 반응을 읽고 선진입/선매도.**

### 5.2 Priority Execution Queue (PEQ)

Aegis는 자금 꼬임을 방지하기 위해 다음 순서로 주문을 처리합니다.

* **Priority 0 (Critical):** **Kill Switch (Sentinel)**, 로스컷. -> *즉시 시장가 매도.* `ApiGatekeeper`의 비상 예비 토큰을 사용하여 즉시 전송.*
* **Priority 1 (High):** Profit Take (익절). -> *`0C` 호가 참조 지정가/시장가 매도.* *토큰 대기 후 전송.*
* **Priority 2 (Normal):** New Buy (신규 매수). -> *예수금(`kt00004`) 확인 후 실행.* *예수금 확인 후 전송.*

### 5.3 Hybrid Execution & Latency Strategy (Dual-Speed)

AI의 추론 지연 시간(Latency)을 극복하고 초단기 변동성에 대응하기 위해 **Java(Reflex)와 AI(Brain)**의 역할을 엄격히 분리합니다.

| 구분 | 담당 (Role) | 주기 (Frequency) | 책임 (Responsibility) |
| :--- | :--- | :--- | :--- |
| **Java Algo** | **Vector (Core), Aegis** | **Tick / Real-time** | 실시간 시세 수신, 이평선 수렴 계산, **손절매(Stop-Loss) 자동 발동**, 트레일링 스탑 감시. (AI 승인 없이 즉시 실행) |
| **AI Agent** | **Nexus, Sentinel** | **Event / 1-Min** | 진입 여부 최종 승인(Entry Approval), 시장 분위기 파악, 재진입 판단, 목표가 재설정. |

* **Principle:** "진입(Entry)은 신중하게(AI), 청산(Exit)은 기계적으로(Java)."

---

## 6. External API Integration Summary

*(개발 시 `EXT_API_Specification.md` 및 `키움 API 명세` 폴더 참조)*

* **RSS:** `Rome` 라이브러리 사용.
* **Naver:** Search API (`X-Naver-Client-Id`).
* **Kiwoom:** OAuth2 (`au10001`), REST, WebSocket (`ReactorNetty`).

### 6.1 RSS Feeds (Real-time Surveillance Layer)

장중 실시간 뉴스, 공시(DART), 글로벌 매크로 지표 모니터링 (무제한/무료).

### 6.2 Naver Search API (Deep Dive Layer)

장전 주도주 발굴 및 RSS로 포착된 이슈의 심층 검증 (Quota 관리 필수).

### 6.3 Kiwoom Open API (Execution Layer)

장중(09:00~15:00) 주식 정보 확인 및 주식 매매, 거래 동향 확인.

* **Authentication:** `au10001` (토큰 발급). 응답 필드 `token` 확인 필수. `au10002` (토큰 폐기).
* **Headers:** `api-id`, `authorization`, `cont-yn`(연속조회), `next-key` 헤더 필수 구현.
* **Market Data:**
  * `ka10081`/`ka10080`: 일봉/분봉 차트 (Vector).
  * **`ka10005`:** 주식 일주월시분 시계열 조회 - 이동평균선(SMA) 계산용 (Vector).
  * **`ka20001`:** 업종 현재가 - 시장 전체 방향성 파악용 (Sonar).
  * `ka10008`(외인), `ka90003`(프로그램상위), `ka10040`(거래원): 수급 분석용 (Sonar).
* **Trading:**
  * `kt10000`/`kt10001`: 매수/매도 주문 (Aegis).
  * `kt00004`: 계좌평가현황 (예수금/D+2추정금 확인).
  * **`kt00007`:** 계좌별 주문 체결 내역 상세 - 매매 복기 및 성과 분석용 (Aegis).
* **Real-time (WebSocket):**
  * **`00` (체결):** 주문 체결 확인 및 미체결 잔량 관리.
  * **`0A` (주식기세):** **Vector**의 시가/고가/저가 분석.
  * **`0B` (주식체결):** **Vector**의 실시간 틱 분석.
  * **`0D` (호가잔량):** **Vector**의 호가창 변동 감지.
  * **`0w` (프로그램매매):** **Sonar**의 핵심 데이터. 장중 수급 이탈 감지.
  * **`1h` (VI발동):** **Sentinel**의 뉴스 크로스체크 트리거.
  * **`0C` (우선호가):** **Aegis**의 슬리피지 방지용 호가 확인.

### 6.4 Global Rate Limit Policy (Governance)

모든 외부 API 호출은 `ApiGatekeeper`를 통해 중앙 통제되며, 각 서비스의 정책과 과금 모델에 따라 차별화된 제한을 적용합니다.

| Target API | Limit Strategy | Quota / Setting | Rationale |
| :--- | :--- | :--- | :--- |
| **Kiwoom** | **Leaky Bucket** | **4 req/sec** (Strict) | 초당 5건 제한을 안전하게 준수하여 계좌 동결(Ban) 방지. |
| **Naver** | **Token Bucket** | **10 req/sec** (Daily 25k) | 순간적인 트래픽(Burst)은 허용하되, `Bucket4j`로 일일 총량(25,000)을 카운팅하여 고갈 시 RSS 모드로 강제 전환. |
| **Gemini** | **Token Bucket** | **1,000 req/min** (Safety) | **Pay-as-you-go(유료)** 모델 사용으로 성능 제한 해제. 단, 로직 오류로 인한 과금 폭탄 방지를 위해 소프트 리밋 적용. |

* **Note:** Gemini는 유료 모델이므로 속도 제한보다는 **비용 효율성**에 초점을 맞추며, Kiwoom은 **물리적 호출 제한** 준수에 초점을 맞춥니다.

---

## 7. Database Modeling (Persistence)

### 7.1 Entity Relationship Diagram (Conceptual)

* **Account (1) : Journal (N)** (계좌별 일지)
* **TargetStock (1) : TradeLog (N)** (하나의 분석 종목에 대해 여러 번 매매 가능 - 재진입)
* **TargetStock (1) : VirtualResult (1)** (오버나잇 시뮬레이션 결과)

### 7.2 Detailed Schema Specification

```sql
/* 1. 계좌 및 자산 (Resource) */
CREATE TABLE account (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    account_no VARCHAR(20) NOT NULL UNIQUE, -- 실계좌번호
    total_asset DECIMAL(18, 2),             -- 총 추정 자산
    deposit DECIMAL(18, 2),                 -- 예수금 (D+2)
    d2_deposit DECIMAL(18, 2),              -- 주문 가능 예수금
    daily_profit DECIMAL(18, 2),            -- 당일 실현 손익
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* 2. 시장 상황 이력 (Context - Time Series) */
/* Resonance 에이전트가 1분/5분 단위로 기록하는 시장 분위기 */
CREATE TABLE market_history (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    recorded_at TIMESTAMP NOT NULL,
    market_score INT,                       -- Resonance 점수 (0~100)
    kospi_index DECIMAL(10, 2),
    kosdaq_index DECIMAL(10, 2),
    us_futures_index DECIMAL(10, 2),
    risk_status VARCHAR(10)                 -- RISK_ON, RISK_OFF
);
CREATE INDEX idx_market_time ON market_history(recorded_at);

/* 3. 종목 분석 및 전략 (Strategy - The Brain) */
/* 매일 장전/장중 분석된 종목의 스냅샷. TradeLog의 부모 엔티티 */
CREATE TABLE target_stock (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    base_date DATE NOT NULL,                -- 분석 일자
    stock_code VARCHAR(10) NOT NULL,
    stock_name VARCHAR(50),
    
    /* Analysis Scores (Raw Data for AI Review) */
    news_score INT,                         -- Sentinel 점수
    tech_score INT,                         -- Vector 점수
    fund_score INT,                         -- Axiom 점수
    flow_score INT,                         -- Sonar 점수
    nexus_score INT,                        -- 종합 점수
    
    /* Decision */
    decision VARCHAR(20),                   -- BUY, WATCH, REJECT
    risk_level VARCHAR(10),                 -- HIGH, MEDIUM, LOW
    strategy_mode VARCHAR(20),              -- 사용자의 당시 성향 (Aggressive 등)
    
    /* Dynamic Pricing */
    original_target_price DECIMAL(10, 0),   -- 장전 수립 목표가
    original_stop_loss DECIMAL(10, 0),      -- 장전 수립 손절가
    current_target_price DECIMAL(10, 0),    -- 장중 조정 목표가 (Trailing)
    current_stop_loss DECIMAL(10, 0),       -- 장중 조정 손절가
    
    /* Status */
    status VARCHAR(20),                     -- WATCHING, TRADED, ENDED
    nexus_reason TEXT,                      -- AI 선정 사유 요약
    created_at TIMESTAMP,
    
    CONSTRAINT uk_target_day_code UNIQUE (base_date, stock_code)
);

/* 4. 매매 실행 로그 (Execution - The Wallet) */
CREATE TABLE trade_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    target_stock_id BIGINT,                 -- FK: 어떤 전략에 의한 매매인가?
    order_id VARCHAR(20),                   -- 키움 주문번호
    stock_code VARCHAR(10) NOT NULL,
    
    /* Execution Details */
    trade_type VARCHAR(10),                 -- BUY, SELL
    order_price DECIMAL(10, 0),             -- 주문가
    filled_price DECIMAL(10, 0),            -- 체결가 (평균)
    quantity INT,
    
    /* Quality Metrics */
    slippage_rate DECIMAL(5, 2),            -- 슬리피지 발생률 (%)
    market_score_snapshot INT,              -- 주문 시점의 시장 점수 (MarketHistory 참조)
    
    /* Status */
    status VARCHAR(20),                     -- PENDING, FILLED, PARTIAL, CANCELLED
    agent_msg VARCHAR(255),                 -- Aegis/Vector의 주문 코멘트 (예: "20일선 붕괴로 손절")
    executed_at TIMESTAMP,
    
    FOREIGN KEY (target_stock_id) REFERENCES target_stock(id)
);

/* 5. 가상 보유 시뮬레이션 (Phase 2 Prep) */
/* 15:20 강제 청산 시, 만약 홀딩했다면 익일 시초가가 어땠을지 기록 */
CREATE TABLE virtual_overnight_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    target_stock_id BIGINT,                 -- FK
    close_price_day1 DECIMAL(10, 0),        -- 당일 종가 (강제청산가)
    open_price_day2 DECIMAL(10, 0),         -- 익일 시초가
    potential_profit_rate DECIMAL(5, 2),    -- 오버나잇 했을 경우 예상 수익률
    recorded_at TIMESTAMP,
    
    FOREIGN KEY (target_stock_id) REFERENCES target_stock(id)
);

/* 6. 매매 일지 & 회고 (Review) */
CREATE TABLE journal (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    
    /* Daily Summary */
    total_profit_loss DECIMAL(18, 2),
    win_rate DECIMAL(5, 2),                 -- 승률
    trade_count INT,
    
    /* AI Feedback */
    best_trade_log_id BIGINT,               -- 최고의 매매 (TradeLog FK)
    worst_trade_log_id BIGINT,              -- 최악의 매매 (TradeLog FK)
    ai_review_content TEXT,                 -- Nexus의 총평 (Markdown)
    improvement_points JSON,                -- 향후 개선점 (Action Items)
    
    created_at TIMESTAMP
);

/* 7. 설정 (Config) */
CREATE TABLE user_setting (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    strategy_mode VARCHAR(20) DEFAULT 'NEUTRAL',
    re_entry_allowed BOOLEAN DEFAULT TRUE,
    max_loss_per_trade DECIMAL(5, 2) DEFAULT 3.0,
    api_rate_limit_config JSON              -- API별 제한 속도 설정
);

CREATE TABLE rss_feed_config (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(50),
    url VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE
);
```

---

## 8. Implementation Roadmap (Integrated Step-by-Step)

**Phase 1: Foundation (인프라 및 스키마 구축)**

* [x] **Backend Core:** Java 21 `VirtualThreadExecutor` 및 `RestClient` 설정.
* [x] **Persistence:** H2(Dev)/PostgreSQL(Prod) + `HikariCP` + JPA 설정.
  * **Schema:** `Account`, `TargetStock`, `TradeLog`, `Journal` 및 **`UserSetting`, `RssFeedConfig`** 엔티티 매핑.
* [x] **Kiwoom Connectivity:**
  * `WireMock` 기반 API Mock Server 구축 (TDD 환경).
  * `KiwoomClient` 구현: `au10001`(Auth) 및 공통 헤더(`cont-yn`) 처리.
* [x] **Frontend Setup:** React 19 + Vite + **FSD 아키텍처 폴더링** + Shadcn UI 설치.

**Phase 2: The 7-Agents & Strategy (두뇌 및 판단 로직)**

* [x] **AI Core:** LangChain4j + Gemini API (`gemini-2.5-flash`) 연동.
* [x] **Sentinel (News):**
  * **Active:** Naver Search API 연동 (장전 주도주 발굴).
  * **Passive:** `rss_feed_config` 테이블 연동 동적 RSS 수집기.
* [x] **Sonar (Flow):** `ka10008`(외인), `ka10040`(거래원) 분석 로직 구현.
* [x] **Nexus (Brain):**
  * `UserSetting`의 성향(Aggressive/Neutral/Stable)을 반영한 **매수 승인 및 목표가/손절가 동적 계산** 로직.
  * **Re-entry:** `TradeLog` 조회 기반 재진입 쿨타임 체크 로직.
* [x] **API:** 프론트엔드용 `Settings` (전략 변경, RSS 관리) CRUD API 구현.

**Phase 3: Real-time Surveillance & Gatekeeping (감시 체계)**

* [x] **Resonance (Gatekeeper):** Global Index 및 시장 점수 산출 로직. (점수 미달 시 진입 차단).
* [x] **Surveillance Engine:** `Rome` 라이브러리 기반 DART 공시 1분 주기 폴링.
* [x] **Kill Switch:** '횡령/배임' 키워드 감지 시 `ApplicationEventPublisher`로 긴급 매도 이벤트 발행.

**Phase 4: Trading Engine (실시간 매매 심장)**

* [x] **WebSocket:** `ReactorNetty` 기반 `00`(체결), `0w`(프로그램), `0A`(호가) 수신 및 라우팅.
* [x] **Vector (Eye):** **NanoBananaCalculator** 구현 (이평선 수렴/발산 실시간 계산).
* [x] **Aegis (Wallet):**
  * **PEQ (Priority Execution Queue):** KillSwitch(P0) > 익절(P1) > 매수(P2) 우선순위 처리.
  * `0C` 호가 기반 슬리피지 제어 및 `kt10000`(주문)/`kt10003`(취소) 집행.
* [x] **Position Management:** 실시간 등락률 감시 및 **ATR 트레일링 스탑** 로직 적용.

**Phase 5: Frontend Integration & Verification (통합 및 검증)**

* [x] **Dashboard:**
  * `Recharts`: 캔들 차트 + 매매 타점 오버레이.
  * `React-TreeMap`: 포트폴리오 히트맵 시각화.
  * WebSocket 로그 뷰어 연동.
* [x] **Deep Analysis Page:** 종목 조회 시 AI 실시간 분석(지지/저항선) 결과 렌더링.
* [x] **Settings/Journal:** 전략 설정 변경 및 매매일지/AI 복기 뷰어 구현.
* [x] **Final Test:** 모의투자 환경 연결 및 장중 시나리오(급등/급락) 통합 테스트.
