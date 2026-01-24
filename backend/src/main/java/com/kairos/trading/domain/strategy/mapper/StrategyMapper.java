package com.kairos.trading.domain.strategy.mapper;

import com.kairos.trading.domain.strategy.dto.TargetStockDto;
import com.kairos.trading.domain.strategy.entity.TargetStock;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Strategy 도메인 MapStruct Mapper.
 */
@Mapper(componentModel = "spring")
public interface StrategyMapper {

    /**
     * TargetStock Entity → TargetStockDto 변환.
     */
    TargetStockDto toTargetStockDto(TargetStock targetStock);

    /**
     * TargetStock Entity 리스트 → TargetStockDto 리스트 변환.
     */
    List<TargetStockDto> toTargetStockDtoList(List<TargetStock> targetStocks);
}
