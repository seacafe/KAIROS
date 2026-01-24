import { useSettings, useUpdateStrategy, useRssFeeds, useDeleteRssFeed } from '@/shared/api/hooks';
import { Trash2 } from 'lucide-react';

/**
 * 설정 페이지.
 */
export function SettingsPage() {
    const { data: settings } = useSettings();
    const { data: rssFeeds } = useRssFeeds();
    const updateStrategy = useUpdateStrategy();
    const deleteFeed = useDeleteRssFeed();

    const strategies = [
        { value: 'AGGRESSIVE', label: '공격형', desc: '높은 변동성 수용, 진입 임계값 50' },
        { value: 'NEUTRAL', label: '중립형', desc: '균형 전략, 진입 임계값 60' },
        { value: 'STABLE', label: '안정형', desc: '보수적 접근, 진입 임계값 70' },
    ];

    return (
        <div className="space-y-8 max-w-2xl">
            <div>
                <h1 className="text-2xl font-bold">설정</h1>
                <p className="text-muted-foreground">
                    투자 성향 및 시스템 설정을 관리합니다.
                </p>
            </div>

            {/* 투자 성향 */}
            <section className="rounded-xl border border-border bg-card p-6">
                <h2 className="text-lg font-semibold mb-4">투자 성향</h2>
                <div className="space-y-3">
                    {strategies.map((strategy) => (
                        <label
                            key={strategy.value}
                            className={`flex items-center justify-between rounded-lg border p-4 cursor-pointer transition-all ${settings?.strategyMode === strategy.value
                                    ? 'border-primary bg-primary/10'
                                    : 'border-border hover:border-muted-foreground'
                                }`}
                        >
                            <div>
                                <div className="font-medium">{strategy.label}</div>
                                <div className="text-sm text-muted-foreground">{strategy.desc}</div>
                            </div>
                            <input
                                type="radio"
                                name="strategy"
                                value={strategy.value}
                                checked={settings?.strategyMode === strategy.value}
                                onChange={() => updateStrategy.mutate(strategy.value)}
                                className="h-4 w-4 text-primary"
                            />
                        </label>
                    ))}
                </div>
            </section>

            {/* RSS 피드 관리 */}
            <section className="rounded-xl border border-border bg-card p-6">
                <h2 className="text-lg font-semibold mb-4">RSS 피드</h2>
                {rssFeeds && rssFeeds.length > 0 ? (
                    <div className="space-y-2">
                        {rssFeeds.map((feed) => (
                            <div
                                key={feed.id}
                                className="flex items-center justify-between rounded-lg bg-secondary/30 p-3"
                            >
                                <div>
                                    <div className="font-medium">{feed.name}</div>
                                    <div className="text-xs text-muted-foreground">{feed.category}</div>
                                </div>
                                <button
                                    onClick={() => deleteFeed.mutate(feed.id)}
                                    className="p-2 text-muted-foreground hover:text-red-400 transition-colors"
                                >
                                    <Trash2 className="h-4 w-4" />
                                </button>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className="text-muted-foreground">등록된 RSS 피드가 없습니다.</p>
                )}

                {/* TODO: RSS 추가 폼 */}
                <button className="mt-4 w-full rounded-lg border border-dashed border-muted-foreground py-3 text-muted-foreground hover:border-primary hover:text-primary transition-colors">
                    + RSS 피드 추가
                </button>
            </section>
        </div>
    );
}
