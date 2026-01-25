import { useMemo } from 'react';
import {
    ComposedChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    ResponsiveContainer,
    ReferenceLine,
    Cell,
} from 'recharts';

interface CandlestickData {
    date: string;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
    // 매매 타점 (optional)
    buyPoint?: number;
    sellPoint?: number;
}

interface CandlestickChartProps {
    data: CandlestickData[];
    height?: number;
}

/**
 * 캔들스틱 차트 컴포넌트.
 * Recharts를 활용한 주가 차트 + 매매 타점 표시.
 */
export function CandlestickChart({ data, height = 400 }: CandlestickChartProps) {
    // 캔들 데이터 변환 (Recharts Bar 형식)
    const chartData = useMemo(() => {
        return data.map((item) => ({
            ...item,
            // 캔들 바디 (open-close 구간)
            candleBody: [item.open, item.close],
            // 상승/하락 여부
            isUp: item.close >= item.open,
            // 윅 (high-low 구간)
            wick: [item.low, item.high],
        }));
    }, [data]);

    // 가격 범위 계산
    const { minPrice, maxPrice } = useMemo(() => {
        if (data.length === 0) return { minPrice: 0, maxPrice: 100 };
        const prices = data.flatMap((d) => [d.high, d.low]);
        const min = Math.min(...prices);
        const max = Math.max(...prices);
        const padding = (max - min) * 0.1;
        return { minPrice: min - padding, maxPrice: max + padding };
    }, [data]);

    if (data.length === 0) {
        return (
            <div className="flex h-64 items-center justify-center text-muted-foreground">
                차트 데이터가 없습니다.
            </div>
        );
    }

    return (
        <div className="w-full" style={{ height }}>
            <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                    <XAxis
                        dataKey="date"
                        tick={{ fill: '#888', fontSize: 11 }}
                        axisLine={{ stroke: '#333' }}
                    />
                    <YAxis
                        domain={[minPrice, maxPrice]}
                        tick={{ fill: '#888', fontSize: 11 }}
                        axisLine={{ stroke: '#333' }}
                        tickFormatter={(value) => value.toLocaleString()}
                    />
                    <Tooltip
                        contentStyle={{
                            backgroundColor: '#1a1a2e',
                            border: '1px solid #333',
                            borderRadius: '8px',
                        }}
                        labelStyle={{ color: '#888' }}
                        formatter={(value: number) => [`₩${value.toLocaleString()}`, '']}
                    />

                    {/* 캔들 바디 */}
                    <Bar dataKey="candleBody" barSize={8}>
                        {chartData.map((entry, index) => (
                            <Cell
                                key={`cell-${index}`}
                                fill={entry.isUp ? '#10b981' : '#ef4444'}
                            />
                        ))}
                    </Bar>

                    {/* 매수 타점 */}
                    {chartData.map((entry, index) =>
                        entry.buyPoint ? (
                            <ReferenceLine
                                key={`buy-${index}`}
                                y={entry.buyPoint}
                                stroke="#10b981"
                                strokeDasharray="3 3"
                                label={{ value: 'BUY', fill: '#10b981', fontSize: 10 }}
                            />
                        ) : null
                    )}

                    {/* 매도 타점 */}
                    {chartData.map((entry, index) =>
                        entry.sellPoint ? (
                            <ReferenceLine
                                key={`sell-${index}`}
                                y={entry.sellPoint}
                                stroke="#ef4444"
                                strokeDasharray="3 3"
                                label={{ value: 'SELL', fill: '#ef4444', fontSize: 10 }}
                            />
                        ) : null
                    )}
                </ComposedChart>
            </ResponsiveContainer>
        </div>
    );
}
