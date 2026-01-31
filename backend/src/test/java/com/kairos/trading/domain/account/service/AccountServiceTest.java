package com.kairos.trading.domain.account.service;

import com.kairos.trading.domain.account.dto.AccountBalanceDto;
import com.kairos.trading.domain.account.dto.HoldingDto;
import com.kairos.trading.domain.account.entity.Account;
import com.kairos.trading.domain.account.entity.Holding;
import com.kairos.trading.domain.account.mapper.AccountMapper;
import com.kairos.trading.domain.account.repository.AccountRepository;
import com.kairos.trading.domain.account.repository.HoldingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private AccountMapper accountMapper;

    @Test
    @DisplayName("getBalance: 계좌가 존재하면 Mapper를 통해 DTO를 반환해야 한다")
    void getBalance_ShouldReturnDto_WhenAccountExists() {
        // Given
        String accountNo = "123456";
        Account account = Account.builder()
                .accountNo(accountNo)
                .totalAsset(BigDecimal.valueOf(1000000))
                .build();

        AccountBalanceDto expectedDto = AccountBalanceDto.of(
                accountNo, BigDecimal.valueOf(1000000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        given(accountRepository.findByAccountNo(accountNo)).willReturn(Optional.of(account));
        given(accountMapper.toAccountBalanceDto(account)).willReturn(expectedDto);

        // When
        AccountBalanceDto result = accountService.getBalance(accountNo);

        // Then
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    @DisplayName("getBalance: 계좌가 없으면 예외를 던져야 한다")
    void getBalance_ShouldThrowException_WhenAccountNotFound() {
        // Given
        String accountNo = "Unknown";
        given(accountRepository.findByAccountNo(accountNo)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getBalance(accountNo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("계좌를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("getHoldings: 전체 보유 종목을 조회하여 DTO 리스트로 반환해야 한다")
    void getHoldings_ShouldReturnDtoList() {
        // Given
        List<Holding> holdings = List.of(Holding.builder().build());
        List<HoldingDto> expectedDtos = List
                .of(new HoldingDto("005930", "Samsung", 10, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, 0.0, 0.0));

        given(holdingRepository.findAll()).willReturn(holdings);
        given(accountMapper.toHoldingDtoList(holdings)).willReturn(expectedDtos);

        // When
        List<HoldingDto> result = accountService.getHoldings();

        // Then
        assertThat(result).isEqualTo(expectedDtos);
    }

    @Test
    @DisplayName("getBalance: 기본 계좌 조회 시 Repository를 호출해야 한다")
    void getBalance_Default_ShouldReturnDto() {
        // Given
        Account account = Account.builder().accountNo("default").build();
        given(accountRepository.findByAccountNo("default")).willReturn(Optional.of(account));
        given(accountMapper.toAccountBalanceDto(account)).willReturn(
                AccountBalanceDto.of("default", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        // When
        AccountBalanceDto result = accountService.getBalance();

        // Then
        assertThat(result.accountNo()).isEqualTo("default");
        verify(accountRepository).findByAccountNo("default");
    }

    @Test
    @DisplayName("getHoldings: 계좌 ID로 보유 종목 조회 시 Repository를 호출해야 한다")
    void getHoldings_ByAccountId_ShouldReturnDtoList() {
        // Given
        Long accountId = 1L;
        List<Holding> holdings = List.of(Holding.builder().build());
        given(holdingRepository.findByAccountId(accountId)).willReturn(holdings);
        given(accountMapper.toHoldingDtoList(holdings)).willReturn(List.of(
                new HoldingDto("005930", "Samsung", 10, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, 0.0, 0.0)));

        // When
        List<HoldingDto> result = accountService.getHoldings(accountId);

        // Then
        assertThat(result).hasSize(1);
        verify(holdingRepository).findByAccountId(accountId);
    }
}
