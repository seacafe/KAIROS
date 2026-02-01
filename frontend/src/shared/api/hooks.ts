import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from './client';
import { queryKeys } from './queryKeys';

/**
 * 계좌 잔고 조회.
 */
export function useBalance() {
    return useQuery({
        queryKey: queryKeys.accounts.balance(),
        queryFn: api.getBalance,
        refetchInterval: 60000, // 1분 주기
    });
}

/**
 * 계좌 요약 정보 조회 (종합).
 */
export function useAccountSummary() {
    return useQuery({
        queryKey: queryKeys.accounts.summary(),
        queryFn: api.getAccountSummary,
        refetchInterval: 60000, // 1분 주기
    });
}

/**
 * 보유 종목 조회.
 */
export function useHoldings() {
    return useQuery({
        queryKey: queryKeys.accounts.all,
        queryFn: api.getHoldings,
        refetchInterval: 30000, // 30초 주기
    });
}

/**
 * AI 추천 종목 조회.
 */
export function useStocks(date?: string) {
    return useQuery({
        queryKey: queryKeys.stocks.list(date),
        queryFn: () => api.getStocks(date),
    });
}

/**
 * 종목 상세 분석 조회.
 */
export function useStockAnalysis(stockCode: string) {
    return useQuery({
        queryKey: queryKeys.stocks.analysis(stockCode),
        queryFn: () => api.getStockAnalysis(stockCode),
        enabled: !!stockCode,
    });
}

/**
 * 당일 매매 로그 조회.
 */
export function useTradeLogs() {
    return useQuery({
        queryKey: queryKeys.trades.logs(),
        queryFn: api.getTradeLogs,
    });
}

/**
 * 수동 매도.
 */
export function useManualSell() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: api.manualSell,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.trades.all });
            queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all });
        },
    });
}

/**
 * 매매일지 목록 조회.
 */
export function useJournals() {
    return useQuery({
        queryKey: queryKeys.journals.all,
        queryFn: api.getJournals,
    });
}

/**
 * 매매일지 상세 조회.
 */
export function useJournalDetail(date: string) {
    return useQuery({
        queryKey: queryKeys.journals.detail(date),
        queryFn: () => api.getJournalDetail(date),
        enabled: !!date,
    });
}

/**
 * 사용자 설정 조회.
 */
export function useSettings() {
    return useQuery({
        queryKey: queryKeys.settings.all,
        queryFn: api.getSettings,
    });
}

/**
 * 투자 성향 변경.
 */
export function useUpdateStrategy() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: api.updateStrategy,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.settings.all });
        },
    });
}

/**
 * RSS 피드 목록 조회.
 */
export function useRssFeeds() {
    return useQuery({
        queryKey: queryKeys.settings.rssFeeds(),
        queryFn: api.getRssFeeds,
    });
}

/**
 * RSS 피드 추가.
 */
export function useCreateRssFeed() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: api.createRssFeed,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.settings.rssFeeds() });
        },
    });
}

/**
 * RSS 피드 삭제.
 */
export function useDeleteRssFeed() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: api.deleteRssFeed,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.settings.rssFeeds() });
        },
    });
}
