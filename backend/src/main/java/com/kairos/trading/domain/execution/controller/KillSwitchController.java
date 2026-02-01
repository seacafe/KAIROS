package com.kairos.trading.domain.execution.controller;

import com.kairos.trading.common.response.BaseResponse;
import com.kairos.trading.common.websocket.WebSocketMessageService;
import com.kairos.trading.domain.execution.service.TradeExecutionService;
import com.kairos.trading.domain.strategy.dto.ExecutionOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Kill Switch 컨트롤러.
 * 긴급 상황 시 전체 또는 특정 종목 일괄 매도 처리.
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class KillSwitchController {

        private final TradeExecutionService executionService;
        private final WebSocketMessageService wsMessageService;

        /**
         * Kill Switch 발동 - 전체 보유 종목 긴급 매도.
         * 
         * @param request reason: 발동 사유
         */
        @PostMapping("/kill-switch")
        public BaseResponse<Map<String, Object>> activateKillSwitch(
                        @RequestBody Map<String, String> request) {
                String reason = request.getOrDefault("reason", "사용자 요청");
                log.warn("[KillSwitch] 발동! 사유: {}", reason);

                // Kill Switch 주문 생성 및 실행
                var killOrder = ExecutionOrder.killSwitchSell(
                                "ALL", "전체 종목", 0, reason);
                executionService.submitOrder(killOrder);

                // WebSocket 알림 전송
                wsMessageService.sendKillSwitchAlert(reason, "ALL");

                return BaseResponse.success(Map.of(
                                "status", "KILL_SWITCH_ACTIVATED",
                                "reason", reason,
                                "message", "긴급 매도 명령이 실행되었습니다."));
        }

        /**
         * 특정 종목 긴급 매도.
         * 
         * @param stockCode 종목코드
         * @param request   reason: 발동 사유, quantity: 매도 수량
         */
        @PostMapping("/kill-switch/{stockCode}")
        public BaseResponse<Map<String, Object>> activateKillSwitchForStock(
                        @PathVariable String stockCode,
                        @RequestBody Map<String, Object> request) {
                String reason = (String) request.getOrDefault("reason", "사용자 요청");
                int quantity = request.containsKey("quantity")
                                ? ((Number) request.get("quantity")).intValue()
                                : 0; // 0 means all

                log.warn("[KillSwitch] 종목별 발동! 종목: {}, 수량: {}, 사유: {}", stockCode, quantity, reason);

                // Kill Switch 주문 생성 및 실행
                var killOrder = ExecutionOrder.killSwitchSell(
                                stockCode, stockCode, quantity, reason);
                executionService.submitOrder(killOrder);

                // WebSocket 알림 전송
                wsMessageService.sendKillSwitchAlert(reason, stockCode);

                return BaseResponse.success(Map.of(
                                "status", "KILL_SWITCH_ACTIVATED",
                                "stockCode", stockCode,
                                "quantity", quantity,
                                "reason", reason,
                                "message", stockCode + " 긴급 매도 명령이 실행되었습니다."));
        }
}
