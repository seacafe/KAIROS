import { useMemo } from 'react';
import { Treemap, ResponsiveContainer, Tooltip } from 'recharts';
import type { Holding } from '@/shared/api/client';

interface PortfolioHeatmapProps {
    holdings: Holding[];
    height?: number;
}

/**
 * 포트폴리오 히트맵 컴포넌트.
 * 보유 종목을 수익률에 따라 색상으로 시각화.
 */
export function PortfolioHeatmap({ holdings, height = 300 }: PortfolioHeatmapProps) {
    // Treemap 데이터 변환
    const treeData = useMemo(() => {
        return holdings.map((holding) => ({
            name: holding.stockName,
            size: holding.quantity * holding.currentPrice, // 평가금액 계산
            profitRate: holding.profitRate,
            profitLoss: holding.profitLoss,
            // 수익률에 따른 색상
            fill: getColorByProfitRate(holding.profitRate),
        }));
    }, [holdings]);

    if (holdings.length === 0) {
        return (
            <div className="flex h-48 items-center justify-center rounded-xl border border-border bg-card text-muted-foreground">
                보유 종목이 없습니다.
            </div>
        );
    }

    return (
        <div className="w-full rounded-xl border border-border bg-card p-4" style={{ height }}>
            <h3 className="mb-4 text-lg font-semibold">자산 히트맵</h3>
            <ResponsiveContainer width="100%" height={height - 60}>
                <Treemap
                    data={treeData}
                    dataKey="size"
                    aspectRatio={4 / 3}
                    stroke="#1a1a2e"
                    content={<CustomTreemapContent />}
                >
                    <Tooltip
                        contentStyle={{
                            backgroundColor: '#1a1a2e',
                            border: '1px solid #333',
                            borderRadius: '8px',
                        }}
                        formatter={(value, _name, props) => [
                            `₩${(value as number)?.toLocaleString() ?? 0} (${props.payload.profitRate > 0 ? '+' : ''}${props.payload.profitRate?.toFixed(2) ?? 0}%)`,
                            props.payload.name,
                        ]}
                    />
                </Treemap>
            </ResponsiveContainer>
        </div>
    );
}

/**
 * 커스텀 Treemap 셀 렌더러.
 */
function CustomTreemapContent(props: any) {
    const { x, y, width, height, name, fill, profitRate } = props;

    if (width < 40 || height < 30) {
        return (
            <rect x={x} y={y} width={width} height={height} fill={fill} stroke="#1a1a2e" />
        );
    }

    return (
        <g>
            <rect x={x} y={y} width={width} height={height} fill={fill} stroke="#1a1a2e" />
            <text
                x={x + width / 2}
                y={y + height / 2 - 8}
                textAnchor="middle"
                fill="#fff"
                fontSize={width > 80 ? 12 : 10}
                fontWeight="bold"
            >
                {name}
            </text>
            <text
                x={x + width / 2}
                y={y + height / 2 + 10}
                textAnchor="middle"
                fill="#fff"
                fontSize={width > 80 ? 11 : 9}
            >
                {profitRate > 0 ? '+' : ''}{profitRate?.toFixed(1)}%
            </text>
        </g>
    );
}

/**
 * 수익률에 따른 색상 반환.
 */
function getColorByProfitRate(rate: number): string {
    if (rate >= 5) return '#10b981';      // 강한 상승 (녹색)
    if (rate >= 2) return '#34d399';      // 상승
    if (rate >= 0) return '#6ee7b7';      // 약한 상승
    if (rate >= -2) return '#fca5a5';     // 약한 하락
    if (rate >= -5) return '#f87171';     // 하락
    return '#ef4444';                      // 강한 하락 (빨간색)
}
