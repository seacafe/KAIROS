import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { create } from 'zustand';

// 알림 메시지 타입
export interface AlertMessage {
    type: 'NEWS' | 'RISK' | 'ORDER' | 'SYSTEM' | 'KILL_SWITCH';
    title: string;
    message: string;
    level: 'INFO' | 'WARNING' | 'CRITICAL';
    timestamp: string;
    stockCode?: string;
}

// 거래 메시지 타입
export interface TradeMessage {
    stockCode: string;
    action: 'BUY' | 'SELL';
    status: 'SUBMITTED' | 'FILLED' | 'REJECTED';
    details: Record<string, unknown>;
    timestamp: string;
}

// WebSocket 스토어 상태
interface WebSocketState {
    // 연결 상태
    connected: boolean;
    connecting: boolean;
    error: string | null;

    // 메시지 저장
    alerts: AlertMessage[];
    trades: TradeMessage[];

    // 클라이언트 인스턴스
    client: Client | null;

    // 액션
    connect: () => void;
    disconnect: () => void;
    clearAlerts: () => void;
    addAlert: (alert: AlertMessage) => void;
    addTrade: (trade: TradeMessage) => void;
}

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

/**
 * WebSocket 연결 및 상태 관리 스토어.
 * STOMP over SockJS 프로토콜 사용.
 */
export const useWebSocketStore = create<WebSocketState>((set, get) => ({
    connected: false,
    connecting: false,
    error: null,
    alerts: [],
    trades: [],
    client: null,

    connect: () => {
        const { connected, connecting } = get();
        if (connected || connecting) return;

        set({ connecting: true, error: null });

        const stompClient = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => {
                console.log('[WebSocket] Connected');
                set({ connected: true, connecting: false, error: null });

                // 알림 구독
                stompClient.subscribe('/topic/alert', (message: IMessage) => {
                    const alert = JSON.parse(message.body) as AlertMessage;
                    get().addAlert(alert);
                });

                // 거래 업데이트 구독
                stompClient.subscribe('/topic/trade', (message: IMessage) => {
                    const trade = JSON.parse(message.body) as TradeMessage;
                    get().addTrade(trade);
                });
            },

            onDisconnect: () => {
                console.log('[WebSocket] Disconnected');
                set({ connected: false, connecting: false });
            },

            onStompError: (frame) => {
                console.error('[WebSocket] STOMP Error:', frame.headers['message']);
                set({ connected: false, connecting: false, error: frame.headers['message'] });
            },
        });

        stompClient.activate();
        set({ client: stompClient });
    },

    disconnect: () => {
        const { client } = get();
        if (client) {
            client.deactivate();
            set({ client: null, connected: false });
        }
    },

    clearAlerts: () => set({ alerts: [] }),

    addAlert: (alert) =>
        set((state) => ({
            alerts: [alert, ...state.alerts].slice(0, 50), // 최근 50개만 유지
        })),

    addTrade: (trade) =>
        set((state) => ({
            trades: [trade, ...state.trades].slice(0, 100), // 최근 100개만 유지
        })),
}));
