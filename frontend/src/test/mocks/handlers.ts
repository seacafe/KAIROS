import { http, HttpResponse } from 'msw'

export const handlers = [
    // Target Stocks
    http.get('*/api/targets', () => {
        return HttpResponse.json({
            status: 'SUCCESS',
            data: [
                {
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
                    nexusReason: '강력한 펀더멘털과 수급 유입\n긍정적 뉴스 모멘텀'
                }
            ]
        })
    }),

    // Balance
    http.get('*/api/accounts/balance', () => {
        return HttpResponse.json({
            status: 'SUCCESS',
            data: {
                accountNo: '123-456-789',
                totalAsset: 10000000,
                deposit: 5000000,
                d2Deposit: 5000000,
                dailyProfitLoss: 150000,
                dailyReturnRate: 1.5
            }
        })
    }),

    // Holdings
    http.get('*/api/accounts/holdings', () => {
        return HttpResponse.json({
            status: 'SUCCESS',
            data: []
        })
    }),

    // Trade Logs
    http.get('*/api/trades', () => {
        return HttpResponse.json({
            status: 'SUCCESS',
            data: []
        })
    })
]
