package com.kairos.trading.domain.journal.mapper;

import com.kairos.trading.domain.journal.dto.JournalDto;
import com.kairos.trading.domain.journal.entity.Journal;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Journal 도메인 MapStruct Mapper.
 */
@Mapper(componentModel = "spring")
public interface JournalMapper {

    /**
     * Journal Entity → JournalDto 변환.
     */
    JournalDto toJournalDto(Journal journal);

    /**
     * Journal Entity 리스트 → JournalDto 리스트 변환.
     */
    List<JournalDto> toJournalDtoList(List<Journal> journals);
}
