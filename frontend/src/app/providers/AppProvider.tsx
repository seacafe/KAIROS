import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactNode } from 'react'

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
 * 전역 프로바이더.
 * frontendrule.md §1.1 준수 - app/providers/ 분리
 */
export function AppProvider({ children }: AppProviderProps) {
    return (
        <QueryClientProvider client={queryClient}>
            {children}
        </QueryClientProvider>
    )
}
