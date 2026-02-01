import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactNode } from 'react'
import { AlertToast, useWebSocketConnection } from '@/features/alerts/AlertToast'

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 1000 * 60, // 1분
            gcTime: 1000 * 60 * 5, // 5분
            retry: 1,
            refetchOnWindowFocus: false,
        },
    },
})

interface AppProviderProps {
    children: ReactNode
}

/**
 * WebSocket 연결 래퍼.
 */
function WebSocketProvider({ children }: { children: ReactNode }) {
    useWebSocketConnection();
    return <>{children}</>;
}

/**
 * 전역 프로바이더.
 * frontendrule.md §1.1 준수 - app/providers/ 분리
 */
export function AppProvider({ children }: AppProviderProps) {
    return (
        <QueryClientProvider client={queryClient}>
            <WebSocketProvider>
                {children}
                <AlertToast />
            </WebSocketProvider>
        </QueryClientProvider>
    )
}
