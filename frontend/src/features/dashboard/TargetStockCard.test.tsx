import { render, screen } from '@testing-library/react'
import { TargetStockCard } from './TargetStockCard'
import { TargetStock } from '@/shared/api/client'

describe('TargetStockCard', () => {
    const mockTarget: TargetStock = {
        id: 1,
        baseDate: '20240101',
        stockCode: '005930',
        stockName: '삼성전자',
        decision: 'BUY',
        riskLevel: 'LOW',
        nexusScore: 85,
        targetPrice: 75000,
        stopLoss: 69000,
        status: 'ACTIVE',
        agentScores: {
            Sentinel: 80,
            Axiom: 90,
            Vector: 85,
            Resonance: 70,
            Sonar: 88
        },
        nexusReason: '강력한 펀더멘털과 수급 유입'
    }

    it('renders stock information correctly', () => {
        render(<TargetStockCard target={mockTarget} />)

        expect(screen.getByText('삼성전자')).toBeInTheDocument()
        expect(screen.getByText('005930')).toBeInTheDocument()
        const scoreElements = screen.getAllByText('85')
        expect(scoreElements.length).toBeGreaterThan(0) // Score and Agent Score
        // Check for 'BUY' related visual or text? 
        // Logic uses decisionColor, doesn't print "BUY" explicitly other than maybe accessible text or implicit.
        // Actually, the card doesn't print "BUY" text, it uses color.
    })

    it('displays correct agent scores', () => {
        render(<TargetStockCard target={mockTarget} />)

        // AgentScoreBadge renders "Sentinel 80"
        expect(screen.getByText('Sentinel')).toBeInTheDocument()
        expect(screen.getByText('80')).toBeInTheDocument()
    })
})
