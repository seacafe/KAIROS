package com.kairos.trading.domain.account.mapper;

import com.kairos.trading.domain.account.dto.AccountBalanceDto;
import com.kairos.trading.domain.account.dto.HoldingDto;
import com.kairos.trading.domain.account.entity.Account;
import com.kairos.trading.domain.account.entity.Holding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * Account 도메인 MapStruct Mapper.
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    /**
     * Account Entity → AccountBalanceDto 변환.
     */
    @Mapping(target = "dailyProfitLoss", source = "dailyProfit")
    @Mapping(target = "dailyReturnRate", expression = "java(calculateReturnRate(account))")
    AccountBalanceDto toAccountBalanceDto(Account account);

    /**
     * Holding Entity → HoldingDto 변환.
     */
    @Mapping(target = "weight", constant = "0.0")
    HoldingDto toHoldingDto(Holding holding);

    /**
     * Holding Entity 리스트 → HoldingDto 리스트 변환.
     */
    List<HoldingDto> toHoldingDtoList(List<Holding> holdings);

    /**
     * 당일 수익률 계산.
     */
    default double calculateReturnRate(Account account) {
        if (account.getTotalAsset() == null || account.getDailyProfit() == null) {
            return 0.0;
        }
        BigDecimal prevAsset = account.getTotalAsset().subtract(account.getDailyProfit());
        if (prevAsset.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return account.getDailyProfit()
                .divide(prevAsset, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
