import { useEffect } from 'react';
import { AlertTriangle, Info, AlertCircle } from 'lucide-react';
import { useWebSocketStore, AlertMessage } from '@/stores/websocket';

/**
 * 실시간 알림 토스트 컴포넌트.
 * WebSocket으로 수신한 알림을 화면 우상단에 표시.
 */
export function AlertToast() {
    const { alerts, clearAlerts } = useWebSocketStore();

    // 최신 5개 알림만 표시
    const visibleAlerts = alerts.slice(0, 5);

    if (visibleAlerts.length === 0) return null;

    return (
        <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm">
            {visibleAlerts.map((alert, index) => (
                <AlertItem key={`${alert.timestamp}-${index}`} alert={alert} />
            ))}

            {alerts.length > 5 && (
                <button
                    onClick={clearAlerts}
                    className="text-xs text-muted-foreground hover:text-foreground text-center py-1"
                >
                    {alerts.length - 5}개 더 보기... (클릭하여 모두 닫기)
                </button>
            )}
        </div>
    );
}

function AlertItem({ alert }: { alert: AlertMessage }) {
    const getLevelStyles = () => {
        switch (alert.level) {
            case 'CRITICAL':
                return 'border-red-500/50 bg-red-500/10';
            case 'WARNING':
                return 'border-yellow-500/50 bg-yellow-500/10';
            default:
                return 'border-blue-500/50 bg-blue-500/10';
        }
    };

    const getIcon = () => {
        switch (alert.level) {
            case 'CRITICAL':
                return <AlertTriangle className="h-5 w-5 text-red-400" />;
            case 'WARNING':
                return <AlertCircle className="h-5 w-5 text-yellow-400" />;
            default:
                return <Info className="h-5 w-5 text-blue-400" />;
        }
    };

    return (
        <div
            className={`rounded-lg border p-4 shadow-lg backdrop-blur-sm animate-in slide-in-from-right ${getLevelStyles()}`}
        >
            <div className="flex items-start gap-3">
                {getIcon()}
                <div className="flex-1 min-w-0">
                    <p className="font-semibold text-sm truncate">{alert.title}</p>
                    <p className="text-xs text-muted-foreground mt-1 line-clamp-2">
                        {alert.message}
                    </p>
                    <p className="text-xs text-muted-foreground/60 mt-1">
                        {new Date(alert.timestamp).toLocaleTimeString()}
                    </p>
                </div>
            </div>
        </div>
    );
}

/**
 * WebSocket 자동 연결 훅.
 * App 레벨에서 한 번 호출하여 연결 유지.
 */
export function useWebSocketConnection() {
    const { connect, disconnect, connected } = useWebSocketStore();

    useEffect(() => {
        connect();
        return () => disconnect();
    }, [connect, disconnect]);

    return { connected };
}
