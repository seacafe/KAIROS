import { useTargetStocks, useBalance, useTradeLogs } from '@/shared/api/hooks';
import { TargetStockCard } from '@/features/dashboard/TargetStockCard';
import { useAccountStore } from '@/stores/accountStore';
import { useEffect } from 'react';

/**
 * 대시보드 페이지.
 */
export function DashboardPage() {
    const { data: targets, isLoading: targetsLoading } = useTargetStocks();
    const { data: balance } = useBalance();
    const { data: tradeLogs } = useTradeLogs();

    const setAccount = useAccountStore((state) => state.setAccount);

    // 잔고 정보를 전역 스토어에 반영
    useEffect(() => {
        if (balance) {
            setAccount({
                totalAsset: balance.totalAsset,
                deposit: balance.deposit,
                dailyProfit: balance.dailyProfitLoss,
            });
        }
    }, [balance, setAccount]);

    return (
        <div className="space-y-6">
            {/* 페이지 타이틀 */}
            <div>
                <h1 className="text-2xl font-bold">Dashboard</h1>
                <p className="text-muted-foreground">
                    7인 AI 에이전트가 분석한 오늘의 추천 종목입니다.
                </p>
            </div>

            {/* 그리드 레이아웃 */}
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">

                {/* 추천 종목 카드 */}
                <div className="col-span-2">
                    <h2 className="mb-4 text-lg font-semibold">추천 종목</h2>
                    {targetsLoading ? (
                        <div className="flex h-48 items-center justify-center text-muted-foreground">
                            로딩 중...
                        </div>
                    ) : targets && targets.length > 0 ? (
                        <div className="grid gap-4 md:grid-cols-2">
                            {targets.map((target) => (
                                <TargetStockCard key={target.id} target={target} />
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

                {/* 최근 매매 */}
                <div className="col-span-2 rounded-xl border border-border bg-card p-6">
                    <h2 className="mb-4 text-lg font-semibold">최근 매매</h2>
                    {tradeLogs && tradeLogs.length > 0 ? (
                        <div className="space-y-2">
                            {tradeLogs.slice(0, 5).map((log) => (
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
                        <div className="flex h-32 items-center justify-center text-muted-foreground">
                            당일 매매 내역이 없습니다.
                        </div>
                    )}
                </div>

                {/* 실시간 로그 */}
                <div className="rounded-xl border border-border bg-card p-6">
                    <h2 className="mb-4 text-lg font-semibold">실시간 로그</h2>
                    <div className="h-48 overflow-auto font-mono text-xs text-muted-foreground">
                        <div className="space-y-1">
                            <div>[{new Date().toLocaleTimeString()}] 시스템 대기 중...</div>
                            <div>[{new Date(Date.now() - 30000).toLocaleTimeString()}] KAIROS 시작됨</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
