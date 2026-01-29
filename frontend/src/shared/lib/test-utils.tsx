import React, { ReactElement } from 'react'
import { render, RenderOptions } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Routes, Route } from 'react-router-dom'

// Create a new QueryClient for each test
const createTestQueryClient = () => new QueryClient({
    defaultOptions: {
        queries: {
            retry: false, // Turn off retries for testing
            gcTime: 0,
        },
    },
})

export function renderWithProviders(
    ui: ReactElement,
    {
        route = '/',
        ...renderOptions
    }: RenderOptions & { route?: string } = {}
) {
    const testQueryClient = createTestQueryClient()

    function Wrapper({ children }: { children: React.ReactNode }) {
        return (
            <QueryClientProvider client={testQueryClient}>
                <MemoryRouter initialEntries={[route]}>
                    <Routes>
                        <Route path="*" element={children} />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>
        )
    }

    return {
        ...render(ui, { wrapper: Wrapper, ...renderOptions }),
        queryClient: testQueryClient,
    }
}

export * from '@testing-library/react'
