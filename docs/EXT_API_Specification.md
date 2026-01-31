# External API Integration Specification for KAIROS

## 1. Overview

본 문서는 KAIROS 시스템이 외부 서비스(Kiwoom, Naver, RSS, Gemini)와 통신하기 위한 상세 프로토콜 및 데이터 규격을 정의합니다.
"주의: 본 문서에 정의되지 않은 필드는 반드시 동봉된 Kiwoom REST API 문서.md의 해당 TR 섹션을 기준으로 구현한다. Naver API는 공식 개발자 센터 문서를 참조한다."
**모든 필드는 키움증권 Open API 명세서(markdown)를 기준으로 기술되었습니다. 만약 존재하지 않는 필드가 있다면 `키움 REST API 문서`를 확인합니다.**

---

## 2. Information Gathering (Hybrid Strategy)

### 2.1 RSS Feeds (Real-time Surveillance Layer)

* **Target Agent:** **Sentinel (NewsAgent)** & **Resonance (SentimentAgent)**
* **Purpose:** 장중(09:00~15:00) 실시간 이슈, 공시, 글로벌 매크로 감시 (비용: 무료).
* **Mechanism:** 1분 주기 Polling (Java `Rome` Library 사용).
* **Strategy:** Passive Monitoring -> Keyword Match -> Trigger Event (NewsAgent).

#### [Target Sources]

**A. Domestic News (General Market)**

* **Description:** 시장의 전반적인 분위기와 특징주를 실시간으로 포착합니다.
* **Hankyung (증권):** `https://rss.hankyung.com/feed/stock`
* **MK (기업):** `https://www.mk.co.kr/rss/30100041/`
* **Yonhap Infomax (금융/증권):** `https://news.einfomax.co.kr/rss/rss_news.xml`
* **Etoday (증권):** `https://www.etoday.co.kr/news/rss/stock.xml`

**B. Corporate Disclosure (Critical - Kill Switch)**

* **Description:** 주가에 치명적인 영향을 주는 공시를 가장 빠르게 감지합니다.
* **Source:** **DART (전자공시시스템)**
* **URL:** `http://dart.fss.or.kr/api/todayRSS.xml`
* **Trigger Keywords:**
  * `거래정지`, `매매거래정지`
  * `유상증자`, `감자`, `무상감자`
  * `횡령`, `배임`
  * `부도`, `파산`, `회생절차`
  * `불성실공시법인`
* **Action:** 위 키워드 감지 시 `Sentinel`은 즉시 **'Kill Switch'** 시그널을 생성하여 보유 종목의 시장가 매도를 요청해야 합니다.

**C. Global Market (Macro - Sentiment Score)**

* **Description:** 국내 증시에 선행하는 미국 증시와 원자재 흐름을 파악합니다.
* **Investing.com (뉴스):** `https://kr.investing.com/rss/news.rss`
* **Investing.com (증권):** `https://kr.investing.com/rss/stock.rss`
* **Nasdaq (Global):** `https://www.nasdaq.com/feed/rssoutbound`
* **Action:** `Sentinel`은 나스닥 선물 지수 및 반도체 섹터 뉴스를 분석하여 장중 '시장 분위기 점수(Risk-On/Off)'를 산출합니다.

---

### 2.2 Naver Search API (Deep Dive Layer)

* **Purpose:** 장전 주도주 발굴 및 RSS로 포착된 이슈의 심층 검증 (Active Discovery).
* **Official References:**
  * **API Spec:** [Naver News Search API Guide](https://developers.naver.com/docs/serviceapi/search/news/news.md#%EB%89%B4%EC%8A%A4)
  * **Error Codes:** [Naver Common Error Codes](https://developers.naver.com/docs/common/openapiguide/errorcode.md)
* **Base URL:** `https://openapi.naver.com`
* **Content-Type:** `application/json; charset=UTF-8`
* **Authorization:** Headers (`X-Naver-Client-Id`, `X-Naver-Client-Secret`)
* **Rate Limit:** 일일 25,000건 (Daily Quota)

#### [GET] 뉴스 검색

* **Description:** 장전(07:00~08:30) 주도주 발굴을 위해 '특징주', '수주' 등의 키워드로 뉴스를 검색합니다.
* **Endpoint:** `/v1/search/news.json`
* **Method:** `GET`
* **Query Parameters:**
  * `query` (String, Required): 검색어 (UTF-8 URL Encoded)
  * `display` (Integer, Optional): 출력 건수 (기본 10, 최대 100)
  * `start` (Integer, Optional): 검색 시작 위치 (기본 1, 최대 1000)
  * `sort` (String, Optional): 정렬 (`date`: 날짜순, `sim`: 정확도순 - 기본값)
* **Response Body (JSON):**

    ```json
    {
      "lastBuildDate": "Fri, 21 May 2025 10:00:00 +0900",
      "total": 100,
      "start": 1,
      "display": 10,
      "items": [
        {
          "title": "뉴스 제목 (HTML 태그 포함)",
          "originallink": "http://...",
          "link": "http://...",
          "description": "뉴스 요약 (HTML 태그 포함)",
          "pubDate": "Fri, 21 May 2025 10:00:00 +0900"
        }
      ]
    }
    ```

* **Error Codes:**
  * `SE01`: 잘못된 쿼리 요청 (파라미터 확인)
  * `SE06`: 인코딩 실패 (UTF-8 필수)
  * `SE99`: 시스템 에러 (서버 내부 오류)
  * `403`: 권한 없음 (Client ID/Secret 확인 또는 API 권한 미설정)

---

## 3. Kiwoom Open API (Execution Layer)

* **Purpose:** 장중(09:00~15:00) 주식 정보 확인 및 주식 매매, 거래 동향 확인.
* **Base URL (Dev):** `https://mockapi.kiwoom.com`
* **Base URL (Prod):** `https://api.kiwoom.com`
* **Content-Type:** `application/json; charset=UTF-8`
* **Authorization:** Bearer {ACCESS_TOKEN} (OAuth 2.0)
* **appkey:** {APP_KEY}
* **appsecret:** {APP_SECRET}
* **tr_id:** {TR_ID} (API ID, 예: ka10001)
* **cont-yn:** N (기본값) 또는 Y (다음 데이터 요청 시)
* **next-key:** (첫 요청 시 공백, 연속 조회 시 이전 응답 헤더의 next-key 값 전송)
* **Rate Limit:** REST 초당 5건 (계좌당) / WebSocket 제한 없음

### 3.1 Authentication

#### [au10001] 접근 토큰 발급

* **Description:** 매일 장 시작 전(08:00) 호출하여 토큰 갱신 (유효기간: 24시간)
* **Endpoint:** `/oauth2/token`
* **Method:** `POST`
* **Request Body:**

    ```json
    {
      "grant_type": "client_credentials",
      "appkey": "{KIWOOM_APP_KEY}",
      "secretkey": "{KIWOOM_SECRET_KEY}"
    }
    ```

* **Response Body:**
  * `token` (String): 접근 토큰 (API 호출 시 Bearer 헤더에 사용)
  * `expires_in` (Number): 토큰 유효 시간 (초)
  * `token_type` (String): "Bearer"
  * `expires_dt` (String): 만료 일시 (yyyyMMddHHmmss)
* **Note:** 유효기간이 만료되면 `au10002`로 폐기 후 재발급 로직 필요.

#### [au10002] 접근 토큰 폐기 (Revoke)

* **Description:** 장 종료 후 시스템 종료 시 또는 토큰 만료 전 강제 재발급이 필요할 때 기존 토큰을 무효화합니다.
* **Endpoint:** `/oauth2/revoke`
* **Method:** `POST`
* **Request Body:**

    ```json
    {
      "appkey": "{KIWOOM_APP_KEY}",
      "secretkey": "{KIWOOM_SECRET_KEY}",
      "token": "{TOKEN_TO_REVOKE}"
    }
    ```

* **Response Body:**
  * `return_code` (Integer): 결과 코드 (0: 정상)
  * `return_msg` (String): 결과 메시지 (예: "정상적으로 처리되었습니다")

### 3.2 Market Data (REST)

#### [ka10001] 주식 기본 정보 요청

* **Target Agent:** **Axiom (Fundamental Agent)**
* **Description:** 종목의 펀더멘털(PER, PBR, 시가총액) 및 상한가/하한가 정보를 조회하여 재무 리스크를 필터링합니다.
* **Endpoint:** `/api/dostk/stkinfo`
* **Method:** `POST`
* **Request Body:**
  * `stk_cd` (String, Required): 종목코드 (예: "005930")
* **Response Body:**
  * `stk_cd`: 종목코드
  * `stk_nm`: 종목명
  * `setl_mm`: 결산월
  * `fav`: 액면가
  * `cap`: 자본금
  * `flo_stk`: 상장주식
  * `crd_rt`: 신용비율
  * `oyr_hgst`: 연중최고
  * `oyr_lwst`: 연중최저
  * `mac`: 시가총액
  * `mac_wght`: 시가총액비중
  * `for_exh_rt`: 외인소진률
  * `repl_pric`: 대용가
  * `per`: PER
  * `eps`: EPS
  * `roe`: ROE
  * `pbr`: PBR
  * `ev`: EV
  * `bps`: BPS
  * `sale_amt`: 매출액
  * `bus_pro`: 영업이익
  * `cup_nga`: 당기순이익
  * `250hgst`: 250일최고
  * `250lwst`: 250일최저
  * `open_pric`: 시가
  * `high_pric`: 고가
  * `low_pric`: 저가
  * `upl_pric`: 상한가
  * `lst_pric`: 하한가
  * `base_pric`: 기준가
  * `exp_cntr_pric`: 예상체결가
  * `exp_cntr_qty`: 예상체결수량
  * `250hgst_pric_dt`: 250일최고가일
  * `250hgst_pric_pre_rt`: 250일최고가대비율
  * `250lwst_pric_dt`: 250일최저가일
  * `250lwst_pric_pre_rt`: 250일최저가대비율
  * `cur_prc`: 현재가
  * `pre_sig`: 대비기호
  * `pred_pre`: 전일대비
  * `flu_rt`: 등락율
  * `trde_qty`: 거래량
  * `trde_pre`: 거래대비
  * `fav_unit`: 액면가단위
  * `dstr_stk`: 유통주식
  * `dstr_rt`: 유통비율

#### [ka10081] 주식 일봉 차트 조회

* **Target Agent:** **Vector (Technical Agent)**
* **Description:** 장전 5/20/60일 이동평균선 계산 및 NanoBanana 패턴 분석을 위한 일봉 데이터를 조회합니다.
* **Endpoint:** `/api/dostk/chart`
* **Method:** `POST`
* **Request Body:**
  * `stk_cd` (String): 종목코드
  * `base_dt` (String): 기준일자 (YYYYMMDD)
  * `upd_stkpc_tp` (String): 수정주가구분 (0:미적용, 1:적용)
* **Response Body (List: `stk_dt_pole_chart_qry`):**
  * `stk_cd`: 종목코드
  * `dt`: 일자 (YYYYMMDD)
  * `cur_prc`: 현재가(종가)
  * `open_pric`: 시가
  * `high_pric`: 고가
  * `low_pric`: 저가
  * `trde_qty`: 거래량
  * `trde_prica`: 거래대금
  * `trde_tern_rt`: 거래회전율
  * `pred_pre`: 전일대비
  * `pred_pre_sig`: 전일대비기호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)

#### [ka10080] 주식 분봉 차트 조회

* **Target Agent:** **Vector (Technical Agent)**
* **Description:** 장중 실시간 눌림목 및 돌파 타점 계산을 위해 분봉(3분/15분) 데이터를 조회합니다.
* **Endpoint:** `/api/dostk/chart`
* **Method:** `POST`
* **Request Body:**
  * `stk_cd` (String): 종목코드
  * `tic_scope` (String): 틱범위 (1:1분, 3:3분, 5:5분 ...)
  * `upd_stkpc_tp` (String): 수정주가구분 (1:적용)
* **Response Body (List: `stk_min_pole_chart_qry`):**
  * `stk_cd`: 종목코드
  * `cntr_tm`: 체결시간 (HHMMSS)
  * `cur_prc`: 현재가(종가)
  * `open_pric`: 시가
  * `high_pric`: 고가
  * `low_pric`: 저가
  * `trde_qty`: 거래량
  * `acc_trde_qty`: 누적거래량
  * `pred_pre`: 전일대비
  * `pred_pre_sig`: 전일대비기호

#### [ka10008] 주식 외국인 종목별 매매동향

* **Target Agent:** **Sonar (TradingFlow Agent)**
* **Description:** 외국인 보유 비중 변화 및 한도 소진율을 추적하여 수급의 질을 판단합니다.
* **Endpoint:** `/api/dostk/frgnistt`
* **Method:** `POST`
* **Request Body:**
  * `stk_cd` (String, Required): 종목코드 (예: "005930")
* **Response Body (List: `stk_frgnr`):**
  * `dt`: 일자
  * `close_pric`: 종가
  * `pred_pre`: 전일대비
  * `trde_qty`: 거래량
  * `chg_qty`: 변동수량
  * `poss_stkcnt`: 보유주식수
  * `wght`: 비중
  * `gain_pos_stkcnt`: 취득가능주식수
  * `frgnr_limit`: 외국인한도
  * `frgnr_limit_irds`: 외국인한도증감
  * `limit_exh_rt`: 한도소진률

#### [ka10009] 주식 기관 요청 (일별 순매매)

* **Target Agent:** **Sonar (TradingFlow Agent)**
* **Description:** 기관과 외국인의 일별 순매수 추이를 비교하여 쌍끌이 매수 여부를 확인합니다.
* **Endpoint:** `/api/dostk/frgnistt`
* **Method:** `POST`
* **Request Body:**
  * `stk_cd` (String, Required): 종목코드
* **Response Body:**
  * `date`: 일자
  * `close_pric`: 종가
  * `pre`: 대비
  * `orgn_dt_acc`: 기관기간누적
  * `orgn_daly_nettrde`: 기관일별순매매
  * `frgnr_daly_nettrde`: 외국인일별순매매
  * `frgnr_qota_rt`: 외국인지분율

#### [ka90003] 프로그램 순매수 상위 50 요청

* **Target Agent:** **Sonar (TradingFlow Agent)**
* **Description:** 시장 전체의 프로그램 매수세가 집중되는 상위 종목을 파악하여 섹터 흐름을 읽습니다.
* **Endpoint:** `/api/dostk/stkinfo`
* **Method:** `POST`
* **Request Body:**
  * `trde_upper_tp` (매매상위구분),
  * `amt_qty_tp`,
  * `mrkt_tp`
* **Response Body (List: `prm_netprps_upper_50`):**
  * `rank`: 순위
  * `stk_cd`: 종목코드
  * `stk_nm`: 종목명
  * `cur_prc`: 현재가
  * `flu_sig`: 등락기호
  * `pred_pre`: 전일대비
  * `flu_rt`: 등락율
  * `acc_trde_qty`: 누적거래량
  * `prm_sell_amt`: 프로그램매도금액
  * `prm_buy_amt`: 프로그램매수금액
  * `prm_netprps_amt`: 프로그램순매수금액

#### [ka90004] 종목별 프로그램 매매 현황

* **Target Agent:** **Sonar (TradingFlow Agent)**
* **Description:** 특정 타겟 종목에 유입되는 프로그램(차익/비차익) 매수세를 확인합니다.
* **Endpoint:** `/api/dostk/stkinfo`
* **Method:** `POST`
* **Request Body:**
  * `stk_cd` (String): 종목코드
  * `dt` (String): 일자 (YYYYMMDD)
  * `mrkt_tp` (String): 시장구분 ("P00101":코스피, "P10102":코스닥)
* **Response Body (List: `stk_prm_trde_prst`):**
  * `tot_1`: 매수체결수량합계
  * `tot_2`: 매수체결금액합계
  * `tot_3`: 매도체결수량합계
  * `tot_4`: 매도체결금액합계
  * `tot_5`: 순매수대금합계
  * `tot_6`: 합계6
  * `stk_cd`: 종목코드
  * `stk_nm`: 종목명
  * `cur_prc`: 현재가
  * `flu_sig`: 등락기호
  * `pred_pre`: 전일대비
  * `buy_cntr_qty`: 매수체결수량
  * `buy_cntr_amt`: 매수체결금액
  * `sel_cntr_qty`: 매도체결수량
  * `sel_cntr_amt`: 매도체결금액
  * `netprps_prica`: 순매수대금
  * `all_trde_rt`: 전체거래비율

#### [ka10040] 당일 주요 거래원 요청

* **Target Agent:** **Sonar (TradingFlow Agent)**
* **Description:** 상위 거래원(외국계 창구 등)의 매집 여부를 실시간으로 추적합니다.
* **Endpoint:** `/api/dostk/rkinfo`
* **Method:** `POST`
* **Request Body:**
  * `stk_cd` (String): 종목코드
* **Response Body:**
  * `sel_trde_ori_irds_1` ~ `_5`: 매도거래원별증감 (1~5위)
  * `sel_trde_ori_qty_1` ~ `_5`: 매도거래원수량 (1~5위)
  * `sel_trde_ori_1` ~ `_5`: 매도거래원명 (1~5위)
  * `sel_trde_ori_cd_1` ~ `_5`: 매도거래원코드 (1~5위)
  * `buy_trde_ori_1` ~ `_5`: 매수거래원명 (1~5위)
  * `buy_trde_ori_cd_1` ~ `_5`: 매수거래원코드 (1~5위)
  * `buy_trde_ori_qty_1` ~ `_5`: 매수거래원수량 (1~5위)
  * `buy_trde_ori_irds_1` ~ `_5`: 매수거래원별증감 (1~5위)
  * `frgn_sel_prsm_sum_chang`: 외국계매도추정합변동
  * `frgn_sel_prsm_sum`: 외국계매도추정합
  * `frgn_buy_prsm_sum`: 외국계매수추정합
  * `frgn_buy_prsm_sum_chang`: 외국계매수추정합변동
  * `tdy_main_trde_ori`: (List) 당일주요거래원 상세
    * `sel_scesn_tm`: 매도이탈시간
    * `sell_qty`: 매도수량
    * `sel_upper_scesn_ori`: 매도상위이탈원
    * `buy_scesn_tm`: 매수이탈시간
    * `buy_qty`: 매수수량
    * `buy_upper_scesn_ori`: 매수상위이탈원
    * `qry_dt`: 조회일자
    * `qry_tm`: 조회시간

### 3.3 Trading (REST)

#### [kt10000] 주식 매수 주문

* **Target Agent:** **Aegis (Portfolio Manager)**
* **Description:** 최종적으로 승인된 종목에 대해 신규 매수 주문을 실행합니다.
* **Endpoint:** `/api/dostk/ordr`
* **Method:** `POST`
* **Request Body:**
  * `dmst_stex_tp` (String): "KRX" (필수)
  * `stk_cd` (String): 종목코드
  * `ord_qty` (String): 주문수량
  * `ord_uv` (String): 주문단가 (시장가일 경우 "0")
  * `trde_tp` (String): 거래구분 ("00":지정가, "03":시장가)
  * `cond_uv` (String): 조건단가 (일반주문시 공백)
* **Response Body:**
  * `ord_no`: 주문번호
  * `return_code`: 결과코드 (0:정상)
  * `return_msg`: 결과메시지

#### [kt10001] 주식 매도 주문

* **Target Agent:** **Aegis (Portfolio Manager)**
* **Description:** 보유 종목의 이익 실현 또는 손절매(Kill Switch 포함)를 위해 매도 주문을 실행합니다.
* **Endpoint:** `/api/dostk/ordr`
* **Method:** `POST`
* **Request Body:**
  * `dmst_stex_tp` (String): "KRX"
  * `stk_cd` (String): 종목코드
  * `ord_qty` (String): 매도수량
  * `ord_uv` (String): 매도단가
  * `trde_tp` (String): 거래구분 ("00":지정가, "03":시장가)
  * `cond_uv`
* **Response Body:**
  * `ord_no`: 주문번호
  * `dmst_stex_tp`: 국내거래소구분

#### [kt10003] 주식 취소 주문

* **Target Agent:** **Aegis (Portfolio Manager)**
* **Description:** 미체결된 주문을 취소합니다.
* **Endpoint:** `/api/dostk/ordr`
* **Method:** `POST`
* **Request Body:**
  * `dmst_stex_tp` (String): "KRX"
  * `stk_cd` (String): 종목코드
  * `orig_ord_no` (String): 원주문번호 (필수)
  * `cncl_qty` (String): 취소수량 ("0": 전량취소)
* **Response Body:**
  * `ord_no`: 취소주문번호
  * `base_orig_ord_no`: 모주문번호
  * `cncl_qty`: 취소된 수량

#### [kt00004] 계좌 평가 현황

* **Target Agent:** **Aegis (Portfolio Manager)**
* **Description:** 예수금 상황을 확인하여 자금 배분 및 리스크 관리를 수행합니다.
* **Endpoint:** `/api/dostk/acnt`
* **Method:** `POST`
* **Request Body:**
  * `acnt_no` (String): 계좌번호
  * `qry_tp` (String): "0" (전체)
  * `dmst_stex_tp` (String): "KRX"
* **Response Body:**
  * `acnt_nm`: 계좌명
  * `brch_nm`: 지점명
  * `entr`: 예수금
  * `d2_entra`: **D+2추정예수금** (주문가능액 기준)
  * `tot_est_amt`: 유가잔고평가액
  * `aset_evlt_amt`: 예탁자산평가액
  * `tot_pur_amt`: 총매입금액
  * `prsm_dpst_aset_amt`: 추정예탁자산
  * `tot_grnt_sella`: 매도담보대출금
  * `tdy_lspft_amt`: 당일투자원금
  * `invt_bsamt`: 당월투자원금
  * `lspft_amt`: 누적투자원금
  * `tdy_lspft`: 당일투자손익
  * `lspft2`: 당월투자손익
  * `lspft`: 누적투자손익
  * `tdy_lspft_rt`: 당일손익율
  * `lspft_ratio`: 당월손익율
  * `lspft_rt`: 누적손익율
  * `stk_acnt_evlt_prst`: (List) 종목별계좌평가현황
    * `stk_cd`: 종목코드
    * `stk_nm`: 종목명
    * `rmnd_qty`: 보유수량
    * `avg_prc`: 평균단가
    * `cur_prc`: 현재가
    * `evlt_amt`: 평가금액
    * `pl_amt`: 손익금액
    * `pl_rt`: 손익율
    * `loan_dt`: 대출일
    * `pur_amt`: 매입금액
    * `setl_remn`: 결제잔고
    * `pred_buyq`: 전일매수수량
    * `pred_sellq`: 전일매도수량
    * `tdy_buyq`: 금일매수수량
    * `tdy_sellq`: 금일매도수량

#### [kt00005] 체결 잔고

* **Target Agent:** **Aegis (Portfolio Manager)**
* **Description:** 현재 보유 중인 종목의 잔고와 수익률을 실시간으로 확인합니다.
* **Endpoint:** `/api/dostk/acnt`
* **Method:** `POST`
* **Request Body:**
  * `dmst_stex_tp` (String): "KRX"
* **Response Body (List: `stk_cntr_remn`):**
  * `entr`: 예수금
  * `entr_d1`: 예수금D+1
  * `entr_d2`: 예수금D+2
  * `pymn_alow_amt`: 출금가능금액
  * `uncl_stk_amt`: 미수확보금
  * `repl_amt`: 대용금
  * `rght_repl_amt`: 권리대용금
  * `ord_alowa`: 주문가능현금
  * `ch_uncla`: 현금미수금
  * `crd_int_npay_gold`: 신용이자미납금
  * `etc_loana`: 기타대여금
  * `nrpy_loan`: 미상환융자금
  * `profa_ch`: 증거금현금
  * `repl_profa`: 증거금대용
  * `stk_buy_tot_amt`: 주식매수총액
  * `evlt_amt_tot`: 평가금액합계
  * `tot_pl_tot`: 총손익합계
  * `tot_pl_rt`: 총손익률
  * `tot_re_buy_alowa`: 총재매수가능금액
  * `20ord_alow_amt` ~ `100ord_alow_amt`: 증거금별주문가능금액 (20, 30, 40, 50, 60, 100)
  * `crd_loan_tot`: 신용융자합계
  * `crd_loan_ls_tot`: 신용융자대주합계
  * `crd_grnt_rt`: 신용담보비율
  * `dpst_grnt_use_amt_amt`: 예탁담보대출금액
  * `grnt_loan_amt`: 매도담보대출금액
  * `stk_cntr_remn`: (List) 종목별체결잔고
    * `crd_tp`: 신용구분
    * `loan_dt`: 대출일
    * `expr_dt`: 만기일
    * `stk_cd`: 종목번호
    * `stk_nm`: 종목명
    * `setl_remn`: 결제잔고
    * `cur_qty`: 현재잔고
    * `cur_prc`: 현재가
    * `buy_uv`: 매입단가
    * `pur_amt`: 매입금액
    * `evlt_amt`: 평가금액
    * `evltv_prft`: 평가손익
    * `pl_rt`: 손익률

### 3.4 Real-time Data (WebSocket)

* **Purpose:** 장중 실시간 시세, 체결, 수급 정보 수신 (Event-Driven)
* **Base URL (Dev):** `wss://mockapi.kiwoom.com:10000`
* **Base URL (Prod):** `wss://api.kiwoom.com:10000`
* **Protocol:** WebSocket over SSL
* **Auth:** Handshake 시점이 아닌, 연결 후 `REG` 패킷 전송 시 Access Token 포함

#### [REG] 실시간 데이터 등록 (Subscribe)

* **Description:** 특정 종목의 실시간 데이터를 수신하기 위해 서버에 구독 요청을 보냅니다.
* **Path:** `/api/dostk/websocket`
* **Request Payload (JSON):**

    ```json
    {
      "trnm": "REG",
      "grp_no": "100",  // 화면번호 그룹 (임의 지정)
      "refresh": "1",   // 0: 기존 구독 해지 후 등록, 1: 추가 등록 (Default)
      "data": [
        {
          "item": ["005930", "035720"], // 종목코드 리스트
          "type": ["00", "0A", "0w", "1h"] // 구독할 TR 코드 (체결, 기세, 프로그램, VI)
        }
      ]
    }
    ```

  * *Note:* 헤더에 `Authorization: Bearer {ACCESS_TOKEN}` 필수 포함.

#### [REAL] 실시간 데이터 수신 (Response Stream)

* **Description:** 등록된 종목의 이벤트 발생 시 서버로부터 푸시되는 데이터 패킷입니다.
* **Response Payload (JSON):**

    ```json
    {
      "trnm": "REAL",
      "data": [
        {
          "type": "00",       // TR 코드 (예: 주문체결)
          "item": "005930",   // 종목코드
          "name": "주식체결", // 한글명
          "values": {         // 실제 데이터 (Key-Value)
            "20": "090000",
            "10": "75000",
            ...
          }
        }
      ]
    }
    ```

#### [Real-time Fields Definition]

각 TR 코드별 `values` 객체에 포함되는 주요 필드 명세입니다.

#### 주문체결 (Order Execution)

* **Target Agent:** **Aegis (Portfolio Manager)**
* **Description:** 계좌의 주문 접수 및 체결 내역을 실시간으로 수신합니다.
* **Values:**
  * `9201`: 계좌번호
  * `9203`: 주문번호
  * `9205`: 관리자사번
  * `9001`: 종목코드
  * `912`: 주문업무분류
  * `913`: 주문상태 (접수, 체결, 확인, 취소, 거부)
  * `302`: 종목명
  * `900`: 주문수량
  * `901`: 주문가격
  * `902`: 미체결수량
  * `903`: 체결누계금액
  * `904`: 원주문번호
  * `905`: 주문구분 (+/-)
  * `906`: 매매구분
  * `907`: 매도수구분 (1:매도, 2:매수)
  * `908`: 주문/체결시간
  * `909`: 체결번호
  * `910`: 체결가
  * `911`: 체결량
  * `10`: 현재가
  * `27`: (최우선)매도호가
  * `28`: (최우선)매수호가
  * `914`: 단위체결가
  * `915`: 단위체결량
  * `938`: 당일매매수수료
  * `939`: 당일매매세금
  * `919`: 거부사유
  * `920`: 화면번호
  * `921`: 터미널번호
  * `922`: 신용구분
  * `923`: 대출일
  * `10010`: 시간외단일가_현재가
  * `2134`: 거래소구분
  * `2135`: 거래소구분명
  * `2136`: SOR여부

##### [0A] 주식기세 (Stock Quote)

* **Target Agent:** **Vector (Technical Agent)**
* **Description:** 주식의 체결이 발생하지 않을 때 호가 변동이나 대량 매매 등 기세 변화를 감지합니다.
* **Values:**
  * `10`: 현재가
  * `11`: 전일대비
  * `12`: 등락율
  * `27`: (최우선)매도호가
  * `28`: (최우선)매수호가
  * `13`: 누적거래량
  * `14`: 누적거래대금
  * `16`: 시가
  * `17`: 고가
  * `18`: 저가
  * `25`: 전일대비기호
  * `26`: 전일거래량대비
  * `29`: 거래대금증감
  * `30`: 전일거래량대비(비율)
  * `31`: 거래회전율
  * `32`: 거래비용
  * `311`: 시가총액(억)
  * `567`: 상한가발생시간
  * `568`: 하한가발생시간

##### [0C] 주식우선호가 (Best Order Book Quote)

* **Target Agent:** **Aegis (Portfolio Manager)**
* **Description:** 지정가 주문 시 슬리피지를 방지하기 위해 최우선 호가를 실시간으로 파악합니다.
* **Values:**
  * `27`: (최우선)매도호가
  * `28`: (최우선)매수호가

#### [0w] 종목프로그램매매 (Program Trading)

* **Target Agent:** **Sonar (TradingFlow Agent)**
* **Description:** 프로그램 매매 추이를 실시간으로 파악하여 수급의 질을 판단합니다.
* **Values:**
  * `20`: 체결시간
  * `10`: 현재가
  * `25`: 전일대비기호
  * `11`: 전일대비
  * `12`: 등락율
  * `13`: 누적거래량
  * `202`: 매도수량
  * `204`: 매도금액
  * `206`: 매수수량
  * `208`: 매수금액
  * `210`: 순매수수량
  * `211`: 순매수수량증감
  * `212`: 순매수금액
  * `213`: 순매수금액증감
  * `214`: 장시작예상잔여시간
  * `215`: 장운영구분
  * `216`: 투자자별ticker

#### [1h] VI 발동/해제 (Volatility Interruption)

* **Target Agent:** **Sentinel (NewsAgent)**
* **Description:** 변동성 완화 장치(VI) 발동을 감지하여 급등락에 대응하거나 Kill Switch를 검토합니다.
* **Values:**
  * `9001`: 종목코드
  * `302`: 종목명
  * `13`: 누적거래량
  * `14`: 누적거래대금
  * `9068`: VI발동구분
  * `9008`: KOSPI/KOSDAQ구분
  * `9075`: 장전구분
  * `1221`: VI발동가격
  * `1223`: 매매체결처리시각
  * `1224`: VI해제시각
  * `1225`: VI적용구분
  * `1236`: 기준가격 정적
  * `1237`: 기준가격 동적
  * `1238`: 괴리율 정적
  * `1239`: 괴리율 동적
  * `1489`: VI발동가 등락율
  * `1490`: VI발동횟수
  * `9069`: 발동방향구분
  * `1279`: Extra Item

### 3.5 Kiwoom Error Codes

* `0000`: 정상처리
* `1511`: 필수 입력 값 누락
* `1514`: Authorization 형식 오류
* `1516`: Token 정의되지 않음 (재발급 필요)
* `1700`: 요청 개수 초과 (Rate Limit Exceeded)
* `8001`: App Key / Secret Key 검증 실패

### 3.6 Mock Server Scenarios (WireMock Definition)

개발 및 테스트 단계에서 사용하는 Mock Server는 단순한 성공 응답뿐만 아니라, 실전에서 발생 가능한 **Dirty Scenarios**를 반드시 포함해야 합니다.

#### [Scenario 1] High Latency (주문 체결 지연)

* **Purpose:** 급등락 시 주문 처리가 지연될 때, 중복 주문을 방지하고 상태를 관리하는지 테스트.
* **Settings:**
  * **Endpoint:** `/api/dostk/ordr` (`kt10000`)
  * **Behavior:** `FixedDelay: 3000ms` (3초 지연 후 응답)
* **Validation:** Aegis가 타임아웃 처리를 하거나, 비동기 응답을 대기하는 동안 스레드가 차단(Blocking)되지 않고 다른 작업을 수행하는지 확인.

#### [Scenario 2] API Error & Resilience (장애 대응)

* **Purpose:** 키움 서버 일시 장애 시 시스템 셧다운 방지.
* **Settings:**
  * **Case A (Rate Limit):** Status `429 (Too Many Requests)` -> `Retry-After` 헤더 확인 로직 검증.
  * **Case B (Server Fault):** Status `502 (Bad Gateway)` -> 재시도 3회 후 실패 처리 로직 검증.
* **Validation:** `ApiGatekeeper`가 에러를 감지하고 `Resilience4j`의 Circuit Breaker가 작동하는지 확인.

#### [Scenario 3] Partial Fill (부분 체결)

* **Purpose:** 주문 수량보다 적은 수량이 체결되었을 때 잔량 관리 로직 검증.
* **Simulation (WebSocket):**
  * **Order:** 10주 매수 주문.
  * **Mock Event 1:** 3주 체결 (`911`: 3, `902`: 7).
  * **Mock Event 2:** 7주 체결 (`911`: 7, `902`: 0).
* **Validation:** `AccountManager`가 부분 체결 메시지를 수신할 때마다 평균단가와 잔고를 정확히 갱신하는지 확인.

---

## 4. Google Gemini API (AI Reasoning)

* **Target:** 뉴스 감성 분석, 기술적 패턴 해석, 최종 매매 판단
* **Base URL:** `https://generativelanguage.googleapis.com`
* **Auth Type:** Header (`x-goog-api-key`)
* **Content-Type:** `application/json`

### 4.1 [POST] 콘텐츠 생성 (Generate Content)

* **Endpoint:** `/v1beta/models/{model}:generateContent`
* **Method:** `POST`
* **Path Parameter:**
  * `model`:
    * `gemini-2.5-flash-lite`: 뉴스/펀더멘털 분석 (Fast)
    * `gemini-2.5-flash`: 차트/심리/종합 분석 (Standard)
* **Request Body:**

    ```json
    {
      "contents": [
        {
          "role": "user",
          "parts": [
            {
              "text": "Analyze this stock news..."
            }
          ]
        }
      ],
      "generationConfig": {
        "temperature": 0.1,
        "topP": 0.8,
        "maxOutputTokens": 1024,
        "responseMimeType": "application/json"
      }
    }
    ```

* **Response Body:**

    ```json
    {
      "candidates": [
        {
          "content": {
            "parts": [
              {
                "text": "{ \"sentiment\": \"positive\", \"score\": 85 }"
              }
            ],
            "role": "model"
          },
          "finishReason": "STOP",
          "index": 0
        }
      ],
      "usageMetadata": {
        "promptTokenCount": 50,
        "candidatesTokenCount": 20,
        "totalTokenCount": 70
      }
    }
    ```
