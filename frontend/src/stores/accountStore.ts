import { create } from 'zustand'

interface AccountState {
    totalAsset: number
    deposit: number
    dailyProfit: number
    setAccount: (data: Partial<Omit<AccountState, 'setAccount' | 'reset'>>) => void
    reset: () => void
}

const initialState = {
    totalAsset: 0,
    deposit: 0,
    dailyProfit: 0,
}

/**
 * 계좌 정보 전역 스토어.
 * frontendrule.md §2 - Zustand 전역 상태
 */
export const useAccountStore = create<AccountState>((set) => ({
    ...initialState,
    setAccount: (data) => set((state) => ({ ...state, ...data })),
    reset: () => set(initialState),
}))

