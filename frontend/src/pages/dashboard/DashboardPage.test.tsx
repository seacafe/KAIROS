import { renderWithProviders, screen, waitFor } from '@/shared/lib/test-utils'
import { DashboardPage } from './DashboardPage'
import { Suspense } from 'react'
import { describe, it, expect } from 'vitest'

describe('DashboardPage', () => {
    it('renders dashboard components and data correctly', async () => {
        renderWithProviders(
            <Suspense fallback={<div>Loading...</div>}>
                <DashboardPage />
            </Suspense>
        )

        // 1. Loading state verification
        // expect(screen.getByText('Loading...')).toBeInTheDocument()

        // 2. Data verification (Target Stock)
        await waitFor(() => {
            // 삼성전자가 타겟 종목 리스트에 있어야 함
            const elements = screen.getAllByText('삼성전자')
            expect(elements.length).toBeGreaterThan(0)
        })

        // 3. Score verification
        expect(screen.getByText(/85/)).toBeInTheDocument()

        // 4. Holdings verification (PortfolioHeatmap)
        // Note: Recharts renders SVG, so finding text might depend on implementation.
        // Assuming Heatmap renders the stock name.

        // 5. LogViewer verification
        expect(screen.getByText('실시간 로그')).toBeInTheDocument()
    })
})
