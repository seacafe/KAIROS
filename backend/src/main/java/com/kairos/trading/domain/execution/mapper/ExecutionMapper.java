package com.kairos.trading.domain.execution.mapper;

import com.kairos.trading.domain.execution.dto.TradeLogDto;
import com.kairos.trading.domain.execution.entity.TradeLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Execution 도메인 MapStruct Mapper.
 */
@Mapper(componentModel = "spring")
public interface ExecutionMapper {

    /**
     * TradeLog Entity → TradeLogDto 변환.
     */
    TradeLogDto toTradeLogDto(TradeLog tradeLog);

    /**
     * TradeLog Entity 리스트 → TradeLogDto 리스트 변환.
     */
    List<TradeLogDto> toTradeLogDtoList(List<TradeLog> tradeLogs);
}
