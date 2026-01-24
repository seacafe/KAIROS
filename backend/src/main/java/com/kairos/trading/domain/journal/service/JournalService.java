package com.kairos.trading.domain.journal.service;

import com.kairos.trading.domain.journal.dto.JournalDto;
import com.kairos.trading.domain.journal.entity.Journal;
import com.kairos.trading.domain.journal.mapper.JournalMapper;
import com.kairos.trading.domain.journal.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 매매일지 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalService {

    private final JournalRepository journalRepository;
    private final JournalMapper journalMapper;

    /**
     * 매매일지 목록 조회.
     */
    public List<JournalDto> getJournals(LocalDate startDate, LocalDate endDate) {
        List<Journal> journals;

        if (startDate != null && endDate != null) {
            journals = journalRepository.findByDateBetweenOrderByDateDesc(startDate, endDate);
        } else {
            journals = journalRepository.findTop30ByOrderByDateDesc();
        }

        return journalMapper.toJournalDtoList(journals);
    }

    /**
     * 특정 날짜의 매매일지 상세 조회.
     */
    public JournalDto getJournalByDate(LocalDate date) {
        return journalRepository.findByDate(date)
                .map(journalMapper::toJournalDto)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 매매일지가 없습니다: " + date));
    }

    /**
     * 매매일지 저장/업데이트.
     */
    @Transactional
    public JournalDto saveJournal(JournalDto dto) {
        Journal journal = journalRepository.findByDate(dto.date())
                .orElse(Journal.builder().date(dto.date()).build());

        journal.updateDailyStats(dto.totalProfitLoss(), dto.winRate(), dto.tradeCount());

        Journal saved = journalRepository.save(journal);
        return journalMapper.toJournalDto(saved);
    }

    /**
     * AI 복기 결과 업데이트.
     */
    @Transactional
    public void updateAiReview(LocalDate date, String reviewContent, String improvementPoints) {
        Journal journal = journalRepository.findByDate(date)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 매매일지가 없습니다: " + date));

        journal.updateAiReview(reviewContent, improvementPoints);
        journalRepository.save(journal);

        log.info("[JournalService] AI 복기 업데이트 완료: {}", date);
    }
}
