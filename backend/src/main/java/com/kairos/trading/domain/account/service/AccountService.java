package com.kairos.trading.domain.account.service;

import com.kairos.trading.domain.account.dto.AccountBalanceDto;
import com.kairos.trading.domain.account.dto.HoldingDto;
import com.kairos.trading.domain.account.entity.Account;
import com.kairos.trading.domain.account.entity.Holding;
import com.kairos.trading.domain.account.mapper.AccountMapper;
import com.kairos.trading.domain.account.repository.AccountRepository;
import com.kairos.trading.domain.account.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 계좌 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final AccountMapper accountMapper;

    /**
     * 기본 계좌 잔고 조회.
     */
    public AccountBalanceDto getBalance() {
        // TODO: 실제 구현 시 Kiwoom API 연동 또는 DB 조회
        return accountRepository.findByAccountNo("default")
                .map(accountMapper::toAccountBalanceDto)
                .orElseGet(this::getDefaultBalance);
    }

    /**
     * 계좌번호로 잔고 조회.
     */
    public AccountBalanceDto getBalance(String accountNo) {
        return accountRepository.findByAccountNo(accountNo)
                .map(accountMapper::toAccountBalanceDto)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다: " + accountNo));
    }

    /**
     * 보유 종목 조회.
     */
    public List<HoldingDto> getHoldings() {
        // TODO: 실제 구현 시 계좌 ID 조회 후 보유종목 조회
        List<Holding> holdings = holdingRepository.findAll();
        return accountMapper.toHoldingDtoList(holdings);
    }

    /**
     * 계좌 ID로 보유 종목 조회.
     */
    public List<HoldingDto> getHoldings(Long accountId) {
        List<Holding> holdings = holdingRepository.findByAccountId(accountId);
        return accountMapper.toHoldingDtoList(holdings);
    }

    private AccountBalanceDto getDefaultBalance() {
        return AccountBalanceDto.of(
                "default",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }
}
