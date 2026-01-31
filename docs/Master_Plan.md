# 🚀 Project KAIROS: 통합 개발 마스터 플랜 (Final)

## 📌 개요

본 문서는 백엔드(Spring Boot 3.5+, Java 21)와 프론트엔드(React 19, Vite)를 아우르는 단일 진실 공급원(SSOT) 계획서입니다. 모든 개발은 **TDD(Test Driven Development)**를 원칙으로 하며, **가상 스레드(Virtual Threads)** 아키텍처를 엄격히 준수합니다.

---

## 🏗️ Phase 1: Foundation & Infrastructure (기반 구축)

**목표:** B/E와 F/E의 프로젝트 뼈대를 세우고, DB 연동 및 전역 트래픽 제어(Rate Limit) 환경을 구축합니다.

- [x] Backend: Spring Boot Init, Virtual Thread, JPA (PostgreSQL/H2).
- [x] Frontend: Vite React Init, Shadcn UI, Zustand/TanStack Query.

### 1.1 Backend Setup (Spring Boot)

- [x] **Project Init:** Spring Boot 3.5.7, Java 21 LTS 설정.
- [x] **Virtual Thread Config:** `ExecutorService.newVirtualThreadPerTaskExecutor()` 적용 및 `Tomcat` 설정.
- [x] **Global Traffic Governance (ApiGatekeeper):**
  - `Bucket4j`를 도입하여 API별 토큰 버킷 생성.
  - **Kiwoom:** 초당 4회 (Strict Mode - Ban 방지).
  - **Naver:** 초당 10회 (일 25,000회 준수).
  - **Gemini:** 분당 1,000회 (Pay-as-you-go / Cost Safety Cap).
  - 모든 외부 요청을 래핑하는 `Gatekeeper` 컴포넌트 구현.
- [x] **Persistence Layer:** PostgreSQL(Prod)/H2(Dev) 설정, `HikariCP`, JPA/Hibernate 연동.
- [x] **Schema Definition:** `PROJECT-Specification.md` 6.1절 기준 DDL 작성 및 엔티티 매핑.
  - `Account`, `TargetStock`, `TradeLog`, `Journal`, `UserSetting`, `RssFeedConfig`
- [x] **Architecture Check:** `backendrule.md`에 따른 Controller-Service-Repository 구조 및 `BaseResponse` 공통 포맷 구현.

### 1.2 Frontend Setup (React)

- [x] **Project Init:** Vite + React 19 + TypeScript + TailwindCSS.
- [x] **Architecture:** `frontendrule.md`에 따른 **FSD (Feature-Sliced Design)** 폴더 구조 적용.
- [x] **UI Library:** Shadcn UI 설치 및 테마 설정.
- [x] **State Management:** `Zustand` (전역 상태), `TanStack Query v5` (서버 상태) 설정.
- [x] **Router & Layout:** 기본 라우팅 및 레이아웃(Sidebar, Header) 구현.

---

## 🧠 Phase 2: The 7-Agent System (백엔드 핵심 로직)

**목표:** 5인의 분석가, 1인의 전략가, 1인의 집행관 로직을 구현합니다. 모델 이원화(Flash/Pro) 전략을 적용합니다. (`AI_Agent_List.md` 참조)

### 2.1 Foundation & Infrastructure (Core)

- [x] **Global Config:**
  - `Bucket4j` 빈(Bean) 설정. (Kiwoom: Strict, Gemini: High Throughput/Cost-Safe).
  - Google Cloud Project 연동 및 Billing 설정 확인.
- [x] **ApiGatekeeper 구현:**
  - `execute(ApiType type, Supplier<T> action)` 제네릭 메서드 구현.
  - Virtual Thread의 `park()`를 활용한 비동기 대기열(Backpressure) 처리.
- [x] **Mock Server:** `WireMock`을 사용하여 Kiwoom API 응답 모킹(TDD용).

### 2.2 External API Connectors (Via Gatekeeper)

- [/] **Kiwoom API Client:** `ApiGatekeeper`를 경유하는 `RestClient`.
  - [x] `au10001`(토큰), `ka10001`(기본정보) 등 기본 TR 구현.
  - [/] 주문 전송 메서드 실제 동작 (현재 안전장치 적용됨).
- [x] **Mock Server:** `WireMock`을 사용하여 Kiwoom API 응답 모킹(TDD용).
- [x] **Gemini Client:** LangChain4j 설정. `Flash`(분석가용)와 `Pro`(전략가용) 모델 Bean 분리.
- [x] **Naver Search Client:**
  - `ApiGatekeeper`에 일일 쿼터(25,000) 관리 로직 추가.
  - 장전/장중/장후 시간대별 가중치를 둔 호출 스케줄러 구현.
- [x] **RSS Feed Parser:**
  - `Rome` 라이브러리 기반의 비동기 Polling 서비스 구현.
  - DART 공시 전용 파서 및 키워드 필터링 로직 구현.

### 2.3 Intelligence Layer (Analysis & Strategy)

- [x] **Analysis Agents (5인):** Sentinel, Axiom, Vector, Resonance, Sonar 에이전트 클래스 및 프롬프트 구현.
- [x] **Sentinel:** `RssFeedConfig` 연동 동적 RSS 수집기 구현.
- [x] **Vector (Hybrid Analyst):**
  - **Java Layer:** 이평선 수렴도, 이격도, 거래량 급증률 계산 로직.
  - **AI Layer:** Java가 계산한 수치와 호가창 스냅샷(`0D`)을 해석하여 진입/목표가 산출.
- [x] **Investment Strategist (Nexus):**
  - 에이전트 리포트 취합 및 `TargetStock` 승인 로직 구현.
  - **[Logic]** `UserSetting`의 성향(Aggressive/Neutral/Stable)을 읽어와 종목별 `target_price`, `stop_loss_price` 및 **Risk Level** 동적 계산.
  - **[Re-entry]** `TradeLog` 조회 및 쿨타임 계산을 통한 중복 진입 필터링 로직 구현.
  - `JournalService`: 장 마감 후 `TradeLog` 분석 및 AI 회고 생성 로직.

### 2.4 Execution Layer (Aegis - Dual Mode)

- [/] **Core Engine (Runtime - Java):**
  - `TradeExecutionService`: AI 개입 없이 예수금 확인, 호가 스프레드 계산, 주문 전송을 1ms 내에 수행.
    - [/] 실제 주문 전송(`kt10000`) 부분은 `TODO` 상태 (Safety Lock).
  - `AccountManager`: 실시간 잔고 및 미체결 내역 동기화.
  - 예수금 확인 및 주문 집행, `Kill Switch` 발동 권한 구현.
- [x] **Analysis Engine (Post-time - AI):**
  - `PostTradeAnalyzer`: 장 마감 후(`ScheduleService`), 당일 매매 로그를 수집하여 Gemini Flash에게 회고를 요청.
  - **KPI:** 슬리피지(Slippage) > 0.5% 발생한 건들에 대한 원인 분석 리포트 생성.

---

## ⚡ Phase 3: Real-time Engine & Trading Loop (실시간 처리)

**목표:** WebSocket을 통한 실시간 데이터 수신과 NanoBanana 알고리즘을 연동합니다.

- [/] WebSocket (`00`, `0w`) 연동 및 NanoBanana 계산기 구현.
- [x] Kill Switch 이벤트 버스 구현.

### 3.1 WebSocket & Event Bus

- [/] **Kiwoom WebSocket:** `ReactorNettyWebSocketClient` 구현.
  - [x] `00`(체결), `0w`(프로그램), `1h`(VI) 수신 로직 구현.
  - [ ] 실제 서버 연결 (`websocketUrl` 변경 필요).
- [x] **Event System:** Spring `ApplicationEventPublisher`를 이용해 수신된 틱 데이터를 에이전트에게 전파.

### 3.2 Trading Strategy Implementation

- [x] **NanoBanana Calculator:** 5/20/60 이평선 수렴/발산 수치 계산 로직 (순수 Java 연산).
- [x] **Signal Trigger:** `Vector` 에이전트와 연동하여 매수/매도 시그널 생성.
- [x] **Kill Switch:** `Sentinel`(뉴스)의 DART 공시 감지 시 즉시 매도 로직 연결.
- [x] **Aegis Execution Engine:**
  - **PEQ (PriorityBlockingQueue) 구현:** Kill Switch(P0) > 익절(P1) > 매수(P2) 우선순위 처리 로직.
  - **Transaction:** 예수금 확인 및 호가 보정 주문(`kt10000`)과 동시에 `TradeLog` 적재 및 `Account` 잔고 차감(가계산).

### 3.3 Surveillance System (Sentinel)

- [x] **Dual-Track Monitoring:**
  - **Trend Detection (Naver):** '특징주', '수주' 키워드로 장전 주도주 리스트업.
  - **Risk Alert (RSS):** 장중 DART 공시 실시간 감시 및 `KillSwitchEvent` 발행.

---

## 🖥️ Phase 4: Frontend Integration (시각화 및 연동)

**목표:** 백엔드 API와 프론트엔드를 연결하여 사용자가 시스템을 제어하게 합니다.

- [x] **Layout & Navigation:** Sidebar(Dashboard, Journal, Settings) 및 Global Header(자산현황) 구현.
- [x] **Dashboard:**
  - Recharts(캔들), React-TreeMap(자산 히트맵) 적용.
  - WebSocket 로그 뷰어 구현.
- [x] **Journal Page:**
  - 매매일지 리스트 및 상세 보기(AI 피드백 포함) UI 구현.
- [x] **Settings Page:**
  - **RSS Feed 관리자:** React Hook Form을 이용한 RSS URL 추가/삭제 폼 구현.

### 4.1 Dashboard & Visualization

- [x] **Target Stock View:** 당일 추천 종목 및 에이전트별 점수 카드 UI (`EXT_API_Specification.md` 참조).
- [x] **Trading Chart:** `Recharts` 라이브러리로 캔들 차트 및 매매 타점 오버레이 구현.
- [x] **Portfolio Heatmap:** 보유 종목 현황 및 수익률 트리맵 시각화.
- [x] **Logs:** WebSocket 로그 뷰어 구현 (Throttling 적용).

### 4.2 Deep Analysis

- [/] **Deep Analysis Page:** 종목 조회 시 5대 에이전트 실시간 분석 결과 및 AI 지지/저항선 차트 렌더링.
  - [x] UI 구현 완료.
  - [/] API Mocking 연결 확인 필요.

### 4.3 System Control

- [/] **Manual Override:** 비상 시 수동 매도/매수 버튼 및 API 연동.
  - [x] UI 버튼 구현.
  - [ ] 백엔드 연동 테스트 (API TODO 해제 시 가능).
- [x] **Log & Journal:** 매매 일지 및 AI 복기 리포트 조회 화면 구현.
- [x] **Settings Page:**
  - **RSS Feed Manager:** React Hook Form을 이용한 RSS URL 추가/삭제.
  - **Strategy Profile:** 공격형/중립형/안정형 선택 UI.

---

## 🔧 Phase 5: Verification & Simulation (검증)

**목표:** Virtual Thread 환경에서 시스템의 안정성과 로직의 우월성을 검증합니다. (`Benchmark_Report.md` 참조)

### 5.1 TDD Coverage & Quality Gate (Code Level)

`build.gradle`에 **JaCoCo**를 설정하여 빌드 시 커버리지 기준 미달 시 배포를 원천 차단합니다.

- [ ] **Strict Coverage Rule 적용:**
  - **General Domain:** Line Coverage **80%** 이상.
  - **Execution Domain (`domain.execution`):** Line Coverage **95%** 이상 (자금 집행 로직의 무결성 보장).
- [ ] **Unit Testing:**
  - `Nexus`의 전략 분기(Aggressive/Neutral/Stable)별 판단 로직 전수 테스트.
  - `Vector`의 NanoBanana 알고리즘 수치 계산 정밀도 테스트 (소수점 처리).

### 5.2 System Integration Testing (통합 테스트)

실제 API를 호출하지 않고 `WireMock`을 사용하여 극한 상황을 가정하고 시스템이 죽지 않는지 테스트합니다.

- [ ] **Kiwoom API Mocking:**
  - `WireMock`을 사용하여 `au10001`(로그인), `kt10000`(주문) 등의 응답 지연(Latency) 및 에러(500, 429) 상황 시뮬레이션.
  - Rate Limiter(`Bucket4j`)가 초당 4회 제한을 정확히 지키며 큐잉(Queuing) 처리하는지 검증.
- [ ] **Scenario Test (JUnit 5):**
  - **[Scenario A: 급등주 포착]**
        1. WebSocket으로 주가 급등 및 거래량 폭발 데이터 주입.
        2. `Vector` 에이전트가 NanoBanana 시그널 발생 확인.
        3. `Nexus`가 매수 승인(`ExecutionOrder`) 생성 확인.
        4. `Aegis`가 예수금 체크 후 주문 요청 로그 생성 확인.
  - **[Scenario B: Kill Switch 발동]**
        1. RSS Mock 서버에서 '횡령' 키워드 뉴스 주입.
        2. `Sentinel`이 즉시 `KillSwitchEvent` 발행 확인.
        3. `Aegis`가 최우선 순위로 전량 매도 로직 수행 확인.

### 5.4 Integration Scenario Testing (Flow Level)

단순 단위 테스트를 넘어, `MarketSimulatorTest`를 구현하여 **[데이터 수신 -> 판단 -> 주문 -> 체결 -> 잔고 반영]**의 전체 사이클을 검증합니다.

- [ ] **Scenario A: The Happy Path (정상 매매)**
    1. `MockWebSocket`에서 주가 급등(NanoBanana 패턴) 데이터 주입.
    2. `Vector` 감지 → `Nexus` 승인 → `Aegis` 주문 요청 로그 확인.
    3. `WireMock`에서 `kt10000`(주문) 성공 응답 리턴.
    4. `MockWebSocket`으로 `00`(체결) 데이터 수신 시 `Account` 잔고 및 `TradeLog` 상태 변경 확인.
- [ ] **Scenario B: The Crisis (Kill Switch & Recovery)**
    1. `Sentinel`이 DART 공시(횡령) 감지 이벤트 발행.
    2. `Aegis`가 즉시 `Priority 0`으로 매도 주문 생성 확인.
    3. 매도 주문 전송 중 **Network Timeout** 발생 시뮬레이션.
    4. `ApiGatekeeper`의 재시도(Retry) 로직 동작 및 최종 실패 시 알람 발송 여부 검증.

### 5.5 System Integration Testing (WireMock)

실제 API를 호출하지 않고 `WireMock`을 사용하여 **장애 상황**을 시뮬레이션합니다.

- [ ] **Mock Server Scenarios:**
  - **Latency Injection:** 주문(`kt10000`) 응답이 2초 지연될 때 `Aegis`의 타임아웃 핸들링 검증.
  - **Rate Limit Exceeded:** `HTTP 429` 응답 시 `ApiGatekeeper`의 Backoff(대기 후 재요청) 로직 검증.
  - **Server Error:** `HTTP 500/502` 발생 시 시스템이 죽지 않고 에러 로그를 남기며 우회하는지 검증.

### 5.6 Algorithm Verification (과거 데이터 검증)

**Java Stream API**와 **Virtual Threads**를 사용하여 과거 데이터를 고속으로 재생(Replay)하고 로직을 검증합니다.

- [ ] **Backtest Service 구현:**
  - CSV로 저장된 과거 3개월치 분봉 데이터를 로딩.
  - `VirtualThread`로 100배속으로 데이터를 재생하며 `Vector`(차트)와 `Sonar`(수급)의 매수 시그널 발생 시점 기록.
  - **KPI:** 기존 보조지표(RSI, MACD) 대비 진입 시점이 얼마나 빠른지(Tick 단위) 비교.
- [ ] **AI Inference Validation:**
  - 과거 급등/급락 종목의 뉴스 데이터를 Gemini에게 전송하여, 당시 AI가 올바른 판단(Buy/Sell)을 내렸을지 정성적 평가.

### 5.7 Dry Run (실전 모의 투자)

키움증권 **모의투자 서버**에 접속하여 실제와 동일한 환경에서 테스트합니다.

- [ ] **Mock Server Connection:**
  - 접속 URL을 `https://mockapi.kiwoom.com`으로 변경.
  - 장중(09:00~15:20) 실제 자동 매매 가동.
- [ ] **Performance Monitoring:**
  - VisualVM을 연결하여 스레드 폭증(Pinning) 여부 및 힙 메모리 사용량 모니터링.
  - 주문 체결 후 WebSocket 잔고 업데이트(`kt00005` vs `Realtime`) 간의 지연 시간 측정.
