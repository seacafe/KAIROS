/**
 * TanStack Query 키 팩토리
 * 모든 쿼리 키를 중앙에서 관리하여 중복 방지
 */
export const queryKeys = {
    // 계좌 관련
    accounts: {
        all: ['accounts'] as const,
        detail: (id: string) => ['accounts', id] as const,
        balance: () => ['accounts', 'balance'] as const,
        summary: () => ['accounts', 'summary'] as const,
    },

    // 종목 관련
    stocks: {
        all: ['stocks'] as const,
        detail: (code: string) => ['stocks', code] as const,
        list: (date?: string) => ['stocks', 'list', date] as const,
        analysis: (code: string) => ['stocks', 'analysis', code] as const,
    },

    // 매매 관련
    trades: {
        all: ['trades'] as const,
        logs: (date?: string) => ['trades', 'logs', date] as const,
        pending: () => ['trades', 'pending'] as const,
    },

    // 매매일지
    journals: {
        all: ['journals'] as const,
        detail: (date: string) => ['journals', date] as const,
    },

    // 에이전트
    agents: {
        status: () => ['agents', 'status'] as const,
        reports: (agentName: string) => ['agents', 'reports', agentName] as const,
    },

    // 설정
    settings: {
        all: ['settings'] as const,
        rssFeeds: () => ['settings', 'rssFeeds'] as const,
    },
}
