import { http, HttpResponse } from 'msw'

export const handlers = [
    // Target Stocks
    http.get('*/api/v1/stocks/target', () => {
        return HttpResponse.json({
            status: 'SUCCESS',
            data: [
                {
                    code: '005930',
                    name: '삼성전자',
                    score: 85,
                    price: 72000,
                    changeRate: 1.5,
                    agents: {
                        Sentinel: 'BUY',
                        Axiom: 'BUY',
                        Vector: 'BUY',
                        Resonance: 'WATCH',
                        Sonar: 'BUY'
                    }
                },
                {
                    code: '000660',
                    name: 'SK하이닉스',
                    score: 92,
                    price: 135000,
                    changeRate: 2.3,
                    agents: {
                        Sentinel: 'BUY',
                        Axiom: 'BUY',
                        Vector: 'BUY',
                        Resonance: 'BUY',
                        Sonar: 'BUY'
                    }
                }
            ]
        })
    }),

    // Holdings
    http.get('*/api/v1/account/holdings', () => {
        return HttpResponse.json({
            status: 'SUCCESS',
            data: [
                {
                    stockCode: '005930',
                    stockName: '삼성전자',
                    quantity: 10,
                    averagePrice: 70000,
                    currentPrice: 72000,
                    profitRate: 2.85,
                    totalValue: 720000
                }
            ]
        })
    }),

    // Trade Logs
    http.get('*/api/v1/trade/logs', () => {
        return HttpResponse.json({
            status: 'SUCCESS',
            data: []
        })
    })
]
