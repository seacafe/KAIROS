import { useState } from 'react';
import { useStockAnalysis } from '@/shared/api/hooks';
import { CandlestickChart } from '@/features/dashboard/CandlestickChart';
import { Search, TrendingUp, TrendingDown, AlertTriangle, CheckCircle } from 'lucide-react';

interface AgentAnalysis {
    agent: string;
    score: number;
    summary: string;
    signals: string[];
}

/**
 * Deep Analysis 페이지.
 * 종목 검색 후 5인 에이전트 분석 결과를 표시.
 */
export function DeepAnalysisPage() {
    const [stockCode, setStockCode] = useState('');
    const [searchedCode, setSearchedCode] = useState('');
    const { data: analysis, isLoading, error } = useStockAnalysis(searchedCode);

    const handleSearch = () => {
        if (stockCode.trim()) {
            setSearchedCode(stockCode.trim());
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleSearch();
        }
    };

    // 에이전트별 색상
    const getAgentColor = (agent: string) => {
        const colors: Record<string, string> = {
            Sentinel: 'border-purple-500 bg-purple-500/10',
            Axiom: 'border-cyan-500 bg-cyan-500/10',
            Vector: 'border-green-500 bg-green-500/10',
            Resonance: 'border-pink-500 bg-pink-500/10',
            Sonar: 'border-orange-500 bg-orange-500/10',
        };
        return colors[agent] || 'border-gray-500 bg-gray-500/10';
    };

    // 점수에 따른 아이콘
    const getScoreIcon = (score: number) => {
        if (score >= 70) return <CheckCircle className="h-5 w-5 text-green-400" />;
        if (score >= 50) return <TrendingUp className="h-5 w-5 text-yellow-400" />;
        if (score >= 30) return <TrendingDown className="h-5 w-5 text-orange-400" />;
        return <AlertTriangle className="h-5 w-5 text-red-400" />;
    };

    // Mock 에이전트 분석 데이터 (실제로는 API에서 받아옴)
    const agentAnalyses: AgentAnalysis[] = analysis ? [
        {
            agent: 'Sentinel',
            score: analysis.sentinelScore || 75,
            summary: '뉴스 분석 결과 긍정적 모멘텀 확인',
            signals: ['실적 서프라이즈', '신규 사업 진출', '애널리스트 목표가 상향'],
        },
        {
            agent: 'Axiom',
            score: analysis.axiomScore || 68,
            summary: 'PER/PBR 밸류에이션 적정 수준',
            signals: ['PER 12.5배 (업종 평균 15배)', 'ROE 15.2%', '부채비율 45%'],
        },
        {
            agent: 'Vector',
            score: analysis.vectorScore || 82,
            summary: 'NanoBanana 패턴 감지 - 이평선 수렴 완료',
            signals: ['5/20/60 이평선 수렴', '거래량 2.5배 급증', 'MACD 골든크로스'],
        },
        {
            agent: 'Resonance',
            score: analysis.resonanceScore || 71,
            summary: '시장 심리 긍정적, 공포탐욕지수 65',
            signals: ['개인 순매수 전환', 'SNS 언급량 120% 증가', '검색 트렌드 상승'],
        },
        {
            agent: 'Sonar',
            score: analysis.sonarScore || 78,
            summary: '외국인/기관 동시 순매수 확인',
            signals: ['외국인 3일 연속 순매수', '기관 대량 매집', '프로그램 순매수'],
        },
    ] : [];

    return (
        <div className="space-y-6">
            {/* 페이지 타이틀 */}
            <div>
                <h1 className="text-2xl font-bold">Deep Analysis</h1>
                <p className="text-muted-foreground">
                    5인 AI 에이전트의 상세 분석 결과를 확인합니다.
                </p>
            </div>

            {/* 검색 바 */}
            <div className="flex gap-3">
                <div className="relative flex-1 max-w-md">
                    <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
                    <input
                        type="text"
                        value={stockCode}
                        onChange={(e) => setStockCode(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="종목 코드 입력 (예: 005930)"
                        className="w-full rounded-lg border border-border bg-secondary/30 py-3 pl-10 pr-4 focus:border-primary focus:outline-none"
                    />
                </div>
                <button
                    onClick={handleSearch}
                    className="rounded-lg bg-primary px-6 py-3 font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
                >
                    분석하기
                </button>
            </div>

            {/* 로딩 상태 */}
            {isLoading && (
                <div className="flex h-64 items-center justify-center">
                    <div className="text-muted-foreground">분석 중...</div>
                </div>
            )}

            {/* 에러 상태 */}
            {error && (
                <div className="flex h-64 items-center justify-center rounded-xl border border-red-500/30 bg-red-500/10">
                    <div className="text-red-400">분석 결과를 가져올 수 없습니다.</div>
                </div>
            )}

            {/* 분석 결과 */}
            {analysis && !isLoading && (
                <div className="space-y-6">
                    {/* 종목 요약 */}
                    <div className="rounded-xl border border-border bg-card p-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <h2 className="text-xl font-bold">{analysis.stockName}</h2>
                                <p className="text-muted-foreground">{analysis.stockCode}</p>
                            </div>
                            <div className="text-right">
                                <div className="text-3xl font-bold font-mono">
                                    {analysis.totalScore}점
                                </div>
                                <div className={`text-sm ${analysis.recommendation === 'BUY' ? 'text-green-400' : analysis.recommendation === 'REJECT' ? 'text-red-400' : 'text-yellow-400'}`}>
                                    {analysis.recommendation ?? analysis.decision}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 에이전트별 분석 카드 */}
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                        {agentAnalyses.map((item) => (
                            <div
                                key={item.agent}
                                className={`rounded-xl border-2 p-5 ${getAgentColor(item.agent)}`}
                            >
                                {/* 헤더 */}
                                <div className="flex items-center justify-between mb-3">
                                    <span className="font-semibold">{item.agent}</span>
                                    <div className="flex items-center gap-2">
                                        {getScoreIcon(item.score)}
                                        <span className="font-mono font-bold">{item.score}</span>
                                    </div>
                                </div>

                                {/* 요약 */}
                                <p className="text-sm text-muted-foreground mb-3">
                                    {item.summary}
                                </p>

                                {/* 시그널 태그 */}
                                <div className="flex flex-wrap gap-1.5">
                                    {item.signals.map((signal, idx) => (
                                        <span
                                            key={idx}
                                            className="rounded-full bg-white/10 px-2 py-0.5 text-xs"
                                        >
                                            {signal}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* 차트 (캔들스틱) */}
                    <div className="rounded-xl border border-border bg-card p-6">
                        <h3 className="mb-4 text-lg font-semibold">기술적 분석 차트</h3>
                        <CandlestickChart
                            data={analysis.chartData || []}
                            height={350}
                        />
                    </div>

                    {/* Nexus 최종 의견 */}
                    <div className="rounded-xl border-2 border-primary bg-primary/10 p-6">
                        <div className="flex items-center gap-3 mb-3">
                            <span className="text-lg font-bold">🧠 Nexus 최종 판단</span>
                        </div>
                        <p className="text-muted-foreground">
                            {analysis.nexusComment ||
                                '5인 에이전트의 분석 결과를 종합한 결과, 현재 종목은 기술적/수급적 모멘텀이 강하며 단기 매수 관점에서 유효합니다. 단, 뉴스 리스크 모니터링을 권장합니다.'}
                        </p>
                    </div>
                </div>
            )}

            {/* 초기 상태 */}
            {!searchedCode && !isLoading && (
                <div className="flex h-64 items-center justify-center rounded-xl border border-dashed border-border">
                    <div className="text-center text-muted-foreground">
                        <Search className="mx-auto h-12 w-12 mb-3 opacity-50" />
                        <p>종목 코드를 입력하여 상세 분석을 시작하세요.</p>
                    </div>
                </div>
            )}
        </div>
    );
}
