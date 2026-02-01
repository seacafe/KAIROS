# KAIROS Internal API Specification

* **Version:** 3.0.0
* **Protocol:** REST over HTTP/1.1
* **Base URL:** `/api`
* **Content-Type:** `application/json; charset=UTF-8`
* **Authentication:** Bearer Token (JWT) required in `Authorization` header.

> [!NOTE]
> **v3.0.0 변경사항**: API 경로 표준화 및 리소스 식별자를 `stockCode` (업계 표준)로 통일.

---

## 1. Accounts (계좌)

### 1.1 계좌 잔고 조회

현재 계좌의 예수금, 총 자산, 당일 손익 정보를 반환합니다.

* **Endpoint:** `GET /accounts/balance`
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": {
        "accountNo": "50561234-11",
        "totalAsset": 10500000,
        "deposit": 2500000,
        "d2Deposit": 2500000,
        "dailyProfitLoss": 150000,
        "dailyReturnRate": 1.45,
        "updatedAt": "2026-01-22T10:30:00"
      }
    }
    ```

### 1.2 보유 종목 조회

현재 보유 중인 종목 리스트와 실시간 수익률을 반환합니다.

* **Endpoint:** `GET /accounts/holdings`
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": [
        {
          "stockCode": "005930",
          "stockName": "삼성전자",
          "quantity": 10,
          "avgPrice": 72000,
          "currentPrice": 73500,
          "profitLoss": 15000,
          "profitRate": 2.08,
          "weight": 7.0
        }
      ]
    }
    ```

---

## 2. Stocks (AI 추천 종목)

### 2.1 추천 종목 목록

Nexus가 분석하여 선정한 종목 리스트입니다.

* **Endpoint:** `GET /stocks`
* **Query Parameters:**
  * `date`: `YYYY-MM-DD` (Optional, Default: Today)
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": [
        {
          "stockCode": "035720",
          "stockName": "카카오",
          "baseDate": "2026-01-22",
          "nexusScore": 85,
          "decision": "BUY",
          "riskLevel": "HIGH",
          "status": "WATCHING",
          "targetPrice": 55000,
          "stopLoss": 51000,
          "nexusReason": "카카오브레인 합병 이슈 및 외인 수급 연속성 포착"
        }
      ]
    }
    ```

### 2.2 종목 상세 AI 리포트

특정 종목에 대한 7인 에이전트 분석 리포트입니다.

* **Endpoint:** `GET /stocks/{stockCode}/analysis`
* **Path Parameters:**
  * `stockCode`: 종목코드 (예: 005930, 035720)
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": {
        "stockCode": "035720",
        "stockName": "카카오",
        "generatedAt": "2026-01-22T08:15:00",
        "agentScores": {
          "sentinel": 90,
          "axiom": 60,
          "vector": 85,
          "sonar": 75,
          "resonance": 70
        },
        "nexusReport": "## 종합 전략: 적극 매수 (Risk: High)\n\n### 1. Sentinel (News)\n- **호재:** 카카오브레인 합병 소식...\n\n### 2. Vector (Tech)\n- 20일선 지지 확인 및 NanoBanana 패턴 초기...\n\n### 3. Nexus Decision\n- 현재 변동성이 크지만 수급이 뒷받침되므로 진입을 승인합니다."
      }
    }
    ```

---

## 3. Trades (거래)

### 3.1 매매 로그 조회

당일 주문/체결/취소 등의 이벤트 로그입니다.

* **Endpoint:** `GET /trades`
* **Query Parameters:**
  * `date`: `YYYY-MM-DD` (Optional)
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": [
        {
          "id": 501,
          "stockCode": "000660",
          "stockName": "SK하이닉스",
          "tradeType": "BUY",
          "orderPrice": 180000,
          "filledPrice": 180500,
          "quantity": 5,
          "slippageRate": 0.28,
          "status": "FILLED",
          "agentMsg": "체결 완료 (주문번호: 10234)",
          "executedAt": "2026-01-22T09:30:05"
        }
      ]
    }
    ```

### 3.2 수동 매도

관리자가 시스템 개입을 위해 수동으로 매도 주문을 넣습니다.

* **Endpoint:** `POST /trades/manual-sell`
* **Request Body:**

    ```json
    {
      "stockCode": "005930",
      "quantity": 10,
      "reason": "관리자 수동 청산"
    }
    ```

* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": null
    }
    ```

---

## 4. Journals (매매일지)

### 4.1 매매일지 목록

* **Endpoint:** `GET /journals`
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": [
        {
          "id": 1,
          "date": "2026-01-22",
          "totalProfitLoss": -50000,
          "winRate": 33.3,
          "tradeCount": 3
        }
      ]
    }
    ```

### 4.2 매매일지 상세

장 마감 후 작성된 일지와 AI 복기 내용입니다.

* **Endpoint:** `GET /journals/{date}`
* **Path Parameters:**
  * `date`: `YYYY-MM-DD`
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": {
        "id": 1,
        "date": "2026-01-22",
        "totalProfitLoss": -50000,
        "winRate": 33.3,
        "tradeCount": 3,
        "aiReviewContent": "### Aegis Execution Report\n- SK하이닉스: 매수 시 0.28% 슬리피지 발생...",
        "improvementPoints": "기술적 지표의 민감도를 상향 조정합니다."
      }
    }
    ```

---

## 5. Settings (설정)

### 5.1 시스템 설정 조회

* **Endpoint:** `GET /settings`
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": {
        "strategyMode": "NEUTRAL",
        "reEntryAllowed": true,
        "maxLossPerTrade": 3.0
      }
    }
    ```

### 5.2 전략 모드 변경

* **Endpoint:** `PUT /settings/strategy`
* **Query Parameters:**
  * `mode`: `AGGRESSIVE`, `NEUTRAL`, `STABLE`
* **Response Body:** `{"status": "SUCCESS", "data": {...}}`

### 5.3 RSS 피드 관리

* **Endpoint:** `GET /settings/rss-feeds`
* **Response Body:**

    ```json
    {
      "status": "SUCCESS",
      "data": [
        { "id": 1, "name": "DART", "url": "http://dart.fss...", "category": "DISCLOSURE", "isActive": true }
      ]
    }
    ```

* **Endpoint:** `POST /settings/rss-feeds`
  * **Body:** `{ "name": "Source Name", "url": "https://...", "category": "DOMESTIC" }`
* **Endpoint:** `DELETE /settings/rss-feeds/{id}`

---

## 6. System Control

### 6.1 시스템 상태 제어 (Kill Switch)

* **Endpoint:** `POST /system/control`
* **Request Body:**

    ```json
    {
      "command": "STOP_TRADING"
    }
    ```

    | Command | 설명 |
    |---------|------|
    | `START_TRADING` | 매매 시작 |
    | `STOP_TRADING` | 신규 진입 중단 (청산은 유지) |
    | `FORCE_LIQUIDATION` | 즉시 전량 매도 후 종료 |

---

## 7. WebSocket (Real-time)

### 7.1 연결

* **Endpoint:** `/ws-stomp`

### 7.2 알림 토픽

* **Topic:** `/topic/alert`

    ```json
    {
      "type": "KILL_SWITCH",
      "message": "DART '횡령/배임' 공시 감지! (종목: 005930)",
      "timestamp": "2026-01-22T10:30:05",
      "data": {
        "stockCode": "005930",
        "source": "RSS_DART",
        "reason": "횡령설 발생"
      }
    }
    ```

### 7.3 체결 토픽

* **Topic:** `/topic/trade`

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

---

## API Endpoint Summary

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/accounts/balance` | 계좌 잔고 조회 |
| GET | `/accounts/holdings` | 보유 종목 조회 |
| GET | `/stocks` | AI 추천 종목 목록 |
| GET | `/stocks/{stockCode}/analysis` | 종목 AI 분석 리포트 |
| GET | `/trades` | 매매 로그 조회 |
| POST | `/trades/manual-sell` | 수동 매도 |
| GET | `/journals` | 매매일지 목록 |
| GET | `/journals/{date}` | 매매일지 상세 |
| GET | `/settings` | 설정 조회 |
| PUT | `/settings/strategy` | 전략 모드 변경 |
| GET | `/settings/rss-feeds` | RSS 피드 목록 |
| POST | `/settings/rss-feeds` | RSS 피드 추가 |
| DELETE | `/settings/rss-feeds/{id}` | RSS 피드 삭제 |
| POST | `/system/control` | 시스템 제어 (Kill Switch) |
