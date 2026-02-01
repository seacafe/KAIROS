import { useStocks, useAccountSummary, useTradeLogs, useHoldings } from '@/shared/api/hooks';
import { TargetStockCard } from '@/features/dashboard/TargetStockCard';
import { PortfolioHeatmap } from '@/features/dashboard/PortfolioHeatmap';
import { RealtimeLogViewer } from '@/features/dashboard/RealtimeLogViewer';
import { AccountSummaryCard } from '@/features/dashboard/AccountSummaryCard';
import { useAccountStore } from '@/stores/accountStore';
import { useEffect } from 'react';

/**
 * 대시보드 페이지.
 */
export function DashboardPage() {
    const { data: stocks, isLoading: stocksLoading } = useStocks();
    const { data: summary } = useAccountSummary();
    const { data: tradeLogs } = useTradeLogs();
    const { data: holdings } = useHoldings();

    const setAccount = useAccountStore((state) => state.setAccount);

    // 잔고 정보를 전역 스토어에 반영
    useEffect(() => {
        if (summary) {
            setAccount({
                totalAsset: summary.totalAsset,
                deposit: summary.deposit,
                dailyProfit: summary.dailyProfitLoss,
            });
        }
    }, [summary, setAccount]);

    return (
        <div className="space-y-6">
            {/* 페이지 타이틀 */}
            <div>
                <h1 className="text-2xl font-bold">Dashboard</h1>
                <p className="text-muted-foreground">
                    7인 AI 에이전트가 분석한 오늘의 추천 종목입니다.
                </p>
            </div>

            {/* 계좌 요약 (New) */}
            <AccountSummaryCard />

            {/* 그리드 레이아웃 */}
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">

                {/* 추천 종목 카드 */}
                <div className="col-span-2">
                    <h2 className="mb-4 text-lg font-semibold">추천 종목</h2>
                    {stocksLoading ? (
                        <div className="flex h-48 items-center justify-center text-muted-foreground">
                            로딩 중...
                        </div>
                    ) : stocks && stocks.length > 0 ? (
                        <div className="grid gap-4 md:grid-cols-2">
                            {stocks.map((stock) => (
                                <TargetStockCard key={stock.stockCode} target={stock} />
                            ))}
                        </div>
                    ) : (
                        <div className="flex h-48 items-center justify-center rounded-xl border border-border bg-card text-muted-foreground">
                            추천 종목이 없습니다. 장 시작 후 분석이 진행됩니다.
                        </div>
                    )}
                </div>

                {/* 에이전트 상태 */}
                <div className="rounded-xl border border-border bg-card p-6">
                    <h2 className="mb-4 text-lg font-semibold">에이전트 상태</h2>
                    <div className="space-y-3">
                        {['Sentinel', 'Axiom', 'Vector', 'Resonance', 'Sonar', 'Nexus', 'Aegis'].map((agent) => (
                            <div key={agent} className="flex items-center justify-between">
                                <span className="text-sm">{agent}</span>
                                <span className="h-2 w-2 rounded-full bg-green-500" />
                            </div>
                        ))}
                    </div>
                </div>

                {/* 자산 히트맵 */}
                <div className="col-span-2">
                    <PortfolioHeatmap
                        holdings={holdings || []}
                        height={280}
                    />
                </div>

                {/* 실시간 로그 */}
                <div>
                    <RealtimeLogViewer />
                </div>

                {/* 최근 매매 */}
                <div className="col-span-3 rounded-xl border border-border bg-card p-6">
                    <h2 className="mb-4 text-lg font-semibold">최근 매매</h2>
                    {tradeLogs && tradeLogs.length > 0 ? (
                        <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                            {tradeLogs.slice(0, 6).map((log) => (
                                <div key={log.id} className="flex items-center justify-between rounded-lg bg-secondary/30 p-3">
                                    <div className="flex items-center gap-3">
                                        <span className={`text-xs font-semibold px-2 py-0.5 rounded ${log.tradeType === 'BUY' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                                            }`}>
                                            {log.tradeType}
                                        </span>
                                        <span className="font-medium">{log.stockName}</span>
                                    </div>
                                    <div className="text-right">
                                        <div className="font-mono">₩{log.filledPrice.toLocaleString()}</div>
                                        <div className="text-xs text-muted-foreground">{log.quantity}주</div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="flex h-24 items-center justify-center text-muted-foreground">
                            당일 매매 내역이 없습니다.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

