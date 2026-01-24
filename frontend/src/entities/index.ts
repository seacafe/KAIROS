/**
 * Stock Entity 타입 정의.
 * frontendrule.md §1.1 - entities/ 폴더
 */
export interface Stock {
    code: string
    name: string
    currentPrice: number
    changeRate: number
    volume: number
}

/**
 * Account Entity 타입 정의.
 */
export interface Account {
    accountNo: string
    totalAsset: number
    deposit: number
    dailyProfitLoss: number
}

/**
 * User Entity 타입 정의.
 */
export interface User {
    id: string
    strategyMode: 'AGGRESSIVE' | 'NEUTRAL' | 'STABLE'
}
