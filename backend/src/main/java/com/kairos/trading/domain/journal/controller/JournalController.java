package com.kairos.trading.domain.journal.controller;

import com.kairos.trading.common.response.BaseResponse;
import com.kairos.trading.domain.journal.dto.JournalDto;
import com.kairos.trading.domain.journal.service.JournalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 매매일지 API.
 * 
 * 역할: 요청 검증 및 응답 변환만 담당.
 * 비즈니스 로직은 JournalService에서 처리.
 */
@Slf4j
@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

        private final JournalService journalService;

        /**
         * 매매일지 목록 조회.
         */
        @GetMapping
        public BaseResponse<List<JournalDto>> getJournals(
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate) {
                log.debug("[API] 매매일지 목록 조회");

                LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
                LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

                return BaseResponse.success(journalService.getJournals(start, end));
        }

        /**
         * 매매일지 상세 조회 (AI 피드백 포함).
         */
        @GetMapping("/{date}")
        public BaseResponse<JournalDto> getJournalDetail(@PathVariable String date) {
                log.debug("[API] 매매일지 상세 조회: {}", date);

                LocalDate targetDate = LocalDate.parse(date);
                return BaseResponse.success(journalService.getJournalByDate(targetDate));
        }
}
