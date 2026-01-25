import { useEffect, useRef, useState, useCallback } from 'react';

interface LogEntry {
    id: string;
    timestamp: string;
    level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
    agent: string;
    message: string;
}

interface RealtimeLogViewerProps {
    wsUrl?: string;
    maxLogs?: number;
}

/**
 * 실시간 로그 뷰어 컴포넌트.
 * WebSocket으로 실시간 로그를 수신하여 표시.
 */
export function RealtimeLogViewer({
    wsUrl = 'ws://localhost:8080/ws/logs',
    maxLogs = 100
}: RealtimeLogViewerProps) {
    const [logs, setLogs] = useState<LogEntry[]>([]);
    const [isConnected, setIsConnected] = useState(false);
    const [autoScroll, setAutoScroll] = useState(true);
    const wsRef = useRef<WebSocket | null>(null);
    const logContainerRef = useRef<HTMLDivElement>(null);

    // WebSocket 연결
    const connect = useCallback(() => {
        if (wsRef.current?.readyState === WebSocket.OPEN) return;

        const ws = new WebSocket(wsUrl);
        wsRef.current = ws;

        ws.onopen = () => {
            setIsConnected(true);
            console.log('[LogViewer] WebSocket 연결됨');
        };

        ws.onclose = () => {
            setIsConnected(false);
            console.log('[LogViewer] WebSocket 연결 해제');
            // 5초 후 재연결
            setTimeout(connect, 5000);
        };

        ws.onerror = (error) => {
            console.error('[LogViewer] WebSocket 에러:', error);
        };

        ws.onmessage = (event) => {
            try {
                const log: LogEntry = JSON.parse(event.data);
                setLogs((prev) => {
                    const newLogs = [...prev, log];
                    // 최대 로그 수 제한
                    return newLogs.slice(-maxLogs);
                });
            } catch (e) {
                console.error('[LogViewer] 메시지 파싱 실패:', e);
            }
        };
    }, [wsUrl, maxLogs]);

    // 연결 관리
    useEffect(() => {
        connect();
        return () => {
            wsRef.current?.close();
        };
    }, [connect]);

    // 자동 스크롤
    useEffect(() => {
        if (autoScroll && logContainerRef.current) {
            logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
        }
    }, [logs, autoScroll]);

    // 로그 레벨별 색상
    const getLevelColor = (level: LogEntry['level']) => {
        switch (level) {
            case 'ERROR': return 'text-red-400';
            case 'WARN': return 'text-yellow-400';
            case 'INFO': return 'text-blue-400';
            case 'DEBUG': return 'text-gray-400';
            default: return 'text-gray-400';
        }
    };

    // 에이전트별 색상
    const getAgentColor = (agent: string) => {
        const colors: Record<string, string> = {
            Sentinel: 'text-purple-400',
            Axiom: 'text-cyan-400',
            Vector: 'text-green-400',
            Resonance: 'text-pink-400',
            Sonar: 'text-orange-400',
            Nexus: 'text-yellow-400',
            Aegis: 'text-red-400',
        };
        return colors[agent] || 'text-gray-400';
    };

    return (
        <div className="rounded-xl border border-border bg-card">
            {/* 헤더 */}
            <div className="flex items-center justify-between border-b border-border px-4 py-3">
                <div className="flex items-center gap-2">
                    <span className={`h-2 w-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`} />
                    <span className="text-sm font-medium">실시간 로그</span>
                    <span className="text-xs text-muted-foreground">({logs.length})</span>
                </div>
                <div className="flex items-center gap-3">
                    <label className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        <input
                            type="checkbox"
                            checked={autoScroll}
                            onChange={(e) => setAutoScroll(e.target.checked)}
                            className="h-3 w-3"
                        />
                        자동 스크롤
                    </label>
                    <button
                        onClick={() => setLogs([])}
                        className="text-xs text-muted-foreground hover:text-foreground"
                    >
                        지우기
                    </button>
                </div>
            </div>

            {/* 로그 목록 */}
            <div
                ref={logContainerRef}
                className="h-48 overflow-auto p-3 font-mono text-xs"
            >
                {logs.length === 0 ? (
                    <div className="flex h-full items-center justify-center text-muted-foreground">
                        {isConnected ? '로그 대기 중...' : '연결 중...'}
                    </div>
                ) : (
                    <div className="space-y-0.5">
                        {logs.map((log) => (
                            <div key={log.id} className="flex gap-2">
                                <span className="text-muted-foreground">{log.timestamp}</span>
                                <span className={`w-12 ${getLevelColor(log.level)}`}>[{log.level}]</span>
                                <span className={`w-20 ${getAgentColor(log.agent)}`}>[{log.agent}]</span>
                                <span className="text-foreground">{log.message}</span>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
