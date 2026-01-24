package com.kairos.trading.domain.account.controller;

import com.kairos.trading.common.response.BaseResponse;
import com.kairos.trading.domain.account.dto.AccountBalanceDto;
import com.kairos.trading.domain.account.dto.HoldingDto;
import com.kairos.trading.domain.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 계좌 정보 API.
 * 
 * 역할: 요청 검증 및 응답 변환만 담당.
 * 비즈니스 로직은 AccountService에서 처리.
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 계좌 잔고 조회.
     */
    @GetMapping("/balance")
    public BaseResponse<AccountBalanceDto> getBalance() {
        log.debug("[API] 계좌 잔고 조회");
        return BaseResponse.success(accountService.getBalance());
    }

    /**
     * 보유 종목 조회.
     */
    @GetMapping("/holdings")
    public BaseResponse<List<HoldingDto>> getHoldings() {
        log.debug("[API] 보유 종목 조회");
        return BaseResponse.success(accountService.getHoldings());
    }
}
