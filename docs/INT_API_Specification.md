# KAIROS Internal API Specification

*   **Version:** 2.0.0 (Final)
*   **Protocol:** REST over HTTP/1.1
*   **Base URL:** `/api/v1`
*   **Content-Type:** `application/json; charset=UTF-8`
*   **Authentication:** Bearer Token (JWT) required in `Authorization` header.

---

## 1. Dashboard & Account (자산 및 현황)

### 1.1 계좌 요약 조회
현재 계좌의 예수금, 총 자산, 당일 손익 정보를 반환합니다. `ApiGatekeeper`를 통해 키움 API 캐시 데이터를 제공합니다.

*   **Endpoint:** `GET /account/summary`
*   **Request Headers:**
    *   `Authorization`: Bearer {token}
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": {
        "accountNumber": "50561234-11",    // 실계좌번호 (마스킹 처리 권장)
        "totalAsset": 10500000,            // 총 추정 자산
        "deposit": 2500000,                // 예수금 (D+2)
        "d2Deposit": 2500000,              // 주문 가능 예수금
        "dailyProfitLoss": 150000,         // 당일 실현 손익 (비용 차감 후)
        "dailyProfitLossRate": 1.45,       // 당일 수익률 (%)
        "totalProfitLoss": 500000,         // 누적 실현 손익
        "totalReturnRate": 5.2,            // 누적 수익률 (%)
        "marketMoodScore": 75,             // Resonance가 산출한 시장 심리 점수 (0~100)
        "systemStatus": "RUNNING",         // RUNNING, PAUSED, ERROR
        "updatedAt": "2026-01-22T10:30:00"
      }
    }
    ```

### 1.2 보유 종목(잔고) 조회
현재 보유 중인 종목 리스트와 실시간 수익률을 반환합니다.

*   **Endpoint:** `GET /account/balance`
*   **Request Headers:**
    *   `Authorization`: Bearer {token}
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": [
        {
          "stockCode": "005930",
          "stockName": "삼성전자",
          "quantity": 10,                 // 보유 수량
          "averagePrice": 72000,          // 매입 평균가
          "currentPrice": 73500,          // 현재가
          "evaluationAmount": 735000,     // 평가 금액
          "purchaseAmount": 720000,       // 매입 금액
          "profitRate": 2.08,             // 수익률 (%)
          "todayProfit": 15000            // 당일 평가 손익
        }
      ]
    }
    ```

---

## 2. Target Stocks (AI Strategy)

### 2.1 당일 추천(Target) 종목 리스트
장전/장중 Nexus가 분석하여 선정한 종목 리스트입니다. **동적 가격 전략(Dynamic Pricing)** 정보가 포함됩니다.

*   **Endpoint:** `GET /stocks/target`
*   **Query Parameters:**
    *   `date`: `YYYY-MM-DD` (Optional, Default: Today)
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": [
        {
          "id": 101,                      // DB PK
          "stockCode": "035720",
          "stockName": "카카오",
          "nexusScore": 85,               // Nexus 종합 점수 (0~100)
          "decision": "BUY",              // BUY, WATCH, REJECT
          "riskLevel": "HIGH",            // HIGH, MEDIUM, LOW (비중 조절용)
          "status": "WATCHING",           // WATCHING, BOUGHT, SOLD, SKIPPED
          "priceStrategy": {
              "targetPrice": 55000,       // 목표가 (익절)
              "stopLossPrice": 51000      // 손절가
          },
          "summary": "카카오브레인 합병 이슈 및 외인 수급 연속성 포착", // 한줄 요약
          "createdAt": "2026-01-22T08:15:00"
        }
      ]
    }
    ```

### 2.2 종목 상세 AI 리포트
특정 종목에 대해 각 에이전트(Flash)와 Nexus(Pro)가 생성한 상세 분석 리포트(Markdown)입니다.

*   **Endpoint:** `GET /stocks/target/{stockCode}/report`
*   **Path Parameters:**
    *   `stockCode`: 종목코드 (예: 005930)
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": {
        "stockCode": "035720",
        "generatedAt": "2026-01-22T08:15:00",
        "scores": {
            "news": 90,
            "fundamental": 60,
            "technical": 85,
            "flow": 75
        },
        "nexusReport": "## 종합 전략: 적극 매수 (Risk: High)\n\n### 1. Sentinel (News)\n- **호재:** 카카오브레인 합병 소식...\n\n### 2. Vector (Tech)\n- 20일선 지지 확인 및 NanoBanana 패턴 초기...\n\n### 3. Nexus Decision\n- 현재 변동성이 크지만 수급이 뒷받침되므로 진입을 승인합니다. 단, 손절가는 -3%로 타이트하게 잡으십시오."
      }
    }
    ```

---

## 3. Trading & History

### 3.1 매매 로그 조회
시스템의 주문 접수, 체결, 취소 등의 이벤트 로그입니다.

*   **Endpoint:** `GET /trades/logs`
*   **Query Parameters:**
    *   `page`: Page number (Default: 0)
    *   `size`: Page size (Default: 20)
    *   `date`: `YYYY-MM-DD` (Optional)
    *   `type`: `ORDER`, `EXECUTION`, `CANCEL`, `ERROR` (Optional)
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": {
        "content": [
          {
            "logId": 501,
            "time": "09:30:05",
            "type": "EXECUTION",
            "side": "BUY",                // BUY, SELL
            "stockCode": "000660",
            "stockName": "SK하이닉스",
            "price": 180000,
            "quantity": 5,
            "message": "체결 완료 (주문번호: 10234)",
            "agent": "Aegis"
          }
        ],
        "totalPages": 5,
        "totalElements": 98
      }
    }
    ```

### 3.2 수동 주문 (비상용)
관리자가 시스템 개입을 위해 수동으로 주문을 넣습니다.

*   **Endpoint:** `POST /trade/manual`
*   **Request Body:**
    ```json
    {
      "stockCode": "005930",
      "side": "SELL",       // BUY, SELL
      "type": "MARKET",     // LIMIT(지정가), MARKET(시장가)
      "price": 0,           // 시장가일 경우 0, 지정가일 경우 가격
      "quantity": 10        // 수량
    }
    ```
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": {
        "orderId": "10235",
        "message": "주문이 전송되었습니다."
      }
    }
    ```

### 3.3 매매 일지 & AI 복기 (Journal)
장 마감 후 작성된 일지와 **Aegis/Nexus의 Dual Review** 내용을 조회합니다.

*   **Endpoint:** `GET /journal/{date}`
*   **Path Parameters:**
    *   `date`: `YYYY-MM-DD`
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": {
        "date": "2026-01-22",
        "dailyProfit": -50000,
        "tradeCount": 3,
        "winRate": 33.3,
        "reviews": {
            "executionReview": "### Aegis Execution Report (Slippage Analysis)\n- **SK하이닉스:** 매수 시 0.8% 슬리피지 발생. 당시 매도 1호가 공백이 발생하여 시장가 체결 시 불리했습니다. 내일은 지정가 범위를 축소하세요.",
            "strategyReview": "### Nexus Strategy Review\n- **전략 평가:** 변동성이 큰 장세에서 Sentinel의 뉴스 감지는 정확했으나, Vector의 진입 타점이 다소 늦었습니다. 기술적 지표의 민감도를 상향 조정합니다."
        },
        "score": 75
      }
    }
    ```

---

## 4. Settings & Control (설정 및 제어)

### 4.1 시스템 설정 조회
*   **Endpoint:** `GET /settings`
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": {
        "strategyMode": "NEUTRAL",      // AGGRESSIVE, NEUTRAL, STABLE
        "reEntryAllowed": true,         // 재진입 허용 여부
        "maxPortfolioWeight": 20,       // 종목당 최대 비중 (%)
        "apiRates": {                   // 현재 적용된 Rate Limit 정보
            "kiwoom": "4 req/sec",
            "naver": "10 req/sec",
            "gemini": "1000 req/min"
        }
      }
    }
    ```

### 4.2 시스템 설정 변경
*   **Endpoint:** `PUT /settings`
*   **Request Body:**
    ```json
    {
      "strategyMode": "AGGRESSIVE",
      "reEntryAllowed": false,
      "maxPortfolioWeight": 30
    }
    ```
*   **Response Body:** `{"status": "SUCCESS", "data": "Settings updated."}`

### 4.3 RSS 피드 관리
*   **Endpoint:** `GET /settings/rss`
*   **Response Body:**
    ```json
    {
      "status": "SUCCESS",
      "data": [
        { "id": 1, "name": "DART", "url": "http://dart.fss...", "isActive": true }
      ]
    }
    ```
*   **Endpoint:** `POST /settings/rss`
    *   **Body:** `{ "name": "Source Name", "url": "https://..." }`
*   **Endpoint:** `DELETE /settings/rss/{id}`

### 4.4 시스템 상태 제어 (Kill Switch)
*   **Endpoint:** `POST /system/control`
*   **Request Body:**
    ```json
    {
      "command": "STOP_TRADING" 
      // START_TRADING: 매매 시작
      // STOP_TRADING: 신규 진입 중단 (청산은 유지)
      // FORCE_LIQUIDATION: 즉시 전량 매도 후 종료
    }
    ```

---

## 5. WebSocket Notification (Real-time)

프론트엔드가 구독(Subscribe)해야 할 WebSocket 토픽 명세입니다.

*   **Endpoint:** `/ws-stomp`
*   **Topic:** `/topic/alert`
*   **Payload Format:**
    ```json
    {
        "type": "KILL_SWITCH",      // KILL_SWITCH, BUY_SIGNAL, SELL_SIGNAL, SYSTEM_ERROR
        "message": "DART '횡령/배임' 공시 감지! (종목: 005930)",
        "timestamp": "2026-01-22T10:30:05",
        "data": {
            "stockCode": "005930",
            "source": "RSS_DART",
            "reason": "횡령설 발생"
        }
    }
    ```

*   **Topic:** `/topic/trade` (실시간 체결 알림)
    ```json
    {
        "type": "EXECUTION",
        "stockCode": "005930",
        "side": "BUY",
        "price": 72000,
        "quantity": 10,
        "timestamp": "2026-01-22T10:31:00"
    }
    ```
--- END OF FILE INT_API_Specification.md ---