/**
 * API 클라이언트 설정.
 */
const API_BASE_URL = '/api';

export async function apiClient<T>(
    endpoint: string,
    options?: RequestInit
): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        headers: {
            'Content-Type': 'application/json',
            ...options?.headers,
        },
        ...options,
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || `API Error: ${response.status}`);
    }

    const result = await response.json();
    return result.data;
}

// API 함수들
export const api = {
    // 계좌
    getBalance: () => apiClient<AccountBalance>('/accounts/balance'),
    getHoldings: () => apiClient<Holding[]>('/accounts/holdings'),

    // 추천 종목
    getStocks: (date?: string) =>
        apiClient<TargetStock[]>(`/stocks${date ? `?date=${date}` : ''}`),
    getStockAnalysis: (stockCode: string) =>
        apiClient<TargetStock>(`/stocks/${stockCode}/analysis`),

    // 매매 로그
    getTradeLogs: () => apiClient<TradeLog[]>('/trades'),
    manualSell: (data: ManualSellRequest) =>
        apiClient<void>('/trades/manual-sell', {
            method: 'POST',
            body: JSON.stringify(data),
        }),

    // 매매일지
    getJournals: () => apiClient<Journal[]>('/journals'),
    getJournalDetail: (date: string) => apiClient<Journal>(`/journals/${date}`),

    // 설정
    getSettings: () => apiClient<UserSetting>('/settings'),
    updateStrategy: (mode: string) =>
        apiClient<UserSetting>(`/settings/strategy?mode=${mode}`, { method: 'PUT' }),
    getRssFeeds: () => apiClient<RssFeed[]>('/settings/rss-feeds'),
    createRssFeed: (data: CreateRssFeedRequest) =>
        apiClient<RssFeed>('/settings/rss-feeds', {
            method: 'POST',
            body: JSON.stringify(data),
        }),
    deleteRssFeed: (id: number) =>
        apiClient<void>(`/settings/rss-feeds/${id}`, { method: 'DELETE' }),

    // 계좌 요약 (New)
    getAccountSummary: () => apiClient<AccountSummary>('/accounts/summary'),
};

// 타입 정의
export interface AccountSummary {
    accountNo: string;
    totalAsset: number;
    deposit: number;
    d2Deposit: number;
    dailyProfitLoss: number;
    dailyReturnRate: number;
    totalProfitLoss: number;
    totalReturnRate: number;
    holdingCount: number;
    holdings: Holding[];
    maxProfit: number;
    maxLoss: number;
    bestPerformer: string;
    worstPerformer: string;
}

export interface HoldingSummary {
    stockCode: string;
    stockName: string;
    profitRate: number;
}
export interface AccountBalance {
    accountNo: string;
    totalAsset: number;
    deposit: number;
    d2Deposit: number;
    dailyProfitLoss: number;
    dailyReturnRate: number;
}

export interface Holding {
    stockCode: string;
    stockName: string;
    quantity: number;
    avgPrice: number;
    currentPrice: number;
    profitLoss: number;
    profitRate: number;
    weight: number;
}

export interface TargetStock {
    stockCode: string;
    baseDate: string;
    stockName: string;
    decision: 'BUY' | 'WATCH' | 'REJECT';
    riskLevel: 'HIGH' | 'MEDIUM' | 'LOW';
    nexusScore: number;
    targetPrice: number;
    stopLoss: number;
    status: string;
    agentScores: Record<string, number>;
    nexusReason: string;
    // 분석 리포트용 추가 필드 (optional)
    sentinelScore?: number;
    axiomScore?: number;
    vectorScore?: number;
    resonanceScore?: number;
    sonarScore?: number;
    totalScore?: number;
    recommendation?: 'BUY' | 'WATCH' | 'REJECT';
    chartData?: CandlestickData[];
    nexusComment?: string;
}

// 캔들스틱 차트 데이터
export interface CandlestickData {
    date: string;
    open: number;
    high: number;
    low: number;
    close: number;
    volume?: number;
}

export interface TradeLog {
    id: number;
    stockCode: string;
    stockName: string;
    tradeType: 'BUY' | 'SELL';
    orderPrice: number;
    filledPrice: number;
    quantity: number;
    slippageRate: number;
    status: string;
    agentMsg: string;
    executedAt: string;
}

export interface Journal {
    id: number;
    date: string;
    totalProfitLoss: number;
    winRate: number;
    tradeCount: number;
    aiReviewContent?: string;
    improvementPoints?: string;
}

export interface UserSetting {
    strategyMode: 'AGGRESSIVE' | 'NEUTRAL' | 'STABLE';
    reEntryAllowed: boolean;
    maxLossPerTrade: number;
}

export interface RssFeed {
    id: number;
    name: string;
    url: string;
    category: 'DOMESTIC' | 'DISCLOSURE' | 'GLOBAL';
    isActive: boolean;
}

export interface ManualSellRequest {
    stockCode: string;
    quantity: number;
    reason?: string;
}

export interface CreateRssFeedRequest {
    name: string;
    url: string;
    category: string;
}
