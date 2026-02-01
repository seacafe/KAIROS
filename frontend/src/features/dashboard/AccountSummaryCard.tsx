import { useAccountSummary } from '@/shared/api/hooks';
import { TrendingUp, TrendingDown, Wallet, PieChart } from 'lucide-react';

export function AccountSummaryCard() {
    const { data: summary, isLoading } = useAccountSummary();

    if (isLoading) {
        return (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {[1, 2, 3, 4].map((i) => (
                    <div key={i} className="rounded-xl border border-border bg-card p-6">
                        <div className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <div className="text-sm font-medium">로딩 중</div>
                        </div>
                        <div className="mt-2 h-8 w-24 animate-pulse rounded bg-muted"></div>
                    </div>
                ))}
            </div>
        );
    }

    if (!summary) {
        return (
            <div className="rounded-xl border border-border bg-card p-6 text-center text-muted-foreground">
                계좌 정보를 불러올 수 없습니다.
            </div>
        );
    }

    const isProfit = summary.totalProfitLoss >= 0;
    const isDailyProfit = summary.dailyProfitLoss >= 0;

    return (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {/* 총 자산 */}
            <div className="rounded-xl border border-border bg-card p-6">
                <div className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <span className="text-sm font-medium">총 자산</span>
                    <Wallet className="h-4 w-4 text-muted-foreground" />
                </div>
                <div className="mt-2">
                    <div className="text-2xl font-bold">₩{summary.totalAsset.toLocaleString()}</div>
                    <p className="text-xs text-muted-foreground mt-1">
                        예수금: ₩{summary.deposit.toLocaleString()}
                    </p>
                </div>
            </div>

            {/* 총 손익 */}
            <div className="rounded-xl border border-border bg-card p-6">
                <div className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <span className="text-sm font-medium">총 손익</span>
                    <TrendingUp className={`h-4 w-4 ${isProfit ? 'text-green-500' : 'text-red-500'}`} />
                </div>
                <div className="mt-2">
                    <div className={`text-2xl font-bold ${isProfit ? 'text-green-500' : 'text-red-500'}`}>
                        {isProfit ? '+' : ''}₩{summary.totalProfitLoss.toLocaleString()}
                    </div>
                    <p className={`text-xs mt-1 ${isProfit ? 'text-green-500' : 'text-red-500'}`}>
                        {isProfit ? '+' : ''}{summary.totalReturnRate.toFixed(2)}%
                    </p>
                </div>
            </div>

            {/* 당일 손익 */}
            <div className="rounded-xl border border-border bg-card p-6">
                <div className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <span className="text-sm font-medium">당일 손익</span>
                    <TrendingDown className={`h-4 w-4 ${isDailyProfit ? 'text-green-500' : 'text-red-500'}`} />
                </div>
                <div className="mt-2">
                    <div className={`text-2xl font-bold ${isDailyProfit ? 'text-green-500' : 'text-red-500'}`}>
                        {isDailyProfit ? '+' : ''}₩{summary.dailyProfitLoss.toLocaleString()}
                    </div>
                    <p className={`text-xs mt-1 ${isDailyProfit ? 'text-green-500' : 'text-red-500'}`}>
                        {isDailyProfit ? '+' : ''}{summary.dailyReturnRate.toFixed(2)}%
                    </p>
                </div>
            </div>

            {/* 보유 종목 */}
            <div className="rounded-xl border border-border bg-card p-6">
                <div className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <span className="text-sm font-medium">보유 종목</span>
                    <PieChart className="h-4 w-4 text-muted-foreground" />
                </div>
                <div className="mt-2">
                    <div className="text-2xl font-bold">{summary.holdingCount}개</div>
                    <p className="text-xs text-muted-foreground mt-1">
                        최고 수익: {summary.bestPerformer || '-'}
                    </p>
                </div>
            </div>
        </div>
    );
}
