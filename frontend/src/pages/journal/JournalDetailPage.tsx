import { useParams, useNavigate } from 'react-router-dom';
import { useJournalDetail } from '@/shared/api/hooks';
import { ArrowLeft } from 'lucide-react';
import ReactMarkdown from 'react-markdown';

/**
 * 매매일지 상세 페이지.
 * AI 복기 결과를 markdown으로 렌더링한다.
 */
export function JournalDetailPage() {
    const { date } = useParams<{ date: string }>();
    const navigate = useNavigate();
    const { data: journal, isLoading, error } = useJournalDetail(date || '');

    if (isLoading) {
        return (
            <div className="flex h-64 items-center justify-center text-muted-foreground">
                로딩 중...
            </div>
        );
    }

    if (error || !journal) {
        return (
            <div className="flex h-64 flex-col items-center justify-center gap-4">
                <p className="text-muted-foreground">해당 날짜의 매매일지를 찾을 수 없습니다.</p>
                <button
                    onClick={() => navigate('/journal')}
                    className="text-primary hover:underline"
                >
                    목록으로 돌아가기
                </button>
            </div>
        );
    }

    const isProfitable = journal.totalProfitLoss >= 0;
    const improvementTags = journal.improvementPoints
        ? JSON.parse(journal.improvementPoints)
        : [];

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center gap-4">
                <button
                    onClick={() => navigate('/journal')}
                    className="rounded-lg p-2 hover:bg-secondary transition-colors"
                >
                    <ArrowLeft className="h-5 w-5" />
                </button>
                <div>
                    <h1 className="text-2xl font-bold">{journal.date}</h1>
                    <p className="text-muted-foreground">
                        {journal.tradeCount}건 거래 | 승률 {journal.winRate}%
                    </p>
                </div>
            </div>

            {/* 요약 카드 */}
            <div className="grid gap-4 md:grid-cols-3">
                <div className="rounded-xl border border-border bg-card p-6">
                    <p className="text-sm text-muted-foreground">총 손익</p>
                    <p className={`text-2xl font-bold font-mono ${isProfitable ? 'text-profit' : 'text-loss'}`}>
                        {isProfitable ? '+' : ''}₩{journal.totalProfitLoss.toLocaleString()}
                    </p>
                </div>
                <div className="rounded-xl border border-border bg-card p-6">
                    <p className="text-sm text-muted-foreground">승률</p>
                    <p className="text-2xl font-bold font-mono">{journal.winRate}%</p>
                </div>
                <div className="rounded-xl border border-border bg-card p-6">
                    <p className="text-sm text-muted-foreground">거래 수</p>
                    <p className="text-2xl font-bold font-mono">{journal.tradeCount}건</p>
                </div>
            </div>

            {/* AI 복기 */}
            {journal.aiReviewContent && (
                <section className="rounded-xl border border-border bg-card p-6">
                    <h2 className="mb-4 text-lg font-semibold">🤖 AI 복기</h2>
                    <div className="prose prose-invert max-w-none">
                        <ReactMarkdown>{journal.aiReviewContent}</ReactMarkdown>
                    </div>
                </section>
            )}

            {/* 개선점 태그 */}
            {improvementTags.length > 0 && (
                <section className="rounded-xl border border-border bg-card p-6">
                    <h2 className="mb-4 text-lg font-semibold">📌 개선점</h2>
                    <div className="flex flex-wrap gap-2">
                        {improvementTags.map((tag: string, idx: number) => (
                            <span
                                key={idx}
                                className="rounded-full bg-primary/20 px-4 py-1.5 text-sm text-primary"
                            >
                                {tag}
                            </span>
                        ))}
                    </div>
                </section>
            )}
        </div>
    );
}
