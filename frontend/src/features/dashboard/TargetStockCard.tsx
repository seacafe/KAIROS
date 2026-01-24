import type { TargetStock } from '@/shared/api/client';

interface AgentScoreBadgeProps {
    name: string;
    score: number;
}

/**
 * 에이전트 점수 배지.
 */
function AgentScoreBadge({ name, score }: AgentScoreBadgeProps) {
    const colorClass =
        score >= 70 ? 'bg-green-500/20 text-green-400' :
            score >= 50 ? 'bg-yellow-500/20 text-yellow-400' :
                'bg-red-500/20 text-red-400';

    return (
        <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs ${colorClass}`}>
            {name}
            <span className="font-mono font-semibold">{score}</span>
        </span>
    );
}

interface TargetStockCardProps {
    target: TargetStock;
    onClick?: () => void;
}

/**
 * 추천 종목 카드.
 */
export function TargetStockCard({ target, onClick }: TargetStockCardProps) {
    const decisionColor = {
        BUY: 'border-green-500 bg-green-500/10',
        WATCH: 'border-yellow-500 bg-yellow-500/10',
        REJECT: 'border-red-500 bg-red-500/10',
    }[target.decision];

    const riskBadge = {
        HIGH: 'text-red-400',
        MEDIUM: 'text-yellow-400',
        LOW: 'text-green-400',
    }[target.riskLevel];

    return (
        <div
            className={`rounded-xl border-2 p-4 transition-all hover:scale-[1.02] cursor-pointer ${decisionColor}`}
            onClick={onClick}
        >
            {/* 헤더 */}
            <div className="flex items-center justify-between mb-3">
                <div>
                    <h3 className="font-semibold text-lg">{target.stockName}</h3>
                    <span className="text-xs text-muted-foreground font-mono">{target.stockCode}</span>
                </div>
                <div className="text-right">
                    <div className="text-2xl font-bold text-primary">{target.nexusScore}</div>
                    <span className={`text-xs ${riskBadge}`}>{target.riskLevel} Risk</span>
                </div>
            </div>

            {/* 에이전트 점수 */}
            <div className="flex flex-wrap gap-1 mb-3">
                {Object.entries(target.agentScores).map(([name, score]) => (
                    <AgentScoreBadge key={name} name={name} score={score} />
                ))}
            </div>

            {/* 가격 정보 */}
            <div className="flex justify-between text-sm">
                <div>
                    <span className="text-muted-foreground">목표가</span>
                    <span className="ml-2 font-mono text-profit">₩{target.targetPrice.toLocaleString()}</span>
                </div>
                <div>
                    <span className="text-muted-foreground">손절가</span>
                    <span className="ml-2 font-mono text-loss">₩{target.stopLoss.toLocaleString()}</span>
                </div>
            </div>

            {/* 판정 사유 (간략) */}
            <p className="mt-3 text-xs text-muted-foreground line-clamp-2">
                {target.nexusReason.split('\n')[0]}
            </p>
        </div>
    );
}
