import { useJournals } from '@/shared/api/hooks';
import { NavLink } from 'react-router-dom';

/**
 * 매매일지 페이지.
 */
export function JournalPage() {
    const { data: journals, isLoading } = useJournals();

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold">매매일지</h1>
                <p className="text-muted-foreground">
                    AI가 분석한 일별 매매 복기입니다.
                </p>
            </div>

            {isLoading ? (
                <div className="flex h-48 items-center justify-center">로딩 중...</div>
            ) : journals && journals.length > 0 ? (
                <div className="space-y-4">
                    {journals.map((journal) => {
                        const isProfitable = journal.totalProfitLoss >= 0;

                        return (
                            <NavLink
                                key={journal.id}
                                to={`/journal/${journal.date}`}
                                className="block rounded-xl border border-border bg-card p-6 transition-all hover:border-primary/50"
                            >
                                <div className="flex items-center justify-between">
                                    <div>
                                        <h3 className="font-semibold">{journal.date}</h3>
                                        <p className="text-sm text-muted-foreground">
                                            {journal.tradeCount}건 거래 | 승률 {journal.winRate}%
                                        </p>
                                    </div>
                                    <div className="text-right">
                                        <div className={`text-xl font-bold font-mono ${isProfitable ? 'text-profit' : 'text-loss'
                                            }`}>
                                            {isProfitable ? '+' : ''}₩{journal.totalProfitLoss.toLocaleString()}
                                        </div>
                                    </div>
                                </div>
                            </NavLink>
                        );
                    })}
                </div>
            ) : (
                <div className="flex h-48 items-center justify-center rounded-xl border border-border bg-card text-muted-foreground">
                    매매일지가 없습니다.
                </div>
            )}
        </div>
    );
}
