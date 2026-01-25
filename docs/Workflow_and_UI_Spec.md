# 🎬 KAIROS: Decision Workflow & UI Specification (Final)

## 📌 개요

본 문서는 KAIROS 시스템의 **시간대별 의사결정 프로세스**, **Nexus의 AI 판단 로직**, **상세 트레이딩 알고리즘**, **데이터 처리 원칙**, 그리고 **반응형 UI/UX 상세 명세**를 정의합니다.
개발자(Antigravity)는 구현 시 본 문서의 시나리오와 로직을 헌법처럼 준수해야 합니다.

---

## 1. Daily Decision Workflow (The Scenario)

시스템의 하루 일과는 `ScheduleService`에 의해 아래 타임라인대로 기계적으로 수행됩니다.

### 🌅 Phase 1: 장전 준비 및 분석 (Morning Routine)

* **Time:** 08:00 ~ 08:30
* **Goal:** `TargetStock` 후보군 확정 (The Strategy).
* **Data Source:** **Naver Search API (Active)**

1. **System Check:** `ApiGatekeeper` 초기화. 키움 API 접속 상태 확인 및 토큰(`au10001`) 갱신.
2. **Discovery (Sentinel):** 네이버 뉴스 API(10 req/sec)를 통해 '특징주', '수주', '공시' 키워드로 당일 주도 테마를 발굴하고 1차 종목 리스트를 추출합니다. (이때 쿼터를 집중 소모)
3. **Filtering (Axiom):** 추출된 종목들의 재무제표(`ka10001`)를 조회합니다. 3년 연속 적자, 자본 잠식 등 '부실 기업'을 리스트에서 즉시 제거합니다.
4. **Trend Check (Sonar):** [전일 데이터] 남은 종목의 일별 수급(`ka10008`)을 조회하여 외국인/기관의 이탈 여부를 확인합니다. 외국인/기관의 '연속 순매수'가 없는 단순 테마주는 점수를 차감합니다.
5. **Setup Check (Vector):** 일봉 차트(`ka10081`)를 분석하여 정배열/수렴 구간인지 확인하고 기술적 점수를 산출합니다.
6. **Final Decision (Nexus):**
    * (상세 로직은 2. Decision Engine 참조) 위 4명의 리포트와 **사용자 설정(Aggressive/Neutral/Stable)**을 종합하여 최종 등급(Buy/Wait/Reject)과 **위험 등급(High/Med/Low)**을 결정 결정합니다.
    * **[Portfolio Balancing]:** 사용자 성향이 'Aggressive'라도, **'High Risk' 종목은 전체 포트폴리오의 50%를 넘지 않도록** 조절합니다. (Risk Management)
    * **[Dynamic Pricing]:** 종목별 재료 강도와 변동성(ATR)을 고려하여 **개별 목표가(TP)와 손절가(SL)**를 책정합니다. (예: 호재가 강력하면 익절 +5%, 잡주는 손절 -4% 등)
    * 최종 승인된 종목만 `TargetStock` 엔티티로 DB에 저장(`status=WATCHING`)합니다. (Gemini Pro 사용)

### ⏱️ Phase 1.5: 장전 동시호가 대응 (Pre-Market Check)

* **Time:** 08:40 ~ 08:59
* **Goal:** 갭(Gap) 보정 및 타겟 최종 조율.

1. **Gap Monitoring (Aegis):** `ka10029`(예상체결등락률) API를 통해 `TargetStock`들의 예상 시초가를 모니터링합니다.
2. **Filter Logic:** 예상 시초가가 **+15% 이상(과도한 갭상승)**이거나 **-5% 이하(악재 의심)**인 종목은 `status=SKIPPED`로 변경하여 당일 매매에서 제외합니다.
3. **Ready:** 08:55분부터 실시간 시세(`00`, `0w`, `0A`) 구독을 시작합니다.

### ☀️ Phase 2: 장중 실시간 대응 (Intraday Execution)

* **Time:** 09:00 ~ 15:00
* **Data Source:** **RSS Feeds (Passive) + WebSocket**
    > 1. **Risk Monitor (RSS):** 네이버 API 호출을 최소화하고, 무료인 RSS(DART, 주요 언론사)를 1분 주기로 폴링하여 악재를 감시합니다.
    > 2. **Deep Verify (Optional):** RSS에서 설명이 부족한 급등/급락 발생 시에만 **예외적으로 네이버 검색 API를 호출**하여 구체적인 사유를 찾습니다.
    > 3. **Execution:** `Vector`(차트)와 `Sonar`(수급) 신호에 따라 매매 수행.
* **Goal:** 확실한 수익 챙기기(Scalping) + 추세 끝까지 먹기(Trend Following) 동시 추구.

1. **Market Gatekeeping (Resonance):**
    * **[Global Watch]:** 나스닥 선물, 환율, KOSPI/KOSDAQ 지수를 1분 단위로 체크합니다.
    * **[Circuit Breaker]:** 시장 심리 점수(Market Score)가 사용자 설정 기준(Stable 50 / Neutral 40 / Aggressive 30) 미만이거나 지수가 급락(-2%) 중이면 SystemStatus를 RiskOff로 전환하고 **신규 매수를 전면 차단**합니다.
2. **Signal Trigger (Vector + Sonar):**
    * **Vector (The Eye):**
          * **Java Layer:** 실시간 틱/분봉 데이터를 연산하여 `NanoBanana` 패턴(수렴+폭발)을 0.01초 내에 감지.
          * **AI Layer:** Java가 감지한 패턴과 **호가창 스냅샷(`0D`)**을 Gemini Flash에게 전송하여, 허매수 여부 및 정밀 진입가를 산출.
    * **Sonar (The Flow):** 패턴 완성 순간, 해당 종목의 **실시간 프로그램 수급(0w)**과 **체결강도**를 확인합니다.
    * **Event: 두 조건(패턴+수급) 충족 시 AnalysisResult (매수 권고)를 Nexus에게 전송합니다.
3. **Strategy Check (Nexus - The Brain):**
    * **Re-entry Policy:**
          * `TradeLog`를 조회하여 당일 매매 이력을 확인합니다.
          * 만약 매매 이력이 있다면, 설정된 **'재진입 허용 여부'** 및 사용자 성향(Aggressive/Neutral/Stable)에 따른 **'쿨타임'**을 체크하여 진입 여부를 결정합니다.
          * 조건 불만족 시 신호를 기각(Reject)합니다.
    * **Approval:** 승인 시 `ExecutionOrder(BUY)` 객체를 생성하여 Aegis에게 전달합니다.
4. **Entry Execution (Nexus → Aegis):**
    * **Nexus (The Brain):**
        * 매수 신호 발생 시 Resonance의 승인을 확인하고, 사용자 성향(Aggressive/Neutral/Stable)에 따라 최종 `ExecutionOrder(BUY)` 객체를 생성하여 Aegis에게 전달합니다. (직접 API를 호출하지 않습니다.)
    * **Aegis (The Wallet - Java Logic):**
        * 전달받은 주문을 **PEQ(Priority Execution Queue)**의 P2(Normal) 등급으로 등록합니다.
        * **[Step 1: Validation]** `ApiGatekeeper`로부터 실행 토큰을 확보하고, `kt00004`(계좌평가)를 조회하여 **설정된 비중(예: 종목당 100만원)에 따른 주문 가능 수량**을 계산합니다.
        * **[Step 2: Pricing]** `0C`(우선호가) 데이터를 조회하여, 현재 매도 1호가와 매수 1호가의 스프레드를 계산합니다.
            * *Java Rule:* 스프레드가 **0.5% 미만**인 정상적인 호가 상황일 때만 **매도 1호가(Ask 1)**로 지정가 주문을 확정합니다. (과도한 스프레드 발생 시 대기)
        * **[Step 3: Action]** 키움 API(`kt10000`) 전송 및 `TradeLog` 기록.
5. **Position Management (Vector → Aegis):**
    * **Vector**는 TargetStock(`status=BOUGHT`) 상태인 종목의 실시간 가격을 감시하며, 보유 종목에 대해 Nexus가 설정해준 **개별 목표가/손절가**를 기준으로 아래 **3단계 청산 로직**을 적용합니다. (3.2절 규칙 적용)
    * **Step A [안전마진 확보]:** 수익률이 **목표가(Target Price)**에 도달하면 `ProfitTake(50%)` 신호를 보냅니다. (절반 이익 실현)
    * **Step B [추세 추종]:** 남은 50% 물량은 **Trailing Stop**을 적용합니다. 고점 대비 **ATR(변동폭)의 2배** 이상 하락 시 `SellSignal(All)`을 보냅니다.
    * **Step C [손절 방어]:** 손절가 이탈 (Hard Cut) 하거나, **20일 이평선 붕괴** (Trend Broken) 시 즉시 `StopLoss(All)` 신호를 보냅니다.
    * 관련 매도 신호는 즉시 Nexus에게 전달하고, Nexus는 `ExecutionOrder(SELL)`을 Aegis에게 하달합니다.
6. **Execution & Correction (Aegis):**
    * **청산 집행:** Vector의 매도 신호(`ProfitTake` / `SellSignal`)를 받으면 즉시 매도 주문(`kt10001`)을 실행합니다.
    * **미체결 관리:** 매수/매도 주문 전송 후 **30초 내에 체결(00)**이 확인되지 않으면, `kt10003`(취소) 후 호가를 재산정하여 재주문(매도의 경우 시장가 전환)하거나 진입을 포기합니다. (Java 알고리즘)
7. **Risk Monitor (RSS):** 네이버 API 호출을 최소화하고, 무료인 RSS(DART, 주요 언론사)를 1분 주기로 폴링하여 악재를 감시합니다.
8. **Deep Verify (Optional):** RSS에서 설명이 부족한 급등/급락 발생 시에만 **예외적으로 네이버 검색 API를 호출**하여 구체적인 사유를 찾습니다.

### 🚨 Phase 3: 리스크 관리 (Surveillance Layer)

* **Time:** 09:00 ~ 15:20 (Always On)
* **Goal:** 돌발 악재 발생 시 즉각 청산.

1. **Surveillance (Sentinel):** DART 공시 RSS를 1분 주기로 폴링합니다. (Always On)
2. **Kill Switch:** 보유 종목에 대해 '횡령', '배임', '거래정지', '감자' 키워드 공시 감지 시 `KillSwitchEvent` 발행.
3. **Emergency Action (Aegis):** `KillSwitchEvent` 수신 즉시, **PEQ Priority 0(최우선 순위)**으로 해당 종목을 시장가 전량 매도합니다.

### 🌇 Phase 4: 장 마감 및 복기 (After Market)

* **Time:** 15:20 ~ 16:00
* **Goal:** 포지션 청산 및 학습.

1. **Liquidation (Aegis- Java):**
    * 15:20 동시호가 진입 전, 보유 중인 모든 종목을 시장가로 매도(`kt10001`)하여 오버나잇 리스크를 제거합니다.
2. **Revoke Token:** 보안을 위해 키움 API 토큰 폐기(`au10002`).
3. **Execution Review (Aegis - AI Mode):**
    * 장중에는 기계처럼 주문만 냈던 Aegis가 분석가 모드로 전환됩니다.
    * 당일 체결된 주문 중 **슬리피지(Slippage)가 0.5% 이상 발생한 건**을 추출합니다.
    * **Gemini Flash**에게 주문 당시의 호가(`0C`) 상황을 전달하고, "주문이 늦었는지, 호가가 얇았는지" 원인을 분석받습니다.
4. **Journaling (Nexus):**
    * Aegis의 실행 분석 결과와 Nexus의 전략 적중률을 종합하여 `Journal` 엔티티를 생성하고 DB에 저장합니다.
    * 당일 `TradeLog`와 수익률을 집계하여 `Journal`을 작성합니다. 손실 거래에 대해 "왜 잃었는가"를 분석하여 기록합니다.

### ⚠️ Phase 5: 예외 상황 대응 (Exception & Recovery)

* **Goal:** 시스템 장애 발생 시 자산을 보호하고 신속하게 정상 궤도로 복귀합니다.
* **Handler:** `SystemMonitorService` (Daemon Thread)

#### 5.1 Kiwoom API 연결 끊김 (Network Error)

1. **Detection:**
    * API 호출 시 `return_code: -100` (연결 실패) 수신.
    * WebSocket Heartbeat(1분 주기) 응답 없음.
2. **Immediate Action (Safety Freeze):**
    * **System Status:** `EMERGENCY_STOP` 전환.
    * **Aegis:** `PEQ`의 모든 신규 주문(P2) 대기열 **동결(Pause)**.
3. **Auto-Recovery Procedure (재접속 시도):**
    * `ApiGatekeeper`는 5초 간격으로 `au10001` (로그인 정보 상세) 호출 시도 (최대 5회).
    * 재접속 성공 시:
        1. **Sync Balance:** `kt00004`를 호출하여 서버 장부와 실제 계좌 잔고의 정합성을 검증합니다.
        2. **Re-subscribe:** `RealtimeService`가 관리하던 구독 목록(TargetStock)에 대해 WebSocket 재등록 패킷 전송.
        3. **Resume:** 상태를 `RUNNING`으로 변경하고 PEQ 재가동.
    * 재접속 5회 실패 시: 관리자에게 **SMS/Slack 긴급 알람** 발송 및 프로세스 **Graceful Shutdown**.

#### 5.2 주문 미체결 및 정합성 불일치 (Order Mismatch)

1. **Scenario:** 주문(`kt10000`)은 성공했으나, 체결 통보 WebSocket(`00`)이 누락된 경우.
2. **Detection:** `TradeLog` 상태가 60초 이상 `PENDING`인데, `kt10075` (미체결 내역) 조회 결과 리스트에 없는 경우 (이미 체결됨).
3. **Resolution:**
    * Aegis는 1분 주기로 `kt00005` (체결 잔고)를 폴링하여 `TradeLog`의 상태를 강제로 동기화(`FILLED`)합니다.
    * 누락된 체결 건에 대해서는 `SystemLog`에 "WebSocket Packet Loss Detected" 경고를 남깁니다.

#### 5.3 데이터 왜곡 감지 (Data Anomaly)

1. **Scenario:** 현재가 50,000원인 종목이 순간적으로 0원이나 100,000원으로 들어오는 데이터 오염(Glitch).
2. **Filter:** `Vector` 에이전트의 Java 전처리 로직에서 **"직전 가격 대비 ±20% 이상 변동"** 시 해당 틱 데이터를 **노이즈(Noise)**로 간주하고 무시(Drop)합니다.

---

## 2. Decision Engine (The Brain & The Wallet)

Nexus는 단순 집계자가 아닌, **User Strategy에 따라 판단 기준을 유연하게 바꾸는 AI 의사결정체**입니다.

### 🧠 2.1 Context Assembly & Inference

Nexus는 4인(Sentinel, Axiom, Vector, Sonar)의 리포트를 종합하여 **매수 여부**뿐만 아니라 **종목의 위험 등급**을 고려하고, **사용자 설정(UserConfig)**을 프롬프트에 주입하여 판단합니다.

* **Prompt Template:**
    > "당신은 펀드매니저 Nexus다. 현재 사용자의 투자 성향은 **'${StrategyMode}'**이다.
    > 아래 4명의 분석가 보고서를 바탕으로 이 종목을 매수할지 결정하라.
    >
    > [분석가 의견]
    > * 뉴스(Sentinel): ${NewsScore} / ${NewsSummary}
    > * 재무(Axiom): ${FundStatus}
    > * 차트(Vector): ${TechScore}
    > * 수급(Sonar): ${FlowScore}
    >
    > [판단 기준]
    > 1. **Aggressive Mode:** Vector(차트)와 Sentinel(재료) 점수를 가중치 1.5배로 계산하라. 리스크가 있어도 기술적 패턴이 좋으면 승인하라.
    > 2. **Neutral Mode:** 펀더멘털과 기술적 분석의 균형을 중시(50:50). 4명 중 3명 이상이 긍정적이어야 승인하라.
    > 3. **Stable Mode:** Axiom(실적)이 Fail이면 무조건 기각하라. Sonar(수급)가 받쳐주지 않으면 차트가 좋아도 보류하라."
    >
    > 위 기준에 따라 다음 3가지를 결정하라:
    > 1. **Decision:** BUY / WATCH / REJECT
    > 2. **Risk Level:** HIGH (테마주/변동성큼) / MEDIUM / LOW (실적주/수급안정)
    > 3. **Reason:** 한 줄 요약."

### ⚙️ 2.2 User Strategy Configuration & System Configuration (사용자 설정)

사용자는 `Settings` 페이지에서 성향을 변경할 수 있으며, 이는 Nexus의 판단 로직과 API 사용 정책에 영향을 미칩니다. Nexus는 이를 반영하여 **종목별 목표/손절가를 동적으로 산정**합니다.

**Configuration**

| 설정 항목 | 옵션 / 설명 |
| :--- | :--- |
| **Strategy Mode** | Aggressive / Neutral / Stable (Nexus 프롬프트에 주입) |
| **AI Model** | **Paid (Pay-as-you-go)**: 속도 제한 없이 7인 에이전트 풀 가동. |
| **Rate Limit** | **Kiwoom:** 4 req/sec (고정), **Gemini:** 1,000 req/min (Cost Guard) |
| **Re-entry** | 허용 (쿨타임 10분/30분) / 금지 |

* **Resource Management:**
  * **Kiwoom:** 초당 4회 제한이 꽉 찰 경우, `Buy` 주문이 `Search` 조회보다 우선순위를 가집니다.
  * **Gemini:** 유료 모델 사용으로 쿼터 걱정은 없으나, 불필요한 호출을 줄이기 위해 Java 로직(NanoBanana)의 1차 필터링은 유지합니다.

**Nexus의 판단 로직**

| 모드 | Aggressive (공격형) | Neutral (중립형) | Stable (안정형) |
| :--- | :--- | :--- | :--- |
| **판단 가중치** | 모멘텀(차트/뉴스) 70% : 펀더멘털/수급 30% | 모멘텀(차트/뉴스) 50% : 펀더멘털/수급 50% | 모멘텀(차트/뉴스) 30% : 펀더멘털/수급 70% |
| **기본 손절폭** | -4.0% ~ -5.0% (여유) | -3.0% (표준) | -1.5% ~ -2.0% (엄격) |
| **기본 익절폭** | +5.0% 이상 + Trailing | +3.0% + Trailing | +2.0% (짧게 확정) |
| **재진입(Re-entry)** | 허용 (쿨타임 10분) | 허용 (쿨타임 30분) | 금지 (1일 1매매) |
| **Resonance 차단** | 시장 점수 30점 미만 시 | 시장 점수 40점 미만 시 | 시장 점수 50점 미만 시 |

### ⚡ 2.3 Aegis Execution Hybrid Logic & PEQ System

Aegis는 주문 요청이 쇄도할 때를 대비해 **우선순위 큐(Priority Queue)**를 운영합니다.

| 우선순위 (Priority) | 주문 유형 | 설명 |
| :--- | :--- | :--- |
| **P0 (Critical)** | **Kill Switch / Stop Loss** | 악재 발생 및 손절은 그 무엇보다 먼저 처리 (시장가). |
| **P1 (High)** | **Profit Take (익절)** | 수익 확정 주문. |
| **P2 (Normal)** | **New Buy (신규 매수)** | 여유 자금이 있을 때만 후순위로 처리. |

* **Rule:** P0 주문이 큐에 들어오면, 처리 중이던 P2(매수) 프로세스를 즉시 중단하고 P0부터 처리합니다.

또한 Aegis는 두 가지 모드로 작동하며, 시스템의 속도와 개선을 동시에 담당합니다.

| 구분 | Runtime Mode (장중) | Review Mode (장후) |
| :--- | :--- | :--- |
| **주체** | **Java Algorithm** | **AI Agent (Gemini Flash)** |
| **목표** | Latency 최소화, 정확한 주문 | 실행 품질 분석, 개선점 도출 |
| **입력** | 실시간 호가, 잔고, 예수금 | `TradeLog` (주문/체결 시각, 가격) |
| **행동** | 호가 계산, API 주문 전송 | 슬리피지 원인 분석 리포트 생성 |
| **Rate Limit** | **Strict (4 req/sec)** | **High (1,000 req/min)** |

---

## 3. Core Trading Logic (Algorithm)

개발자는 아래 알고리즘을 `domain.technical.service`에 구현해야 합니다.

### 🍌 3.1 The NanoBanana Algorithm (Entry)

주가가 수렴(Squeeze) 후 발산(Blast)하는 초입을 포착하는 로직입니다.

* **Timeframe:** 1분봉(1-Minute Candle) 또는 3분봉 기준.

1. **Setup (Squeeze):**
    * **이동평균선 밀집:** 5일, 20일, 60일 이평선의 이격도(Disparity)가 **3% 이내**에 모여 있어야 합니다.
    * `Max(MA5, MA20, MA60) / Min(MA5, MA20, MA60) < 1.03`
2. **Trigger (Blast):**
    * **가격 돌파:** 현재가가 `Max(MA5, MA20, MA60)` (가장 높은 이평선)을 **1% 이상 상향 돌파**해야 합니다.
    * **거래량 폭발:** 실시간 분봉 거래량이 **전일 동시간대 평균 대비 200% 이상** 터져야 합니다.
3. **Validation (Sonar):**
    * 해당 시점의 프로그램 순매수(`0w`)가 **'매수 우위'**여야 합니다. (음수면 가짜 돌파로 간주)

### 🛡️ 3.2 Trading Rules (Exit)

"가격"이 아니라 "추세"가 꺾일 때 매도합니다.

| 구분 | 조건 (Dynamic Condition) | 행동 (Action) | 의도 (Rationale) |
| :--- | :--- | :--- | :--- |
| **안전 마진 (1차)** | 수익률이 **목표가(Target Price)** 도달 시 | **50% 분할 매도** | 일단 수익을 확정하여 심리적 안정 확보 |
| **트레일링 스탑 (2차)** | (익절 후) `최고가 - (ATR * 2.0)` 하락 시 | **잔량 전량 매도** | 추세가 꺾이기 전까지 수익 극대화 |
| **추세 붕괴 (손절)** | 현재가가 **20일 이동평균선**을 하향 돌파 (Dead Cross) | **시장가 전량 매도** | 상승 추세가 끝났으므로 미련 없이 탈출 |
| **하드 컷** | (안전장치) 매수가 대비 **-3.0%** 도달 | **시장가 전량 매도** | 급락 시 최소한의 자본 보전 (마지노선) |
| **하드 컷 (손절)** | 수익률이 **손절가(Stop Price)** 이탈 시 | **시장가 전량 매도** | 최후의 보루 (기계적 손절) |
| **오버나잇** | 15:20 장 마감 임박 | **시장가 전량 매도** | 리스크 제로화 (Day Trading) |

* **ATR (Average True Range):** 주가의 평균적인 변동폭. 변동성이 클 때는 손절폭을 넓게, 작을 때는 좁게 가져가기 위해 사용.

#### [Overnight & Re-entry Policy (Phase #1 Rule)]

1. **No Overnight:**
    * 아무리 차트가 좋고 상한가가 예상되어도, **15:20**이 되면 `Aegis`는 모든 포지션을 **시장가로 강제 청산**합니다.
    * *Future Prep:* 단, `TargetStock` 테이블에 `virtual_hold=true` 플래그를 남겨, 만약 보유했다면 익일 시초가에 어떤 결과가 나왔을지 로그(`VirtualTradeLog`)에 기록하여 Phase #2 데이터를 확보합니다.

2. **Dynamic Re-entry:**
    * 익절/손절 후 주가가 다시 `NanoBanana` 패턴을 만들면 재진입을 허용합니다.
    * 이때, **이전의 목표가/손절가는 폐기**하고, 현재 시점의 이평선과 매물대를 기준으로 `Vector`와 `Nexus`가 가격을 **재산정(Recalculate)**해야 합니다.

---

## 4. Data Persistence Strategy (3-Rule)

데이터의 성격에 따라 저장소와 처리 방식을 엄격히 구분합니다.

| 구분 | 대상 데이터 | 처리 방식 | 원칙 (Principle) |
| :--- | :--- | :--- | :--- |
| **Database** (Memory) | `TargetStock` (분석결과), `TradeLog` (매매내역), `Journal` (일지), `RssFeedConfig` (설정) | **Insert/Update** | **"재가공된 결과(Insight)만 저장한다."** (단순 시세 데이터 저장 금지) |
| **AI LLM** (Logic) | 뉴스 본문, 재무제표 주석, 복잡한 차트 패턴, 매매 복기 | **Inference (Prompt)** | **"판단(Judgment)이 필요한 것만 묻는다."** (PER 계산 등 단순 연산은 Java로 처리) |
| **Ext API** (Fact) | 현재가, 호가, 예수금 잔고, 미체결 내역 | **Real-time Fetch** | **"팩트(Fact)는 저장하지 않고 항상 조회한다."** (DB의 잔고 데이터는 신뢰하지 않음) |

---

## 5. UI/UX Specification (Multi-View Application)

시스템은 **좌측 사이드바(Sidebar)**를 통해 4개의 메인 뷰(Dashboard, Deep Analysis, Journal, Settings)를 전환하는 **반응형(Responsive)** 구조입니다.

**Target Devices:** Desktop(4K), Tablet(iPad), Mobile(iPhone).
**Theme:** Dark Mode Default (Shadcn UI).

### 5.1 Global Layout (Common)

* **Sidebar (Left):**
  * 메뉴: [Dashboard], [**Deep Analysis**], [Journal], [Settings].
  * *Mobile:* 하단 탭바(Bottom Tab)로 변형.
* **Status Bar (Top):**
  * 실시간 KOSPI/KOSDAQ 지수 및 등락률.
  * **Account Summary:** 총 자산, 당일 손익(금액/%), D+2 예수금 (API를 통해 5초 단위 갱신).
  * **System Status:** 각 에이전트(Sentinel, Aegis 등)의 생존 신고(Heartbeat) 상태 표시.

---

### 5.2 View Details

#### **A. 📈 Dashboard (Main Trading Screen)**

**3-Column Grid Layout** (Mobile: Vertical Stack)

* **Left Panel: Target Watchlist & Portfolio**
  * **Tab 1 (Targets):** Sentinel/Nexus가 선정한 당일 공략 종목 리스트. (클릭 시 Center Panel 연동)
  * **Tab 2 (Portfolio):** 현재 보유 중인 종목 리스트와 실시간 수익률.
* **Center Panel: Analysis & Visualization**
  * **Top (Chart):** TradingView 스타일 캔들 차트. NanoBanana 타점(매수/매도 화살표) 오버레이.
  * **Middle (Heatmap):** 현재 내 계좌의 종목별 비중을 나타내는 **Treemap Chart**. (수익 중이면 빨강, 손실이면 파랑)
  * **Bottom (AI Report):** "Nexus가 이 종목을 선정한 이유(펀더멘털+뉴스+수급)"를 Markdown으로 렌더링.
* **Right Panel: Real-time Logs & Control**
  * **Logs:** WebSocket 스트리밍 로그 ("10:23 [Sonar] 삼성전자 프로그램 매수세 포착!").
  * **Control:**
    * **[KILL SWITCH]:** (Red Button) 클릭 시 모든 보유 종목 시장가 전량 매도.
    * **[Manual Buy/Sell]:** (Modal) 비상 시 수동 주문 기능.

#### **B. 🔍 Deep Analysis (Stock Deep Dive)**

사용자가 특정 종목을 입력하면 5대 에이전트가 즉시 분석하여 결과를 보여주는 페이지입니다.

* **Input:** 종목코드/명 검색창.종목코드 입력 -> 5대 에이전트 병렬 호출.
* **Chart View:**
  * 차트 위에 **AI가 계산한 지지선(Support)과 저항선(Resistance)을 가로줄**로 표시.
  * 매수 권장 구간(Buy Zone)과 손절 라인을 색상 영역으로 하이라이팅.
  * "Nexus Commentary: 현재 구름대 상단을 돌파 시도 중입니다." 텍스트 출력.
* **Score Card:** 5대 에이전트 각각의 점수와 종합 점수(0~100) 게이지 차트.

#### **C. 📝 Journal (Review & History)**

매매가 끝난 후 반성하고 기록하는 공간입니다.

* **Calendar View:** 날짜별 수익금액이 표시된 달력. (수익: 빨강, 손실: 파랑)
* **Trade Detail Table:** 해당 일자의 매매 내역(진입가, 청산가, 수익률, 보유시간).
* **AI Feedback UI:** Aegis가 분석한 **"오늘의 슬리피지 분석"**과 Nexus의 **"전략 회고"**가 Markdown 카드로 표시됩니다.
  * **AI의 한마디:** Nexus가 분석한 "오늘의 패인/승인".
  * **Action Item:** "내일은 Vector의 이평선 수렴 기준을 1.5%에서 1.2%로 좁히세요." 같은 구체적 제안 표시.

#### **D. ⚙️ Settings (Admin & Configuration)**

시스템의 두뇌(Nexus)와 심장(Aegis)을 제어하는 통제실입니다. `user_setting` 및 `rss_feed_config` 테이블과 1:1로 매핑됩니다.
모든 숫자 입력 필드는 **[Slider Bar]**와 **[Number Input]**이 동기화된 형태(Dual Control)로 제공되어야 합니다.

**1. Strategy Profile (전략 설정)**

* **Mode Selection:** (Radio Group)
  * 🔴 **Aggressive (공격형):** High Risk/High Return. 뉴스/차트 가중치 ↑.
  * 🟡 **Neutral (중립형):** Balance. 밸런스 투자.
  * 🟢 **Stable (안정형):** Low Risk. 펀더멘털 중시.
* **Re-entry Policy:** (Toggle Switch)
  * [ON]: 익절/손절 후 재진입 허용 (쿨타임 적용).
  * [OFF]: 1일 1종목 1매매 원칙 고수.
* **Risk Management:** (Slider + Input)
  * **Max Loss per Trade:** `-3.0`% (손절 기준선).
    * *Range:* `-1.0%` ~ `-10.0%`
    * *Step:* `0.1%`

**2. System & API Governance (시스템 설정)**

* **API Rate Limits:** (Slider + Input) - *서버 재시작 없이 즉시 반영(Hot Reload)*
  * **Kiwoom (req/sec):** [ 4 ]
    * *Range:* `1` ~ `5` (5 초과 시 계좌 동결 위험 경고 표시)
  * **Naver (req/sec):** [ 10 ]
    * *Range:* `1` ~ `20`
  * **Gemini (req/min):** [ 1000 ]
    * *Range:* `100` ~ `2000`

**3. Data Source Manager (RSS)**

* **Feed List:** (Table with CRUD)
  * Columns: [Source Name], [URL], [Category], [Active Status(Toggle)]
  * Action: [+ Add Feed], [Delete], [Test Connection]
  * *Example:* DART 공시 채널을 일시적으로 끄거나, 새로운 뉴스 소스를 추가.

**4. System Controls (Danger Zone)**

* **[Force Liquidation]:** (Red Button, Double Confirm) 보유 전 종목 시장가 매도.
* **[Reset System]:** (Button) `TargetStock`, `TradeLog` 캐시 초기화 (장 시작 전 사용).
