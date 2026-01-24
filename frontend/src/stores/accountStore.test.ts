import { describe, it, expect, beforeEach } from 'vitest'
import { useAccountStore } from '@/stores/accountStore'

/**
 * accountStore 테스트.
 * frontendrule.md §5 준수 - Zustand Store 테스트
 */
describe('accountStore', () => {
    beforeEach(() => {
        // 각 테스트 전 스토어 초기화
        useAccountStore.getState().reset()
    })

    it('초기 상태가 올바르게 설정되어야 한다', () => {
        const state = useAccountStore.getState()

        expect(state.totalAsset).toBe(0)
        expect(state.deposit).toBe(0)
        expect(state.dailyProfit).toBe(0)
    })

    it('setAccount로 계좌 정보를 업데이트할 수 있다', () => {
        const { setAccount } = useAccountStore.getState()

        setAccount({
            totalAsset: 10000000,
            deposit: 5000000,
            dailyProfit: 150000,
        })

        const state = useAccountStore.getState()
        expect(state.totalAsset).toBe(10000000)
        expect(state.deposit).toBe(5000000)
        expect(state.dailyProfit).toBe(150000)
    })

    it('reset으로 초기 상태로 되돌릴 수 있다', () => {
        const { setAccount, reset } = useAccountStore.getState()

        setAccount({
            totalAsset: 10000000,
            deposit: 5000000,
            dailyProfit: 150000,
        })

        reset()

        const state = useAccountStore.getState()
        expect(state.totalAsset).toBe(0)
        expect(state.deposit).toBe(0)
    })

    it('selector를 사용하면 특정 값만 구독할 수 있다', () => {
        const { setAccount } = useAccountStore.getState()

        setAccount({
            totalAsset: 10000000,
            deposit: 5000000,
            dailyProfit: 150000,
        })

        // 개별 selector 테스트
        const totalAsset = useAccountStore.getState().totalAsset
        expect(totalAsset).toBe(10000000)
    })
})
